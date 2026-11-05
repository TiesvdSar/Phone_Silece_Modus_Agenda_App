package app.stillemodus.calendar

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int
)

data class CalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean = false,
    val color: Int = 0,
    val canceled: Boolean = false,
    val declined: Boolean = false,
    val free: Boolean = false,
    val invited: Boolean = false
)

class CalendarRepository(private val context: Context) {

    fun calendars(): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        val calendars = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                calendars.add(
                    CalendarInfo(
                        id = cursor.getLong(0),
                        displayName = cursor.getString(1) ?: "",
                        accountName = cursor.getString(2) ?: "",
                        color = cursor.getInt(3)
                    )
                )
            }
        }
        return calendars.sortedBy { it.displayName.lowercase() }
    }

    fun eventsBetween(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, startMillis)
        ContentUris.appendId(uriBuilder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.AVAILABILITY
        )

        val events = mutableListOf<CalendarEvent>()
        context.contentResolver.query(uriBuilder.build(), projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val allDay = cursor.getInt(5) != 0
                val begin = cursor.getLong(3)
                val end = cursor.getLong(4)
                events.add(
                    CalendarEvent(
                        eventId = cursor.getLong(0),
                        calendarId = cursor.getLong(1),
                        title = cursor.getString(2) ?: "",
                        startMillis = if (allDay) allDayToLocalMillis(begin) else begin,
                        endMillis = if (allDay) allDayToLocalMillis(end) else end,
                        allDay = allDay,
                        color = cursor.getInt(6),
                        canceled = cursor.getInt(7) == CalendarContract.Events.STATUS_CANCELED,
                        declined = cursor.getInt(8) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
                        free = cursor.getInt(9) == CalendarContract.Events.AVAILABILITY_FREE,
                        invited = cursor.getInt(8) == CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
                    )
                )
            }
        }
        return events
    }

    /** Colors to offer as toggles: the calendar's own color plus those used around now. */
    fun colorsFor(calendar: CalendarInfo, now: Long): List<Int> {
        val used = eventsBetween(now - COLOR_LOOKBACK_MILLIS, now + COLOR_LOOKAHEAD_MILLIS)
            .filter { it.calendarId == calendar.id }
            .map { it.color }
        return (listOf(calendar.color) + used).distinct()
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val COLOR_LOOKBACK_MILLIS = 30 * DAY_MILLIS
        private const val COLOR_LOOKAHEAD_MILLIS = 180 * DAY_MILLIS

        /** The calendar stores all-day events at UTC midnight; they should follow the local day. */
        fun allDayToLocalMillis(utcMidnightMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
            Instant.ofEpochMilli(utcMidnightMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
    }
}
