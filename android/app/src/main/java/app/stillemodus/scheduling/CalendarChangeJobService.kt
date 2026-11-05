package app.stillemodus.scheduling

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.CalendarContract

/**
 * Re-plans when the calendar changes. A ContentObserver would die with the app process; a job
 * with a content trigger also fires while the app isn't running. Such a job fires only once and
 * can't be persisted, so it is scheduled again after every run and after a reboot.
 */
class CalendarChangeJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        CalendarSilenceScheduler.checkAndReschedule(this)
        schedule(this)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    companion object {
        private const val JOB_ID = 1
        private const val UPDATE_DELAY_MILLIS = 2_000L
        private const val MAX_DELAY_MILLIS = 30_000L

        fun ensureScheduled(context: Context) {
            if (jobScheduler(context).getPendingJob(JOB_ID) == null) schedule(context)
        }

        fun cancel(context: Context) {
            jobScheduler(context).cancel(JOB_ID)
        }

        private fun schedule(context: Context) {
            val job = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, CalendarChangeJobService::class.java)
            )
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        CalendarContract.CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                    )
                )
                .setTriggerContentUpdateDelay(UPDATE_DELAY_MILLIS)
                .setTriggerContentMaxDelay(MAX_DELAY_MILLIS)
                .build()
            jobScheduler(context).schedule(job)
        }

        private fun jobScheduler(context: Context): JobScheduler =
            context.getSystemService(JobScheduler::class.java)
    }
}
