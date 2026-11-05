package app.stillemodus.settings

/** Per-calendar settings. The defaults silence every timed event in the calendar. */
data class CalendarRule(
    val excludedColors: Set<Int> = emptySet(),
    val includeKeywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),
    val marginMinutes: Int = 0,
    val marginAfterMinutes: Int = 0,
    val includeAllDay: Boolean = false,
    val includeInvited: Boolean = false
) {
    companion object {
        const val MAX_MARGIN_MINUTES = 120

        fun parseKeywords(text: String): List<String> =
            text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
