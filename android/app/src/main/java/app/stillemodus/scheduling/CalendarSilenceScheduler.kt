package app.stillemodus.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.stillemodus.PermissionCheck
import app.stillemodus.calendar.CalendarRepository
import app.stillemodus.settings.AutoSilencePreferences
import app.stillemodus.settings.CalendarRule
import app.stillemodus.sound.SoundController

object CalendarSilenceScheduler {

    private const val MINUTE_MILLIS = 60 * 1000L
    private const val DAY_MILLIS = 24 * 60 * MINUTE_MILLIS
    private const val FALLBACK_INTERVAL_MILLIS = 15 * MINUTE_MILLIS
    private const val WINDOW_MILLIS = DAY_MILLIS

    fun setEnabled(context: Context, enabled: Boolean) {
        AutoSilencePreferences.setEnabled(context, enabled)
        if (enabled) {
            checkAndReschedule(context)
        } else {
            stop(context)
        }
    }

    fun checkAndReschedule(context: Context) {
        if (!AutoSilencePreferences.isEnabled(context)) return
        if (!PermissionCheck.hasCalendar(context)) {
            // Nothing can be planned anymore, so don't leave the phone silent indefinitely.
            stop(context)
            return
        }

        val now = System.currentTimeMillis()
        // Queried wider than the window: all-day events are stored in UTC, and margins can pull
        // an event that lies outside the window (ended or not yet started) back into it.
        val events = CalendarRepository(context).eventsBetween(
            now - DAY_MILLIS,
            now + WINDOW_MILLIS + CalendarRule.MAX_MARGIN_MINUTES * MINUTE_MILLIS + DAY_MILLIS
        )
        val rules = mutableMapOf<Long, CalendarRule>()
        val windows = SilencePlanner.windows(events) { calendarId ->
            rules.getOrPut(calendarId) { AutoSilencePreferences.rule(context, calendarId) }
        }

        SoundController(context).applySilence(SilencePlanner.isSilentAt(windows, now))

        val nextBoundary = SilencePlanner.nextBoundaryAfter(windows, now)
        scheduleAt(context, nextBoundary ?: (now + FALLBACK_INTERVAL_MILLIS))
        CalendarChangeJobService.ensureScheduled(context)
    }

    private fun stop(context: Context) {
        cancelAlarm(context)
        CalendarChangeJobService.cancel(context)
        SoundController(context).applySilence(false)
    }

    private fun scheduleAt(context: Context, timeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent(context)

        if (PermissionCheck.canScheduleExact(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context))
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
