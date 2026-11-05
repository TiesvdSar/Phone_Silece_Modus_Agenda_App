package app.stillemodus.calendar

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class AllDayTimeTest {

    @Test
    fun `utc midnight becomes local midnight of the same date`() {
        val amsterdam = ZoneId.of("Europe/Amsterdam")
        val utcMidnight = LocalDateTime.of(2026, 7, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val expected = LocalDateTime.of(2026, 7, 1, 0, 0).atZone(amsterdam).toInstant().toEpochMilli()

        assertEquals(expected, CalendarRepository.allDayToLocalMillis(utcMidnight, amsterdam))
    }
}
