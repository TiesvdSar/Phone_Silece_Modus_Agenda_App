package app.stillemodus.sound

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import app.stillemodus.PermissionCheck
import app.stillemodus.settings.AutoSilencePreferences

class SoundController(private val context: Context) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun ringerMode(): Int = audioManager.ringerMode

    fun isDndOn(): Boolean =
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    fun isSilencedByApp(): Boolean = AutoSilencePreferences.isSilenceActive(context)

    /**
     * Uses the interruption filter rather than AudioManager.setRingerMode: recent Android versions
     * silently ignore ringer changes from background apps, and this always runs in the background.
     */
    fun applySilence(shouldSilence: Boolean) {
        if (!PermissionCheck.hasDndAccess(context)) return

        val decision = SilenceRules.decide(
            shouldSilence = shouldSilence,
            active = isSilencedByApp(),
            currentFilter = notificationManager.currentInterruptionFilter
        )
        decision.filterToSet?.let { notificationManager.setInterruptionFilter(it) }
        AutoSilencePreferences.setSilenceActive(context, decision.active)
    }
}
