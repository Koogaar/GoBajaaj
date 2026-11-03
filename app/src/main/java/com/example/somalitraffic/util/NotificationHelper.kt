package com.example.somalitraffic.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

/**
 * NotificationHelper — shows system-level notifications for:
 *  1. New ride request (shown to driver)
 *  2. Driver arrived at pickup (shown to rider — even when app is in background)
 *  3. Ride accepted by driver (shown to rider)
 */
object NotificationHelper {

    private const val CHANNEL_RIDES = "gobajaaj_rides"
    private const val CHANNEL_ARRIVAL = "gobajaaj_arrival"

    private const val NOTIF_NEW_RIDE = 2001
    private const val NOTIF_ARRIVAL = 2002
    private const val NOTIF_ACCEPTED = 2003
    private const val NOTIF_ASSIGNED = 2004
    private const val NOTIF_STARTED = 2005
    private const val NOTIF_COMPLETED = 2006
    private const val NOTIF_CANCELLED = 2007
    private const val NOTIF_SUB_EXPIRED = 2008

    // ---------------------------------------------------------------
    // Channel creation (call once at app start / before any notification)
    // ---------------------------------------------------------------
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)

            // Ride requests channel
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RIDES,
                    "GoBajaaj – Dalabaadka",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Ogeysiinta dalabaadka cusub ee GoBajaaj"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                }
            )

            // Arrival channel (with ringtone)
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ARRIVAL,
                    "GoBajaaj – Imaanshaha Bajaajta",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Ogeysiinta marka bajaajlaha yimaado"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300, 200, 400)
                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
            )
        }
    }

    // ---------------------------------------------------------------
    // 1. New ride request → shown to DRIVER
    // ---------------------------------------------------------------
    fun showNewRideRequestNotification(
        context: Context,
        pickup: String,
        destination: String,
        offer: String
    ) {
        createChannels(context)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val pi = buildMainActivityIntent(context, requestCode = 10)
        val n = NotificationCompat.Builder(context, CHANNEL_RIDES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📯 Dalab Cusub Bajaaj!")
            .setContentText("$pickup ➔ $destination  •  $offer")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Rakaab cusub ayaa dalabanaya bajaaj!\n" +
                        "📍 Ka qaad: $pickup\n" +
                        "🏁 U socda: $destination\n" +
                        "💵 Qiimaha: $offer"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 400))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        safeNotify(context, NOTIF_NEW_RIDE, n)
    }

    // ---------------------------------------------------------------
    // 2. Driver arrived at pickup → shown to RIDER (even if backgrounded)
    // ---------------------------------------------------------------
    fun showDriverArrivedNotification(
        context: Context,
        driverName: String,
        pickup: String
    ) {
        createChannels(context)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val pi = buildMainActivityIntent(context, requestCode = 11)
        val n = NotificationCompat.Builder(context, CHANNEL_ARRIVAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📯 Bajaajlihii Wuu Yimid!")
            .setContentText("$driverName wuxuu sugayaa bannaanka: $pickup")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Bajaajlihii ($driverName) wuxuu soo gaaray!\n" +
                        "📍 Wuxuu sugayaa: $pickup\n" +
                        "➡️ Fadlan soo bax!"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300, 200, 400))
            .build()
        safeNotify(context, NOTIF_ARRIVAL, n)
    }

    // ---------------------------------------------------------------
    // 3. Ride accepted → shown to RIDER
    // ---------------------------------------------------------------
    fun showRideAcceptedNotification(
        context: Context,
        driverName: String,
        plate: String,
        etaMinutes: Int = 5
    ) {
        createChannels(context)
        val pi = buildMainActivityIntent(context, requestCode = 12)
        val n = NotificationCompat.Builder(context, CHANNEL_RIDES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✅ Darawal Ayaa Aqbalay Dalabkaaga!")
            .setContentText("$driverName ($plate) wuxuu u socdaa $etaMinutes daqiiqo")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        safeNotify(context, NOTIF_ACCEPTED, n)
    }

    // ---------------------------------------------------------------
    // 4. Auto-assigned ride -> DRIVER (actions: Aqbal / Iska dhaaf)
    // ---------------------------------------------------------------
    fun showAssignedRideNotification(
        context: Context,
        rideId: String,
        pickup: String,
        destination: String,
        offer: String
    ) {
        createChannels(context)
        val acceptIntent = Intent(context, com.example.RideActionReceiver::class.java).apply {
            action = com.example.RideActionReceiver.ACTION_ACCEPT
            putExtra(com.example.RideActionReceiver.EXTRA_RIDE_ID, rideId)
            putExtra(com.example.RideActionReceiver.EXTRA_ACTION, com.example.RideActionReceiver.ACTION_ACCEPT)
        }
        val declineIntent = Intent(context, com.example.RideActionReceiver::class.java).apply {
            action = com.example.RideActionReceiver.ACTION_DECLINE
            putExtra(com.example.RideActionReceiver.EXTRA_RIDE_ID, rideId)
            putExtra(com.example.RideActionReceiver.EXTRA_ACTION, com.example.RideActionReceiver.ACTION_DECLINE)
        }
        val acceptPi = PendingIntent.getBroadcast(
            context, rideId.hashCode() + 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePi = PendingIntent.getBroadcast(
            context, rideId.hashCode() + 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val n = NotificationCompat.Builder(context, CHANNEL_RIDES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📯 Dalab Cusub Bajaaj!")
            .setContentText("$pickup ➔ $destination  •  $offer")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Dalab cusub ayaa ku qoran!\n" +
                            "📍 Ka qaad: $pickup\n" +
                            "🏁 U socda: $destination\n" +
                            "💵 Qiimaha: $offer"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 400))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(buildMainActivityIntent(context, requestCode = 13))
            .addAction(0, "✅ Aqbal", acceptPi)
            .addAction(0, "❌ Iska dhaaf", declinePi)
            .build()
        safeNotify(context, NOTIF_ASSIGNED, n)
    }

    // ---------------------------------------------------------------
    // 5. Trip lifecycle -> RIDER
    // ---------------------------------------------------------------
    fun showTripStartedNotification(context: Context) {
        createChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_RIDES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🚗 Safarka wuu bilaabmay!")
            .setContentText("Bajaajlaha wuu kaa soo bilaabay safarka. Nasiino wanaagsan!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildMainActivityIntent(context, requestCode = 14))
            .build()
        safeNotify(context, NOTIF_STARTED, n)
    }

    fun showRideCompletedNotification(context: Context) {
        createChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_RIDES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🏁 Safarkii wuu dhammaaday!")
            .setContentText("Mahadsanid inaad GoBajaaj isticmaashay!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(buildMainActivityIntent(context, requestCode = 15))
            .build()
        safeNotify(context, NOTIF_COMPLETED, n)
    }

    fun showRideCancelledNotification(context: Context) {
        createChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_RIDES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("❌ Safarka waa la joojiyay")
            .setContentText("Darawalka wuu ka noqday dalabkaaga.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildMainActivityIntent(context, requestCode = 16))
            .build()
        safeNotify(context, NOTIF_CANCELLED, n)
    }

    // ---------------------------------------------------------------
    // 6. Driver Pass (24h) expired -> DRIVER
    // ---------------------------------------------------------------
    fun showSubscriptionExpiredNotification(context: Context) {
        createChannels(context)
        val n = NotificationCompat.Builder(context, CHANNEL_RIDES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ Muddada Driver Pass-ka waa dhammaatay")
            .setContentText("Sii wadista waddooyinka waxay u baahan tahay cusbooneysiin.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildMainActivityIntent(context, requestCode = 17))
            .build()
        safeNotify(context, NOTIF_SUB_EXPIRED, n)
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private fun buildMainActivityIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun safeNotify(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: Throwable) {
            // Silently skip any notification error / missing permission on any Android device
        }
    }
}
