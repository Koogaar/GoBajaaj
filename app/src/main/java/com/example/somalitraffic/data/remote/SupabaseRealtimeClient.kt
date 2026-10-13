package com.example.somalitraffic.data.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal Supabase Realtime (Phoenix protocol, vsn=1.0.0) client used for
 * LIVE DRIVER LOCATION streaming:
 *
 *   Driver Phone -> GPS -> Supabase (bajaj_rides) -> Realtime -> Rider Phone
 *
 * Uses postgres_changes on the existing bajaj_rides table with a row filter,
 * so the rider receives driver location updates without refreshing. Works
 * with the existing Supabase project (table must be in the
 * `supabase_realtime` publication - see MIGRATION_GOBAAJAAJ_LIVE_RIDING.sql).
 *
 * Designed for Somalia's unstable networks: auto-reconnect with exponential
 * backoff, heartbeat every 20s, and callers always keep a polling fallback
 * plus a "last updated N seconds ago" staleness indicator.
 */
class SupabaseRealtimeClient(
    private val supabaseUrl: String,
    private val anonKey: String
) {
    interface RideListener {
        fun onRideUpdate(record: Map<String, Any?>)
        fun onConnectionChanged(connected: Boolean)
    }

    inner class Subscription internal constructor(
        private val rideId: String,
        private val listener: RideListener
    ) {
        @Volatile private var webSocket: WebSocket? = null
        private val closed = AtomicBoolean(false)
        private val ref = AtomicInteger(1)
        private var attempt = 0
        private var heartbeat: java.util.Timer? = null
        private var reconnectRunnable: Runnable? = null
        private val topic = "realtime:${UUID.randomUUID()}"
        @Volatile private var token: String? = null
        @Volatile private var joined = false

        internal fun connect(token: String?) {
            this.token = token
            openSocket()
        }

        private fun wsUrl(): String {
            val base = supabaseUrl.removePrefix("https://").removePrefix("http://")
            return "wss://$base/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"
        }

        private fun openSocket() {
            if (closed.get()) return
            webSocket?.cancel()
            val req = Request.Builder().url(wsUrl()).build()
            webSocket = client.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    join()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerMessage(text)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onConnectionChanged(false)
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Realtime failure: ${t.message}")
                    listener.onConnectionChanged(false)
                    scheduleReconnect()
                }
            })
        }

        private fun scheduleReconnect() {
            if (closed.get()) return
            attempt = (attempt + 1).coerceAtMost(6)
            val delayMs = (1000L shl (attempt - 1)).coerceAtMost(15_000L)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val r = Runnable { if (!closed.get()) openSocket() }
            reconnectRunnable = r
            handler.postDelayed(r, delayMs)
        }

        private fun send(event: String, payload: JSONObject, joinRef: Int? = null) {
            val message = JSONObject().apply {
                put("topic", topic)
                put("event", event)
                val r = ref.getAndIncrement()
                put("ref", r.toString())
                if (joinRef != null) put("join_ref", joinRef.toString())
                put("payload", payload)
            }
            webSocket?.send(message.toString())
        }

        private fun handleServerMessage(text: String) {
            try {
                val msg = JSONObject(text)
                when (msg.optString("event")) {
                    "phx_reply" -> {
                        val payload = msg.optJSONObject("payload") ?: return
                        val status = payload.optString("status")
                        if (status == "ok") {
                            if (!joined) {
                                joined = true
                                attempt = 0
                                listener.onConnectionChanged(true)
                                startHeartbeat()
                            }
                        } else {
                            Log.w(TAG, "Realtime join error: ${payload.optString("response")}")
                        }
                    }
                    "postgres_changes" -> {
                        val payload = msg.optJSONObject("payload") ?: return
                        val data = payload.optJSONObject("data") ?: return
                        if (data.optString("table") != "bajaj_rides") return
                        val record = data.optJSONObject("record") ?: return
                        val map = mutableMapOf<String, Any?>()
                        record.keys().forEach { k -> map[k] = record.opt(k) }
                        listener.onRideUpdate(map)
                    }
                    "phx_close", "phx_error" -> {
                        joined = false
                        listener.onConnectionChanged(false)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Realtime parse error: ${e.message}")
            }
        }

        private fun startHeartbeat() {
            stopHeartbeat()
            val timer = java.util.Timer("realtime-hb", true)
            timer.scheduleAtFixedRate(object : java.util.TimerTask() {
                override fun run() {
                    if (closed.get()) return
                    try {
                        val payload = JSONObject()
                        val hb = JSONObject().apply {
                            put("topic", "phoenix")
                            put("event", "heartbeat")
                            put("ref", ref.getAndIncrement().toString())
                            put("payload", payload)
                        }
                        webSocket?.send(hb.toString())
                    } catch (_: Exception) {
                    }
                }
            }, 20_000, 20_000)
            heartbeat = timer
        }

        private fun stopHeartbeat() {
            heartbeat?.cancel()
            heartbeat = null
        }

        /** Stops tracking completely (called when the trip ends - no orphan subscriptions). */
        fun close() {
            closed.set(true)
            stopHeartbeat()
            reconnectRunnable?.let {
                android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(it)
            }
            webSocket?.close(1000, "ride_finished")
            webSocket = null
            joined = false
        }

        internal fun join() {
            val payload = JSONObject().apply {
                token?.let { put("access_token", it) }
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().put(JSONObject().apply {
                        put("event", "*")
                        put("schema", "public")
                        put("table", "bajaj_rides")
                        put("filter", "id=eq.$rideId")
                    }))
                    put("broadcast", JSONObject().put("self", false).put("ack", false))
                    put("presence", JSONObject().put("key", ""))
                })
                put("private", true)
            }
            val joinRef = ref.get()
            val message = JSONObject().apply {
                put("topic", topic)
                put("event", "phx_join")
                put("ref", joinRef.toString())
                put("join_ref", joinRef.toString())
                put("payload", payload)
            }
            webSocket?.send(message.toString())
        }
    }

    fun subscribeToRide(rideId: String, token: String?, listener: RideListener): Subscription {
        val sub = Subscription(rideId, listener)
        sub.connect(token)
        return sub
    }

    companion object {
        private const val TAG = "GBRealtime"

        private val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
