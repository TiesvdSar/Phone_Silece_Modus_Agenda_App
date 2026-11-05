package app.stillemodus.sound

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.text.format.DateFormat
import app.stillemodus.MainActivity
import app.stillemodus.R
import java.util.Date

/** Shows that the app silenced the phone and offers a way out if the planned end never comes. */
object SilenceNotification {

    private const val CHANNEL_ID = "silence"
    private const val NOTIFICATION_ID = 1

    fun update(context: Context, silentUntilMillis: Long?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (silentUntilMillis == null) {
            manager.cancel(NOTIFICATION_ID)
            return
        }

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val time = DateFormat.getTimeFormat(context).format(Date(silentUntilMillis))
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text, time))
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notification),
                    context.getString(R.string.notification_sound_on),
                    soundOnIntent(context)
                ).build()
            )
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

    private fun soundOnIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SoundOnReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
}

class SoundOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SoundController(context).turnSoundOn()
        SilenceNotification.update(context, null)
    }
}
