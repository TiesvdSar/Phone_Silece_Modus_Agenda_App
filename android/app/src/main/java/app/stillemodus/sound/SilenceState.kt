package app.stillemodus.sound

import android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL

/**
 * @param active the app itself silenced the phone and owes it a restore
 * @param filterToSet the interruption filter to apply, or null to leave the phone as it is
 */
data class SilenceDecision(val active: Boolean, val filterToSet: Int? = null)

object SilenceRules {

    /** Calls and notifications are fully silent, but the user's alarm clock still goes off. */
    const val SILENT_FILTER = INTERRUPTION_FILTER_ALARMS

    fun decide(shouldSilence: Boolean, active: Boolean, currentFilter: Int): SilenceDecision =
        when {
            // Otherwise the user has their own Do Not Disturb on: leave it, also afterwards.
            shouldSilence && !active ->
                if (currentFilter == INTERRUPTION_FILTER_ALL) {
                    SilenceDecision(active = true, filterToSet = SILENT_FILTER)
                } else {
                    SilenceDecision(active = false)
                }

            // Only restore if still silent; otherwise the user changed it during the event.
            !shouldSilence && active -> SilenceDecision(
                active = false,
                filterToSet = INTERRUPTION_FILTER_ALL.takeIf { currentFilter == SILENT_FILTER }
            )

            else -> SilenceDecision(active)
        }
}
