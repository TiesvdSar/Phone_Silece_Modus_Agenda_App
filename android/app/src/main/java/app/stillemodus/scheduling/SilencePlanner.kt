package app.stillemodus.scheduling

import app.stillemodus.calendar.CalendarEvent
import app.stillemodus.settings.CalendarRule

data class SilenceWindow(val startMillis: Long, val endMillis: Long)

object SilencePlanner {

    private const val MINUTE_MILLIS = 60 * 1000L

    /** Keeps a very long event, for instance from a stranger's invitation, from muting calls for days. */
    private const val MAX_TIMED_EVENT_MILLIS = 24 * 60 * MINUTE_MILLIS

    fun shouldSilence(event: CalendarEvent, rule: CalendarRule): Boolean {
        if (event.canceled || event.declined) return false
        if (event.invited && !rule.includeInvited) return false
        if (event.allDay && !rule.includeAllDay) return false
        if (!event.allDay && event.endMillis - event.startMillis > MAX_TIMED_EVENT_MILLIS) return false
        if (event.color in rule.excludedColors) return false
        if (matchesAny(event.title, rule.excludeKeywords)) return false

        val matchesInclude = matchesAny(event.title, rule.includeKeywords)
        if (rule.includeKeywords.isNotEmpty() && !matchesInclude) return false

        // All-day events are "free" by default, so that must not undo the all-day option.
        // A keyword the user asked for explicitly also wins from "free".
        return !event.free || event.allDay || matchesInclude
    }

    fun windows(events: List<CalendarEvent>, ruleFor: (Long) -> CalendarRule): List<SilenceWindow> =
        events.mapNotNull { event ->
            val rule = ruleFor(event.calendarId)
            if (shouldSilence(event, rule)) {
                SilenceWindow(
                    event.startMillis - rule.marginMinutes * MINUTE_MILLIS,
                    event.endMillis + rule.marginAfterMinutes * MINUTE_MILLIS
                )
            } else {
                null
            }
        }

    fun isSilentAt(windows: List<SilenceWindow>, timeMillis: Long): Boolean =
        windows.any { timeMillis >= it.startMillis && timeMillis < it.endMillis }

    /** When the silence that is going on at [timeMillis] ends, following overlapping windows. */
    fun silentUntil(windows: List<SilenceWindow>, timeMillis: Long): Long {
        var until = timeMillis
        while (true) {
            until = windows
                .filter { it.startMillis <= until && it.endMillis > until }
                .maxOfOrNull { it.endMillis }
                ?: return until
        }
    }

    fun nextBoundaryAfter(windows: List<SilenceWindow>, timeMillis: Long): Long? =
        windows.flatMap { listOf(it.startMillis, it.endMillis) }
            .filter { it > timeMillis }
            .minOrNull()

    private fun matchesAny(title: String, keywords: List<String>): Boolean =
        keywords.any { title.contains(it, ignoreCase = true) }
}
