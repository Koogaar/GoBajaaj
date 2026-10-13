package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.somalitraffic.data.repository.SomaliTrafficRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles ride actions straight from system notifications (works even when
 * the app process was killed):
 *   ACTION_ACCEPT  -> driver accepts the auto-assigned ride
 *   ACTION_DECLINE -> driver declines (releases it back to matching)
 */
class RideActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val rideId = intent.getStringExtra(EXTRA_RIDE_ID) ?: return
        val action = intent.getStringExtra(EXTRA_ACTION) ?: return
        val app = context.applicationContext
        val repo = (app as? GoBajaajApp)?.repository ?: SomaliTrafficRepository(app)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_ACCEPT -> repo.acceptRideByDriver(rideId)
                    ACTION_DECLINE -> repo.declineRide(rideId)
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "gb_ride_action"
        const val EXTRA_RIDE_ID = "gb_ride_id"
        const val ACTION_ACCEPT = "accept"
        const val ACTION_DECLINE = "decline"
    }
}
