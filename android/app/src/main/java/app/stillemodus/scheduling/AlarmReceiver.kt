package app.stillemodus.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CalendarSilenceScheduler.checkAndReschedule(context)
    }
}
