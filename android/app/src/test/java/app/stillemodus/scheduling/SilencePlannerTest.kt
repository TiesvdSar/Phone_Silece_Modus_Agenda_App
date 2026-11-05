package app.stillemodus.scheduling

import app.stillemodus.calendar.CalendarEvent
import app.stillemodus.settings.CalendarRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SilencePlannerTest {

    private val minute = 60_000L
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    private fun event(
        title: String = "Vergadering",
        calendarId: Long = 1,
        start: Long = 100 * minute,
        end: Long = 160 * minute,
        allDay: Boolean = false,
        color: Int = red,
        canceled: Boolean = false,
        declined: Boolean = false,
        free: Boolean = false,
        invited: Boolean = false
    ) = CalendarEvent(1, calendarId, title, start, end, allDay, color, canceled, declined, free, invited)

    @Test
    fun `default rule silences a timed event`() {
        assertTrue(SilencePlanner.shouldSilence(event(), CalendarRule()))
    }

    @Test
    fun `canceled and declined events never silence`() {
        assertFalse(SilencePlanner.shouldSilence(event(canceled = true), CalendarRule()))
        assertFalse(SilencePlanner.shouldSilence(event(declined = true), CalendarRule()))
    }

    @Test
    fun `unanswered invitation only silences when the calendar allows it`() {
        assertFalse(SilencePlanner.shouldSilence(event(invited = true), CalendarRule()))
        assertTrue(SilencePlanner.shouldSilence(event(invited = true), CalendarRule(includeInvited = true)))
    }

    @Test
    fun `timed event longer than a day does not silence`() {
        val day = 24 * 60 * minute
        assertTrue(SilencePlanner.shouldSilence(event(start = 0, end = day), CalendarRule()))
        assertFalse(SilencePlanner.shouldSilence(event(start = 0, end = day + minute), CalendarRule()))
    }

    @Test
    fun `multi-day all-day event still silences when included`() {
        val allDay = event(start = 0, end = 3 * 24 * 60 * minute, allDay = true)
        assertTrue(SilencePlanner.shouldSilence(allDay, CalendarRule(includeAllDay = true)))
    }

    @Test
    fun `silent until follows overlapping windows`() {
        val windows = listOf(SilenceWindow(10, 20), SilenceWindow(15, 30), SilenceWindow(30, 35), SilenceWindow(50, 60))
        assertEquals(35L, SilencePlanner.silentUntil(windows, 12))
        assertEquals(40L, SilencePlanner.silentUntil(windows, 40))
    }

    @Test
    fun `free timed event does not silence`() {
        assertFalse(SilencePlanner.shouldSilence(event(free = true), CalendarRule()))
    }

    @Test
    fun `free timed event silences when an include keyword matches`() {
        val rule = CalendarRule(includeKeywords = listOf("vergadering"))
        assertTrue(SilencePlanner.shouldSilence(event(free = true), rule))
    }

    @Test
    fun `all-day events follow the calendar option, also when marked free`() {
        val allDay = event(allDay = true, free = true)
        assertFalse(SilencePlanner.shouldSilence(allDay, CalendarRule()))
        assertTrue(SilencePlanner.shouldSilence(allDay, CalendarRule(includeAllDay = true)))
    }

    @Test
    fun `excluded color does not silence, other colors do`() {
        val rule = CalendarRule(excludedColors = setOf(red))
        assertFalse(SilencePlanner.shouldSilence(event(color = red), rule))
        assertTrue(SilencePlanner.shouldSilence(event(color = blue), rule))
    }

    @Test
    fun `include keywords limit events and ignore case`() {
        val rule = CalendarRule(includeKeywords = listOf("LES"))
        assertTrue(SilencePlanner.shouldSilence(event(title = "Wiskundeles"), rule))
        assertFalse(SilencePlanner.shouldSilence(event(title = "Tandarts"), rule))
    }

    @Test
    fun `exclude keyword wins from include keyword`() {
        val rule = CalendarRule(includeKeywords = listOf("les"), excludeKeywords = listOf("lunch"))
        assertFalse(SilencePlanner.shouldSilence(event(title = "Les met lunch"), rule))
    }

    @Test
    fun `margin starts the window early but keeps the end`() {
        val windows = SilencePlanner.windows(listOf(event())) { CalendarRule(marginMinutes = 5) }
        assertEquals(listOf(SilenceWindow(95 * minute, 160 * minute)), windows)
    }

    @Test
    fun `margin after keeps the window open past the end`() {
        val windows = SilencePlanner.windows(listOf(event())) {
            CalendarRule(marginMinutes = 5, marginAfterMinutes = 15)
        }
        assertEquals(listOf(SilenceWindow(95 * minute, 175 * minute)), windows)
    }

    @Test
    fun `rules apply per calendar`() {
        val events = listOf(
            event(calendarId = 1),
            event(calendarId = 2, start = 200 * minute, end = 210 * minute)
        )
        val windows = SilencePlanner.windows(events) { calendarId ->
            if (calendarId == 1L) {
                CalendarRule(excludeKeywords = listOf("vergadering"))
            } else {
                CalendarRule(marginMinutes = 10)
            }
        }
        assertEquals(listOf(SilenceWindow(190 * minute, 210 * minute)), windows)
    }

    @Test
    fun `window includes its start and excludes its end`() {
        val windows = listOf(SilenceWindow(10, 20))
        assertFalse(SilencePlanner.isSilentAt(windows, 9))
        assertTrue(SilencePlanner.isSilentAt(windows, 10))
        assertTrue(SilencePlanner.isSilentAt(windows, 19))
        assertFalse(SilencePlanner.isSilentAt(windows, 20))
    }

    @Test
    fun `overlapping windows stay silent until the last one ends`() {
        val windows = listOf(SilenceWindow(10, 20), SilenceWindow(15, 30))
        assertTrue(SilencePlanner.isSilentAt(windows, 25))
        assertEquals(30L, SilencePlanner.nextBoundaryAfter(windows, 20))
    }

    @Test
    fun `next boundary is the nearest future start or end`() {
        val windows = listOf(SilenceWindow(10, 20), SilenceWindow(40, 50))
        assertEquals(10L, SilencePlanner.nextBoundaryAfter(windows, 5))
        assertEquals(20L, SilencePlanner.nextBoundaryAfter(windows, 10))
        assertEquals(40L, SilencePlanner.nextBoundaryAfter(windows, 20))
        assertNull(SilencePlanner.nextBoundaryAfter(windows, 50))
    }
}
