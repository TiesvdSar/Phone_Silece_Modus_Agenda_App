package app.stillemodus

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import app.stillemodus.calendar.CalendarInfo
import app.stillemodus.calendar.CalendarRepository
import app.stillemodus.scheduling.CalendarSilenceScheduler
import app.stillemodus.settings.AutoSilencePreferences
import app.stillemodus.sound.SoundController
import app.stillemodus.ui.onboarding.OnboardingActivity
import app.stillemodus.ui.settings.CalendarSettingsActivity

class MainActivity : Activity() {

    private lateinit var soundController: SoundController
    private lateinit var statusText: TextView
    private lateinit var autoSilenceSwitch: Switch
    private lateinit var warningCard: View
    private lateinit var warningText: TextView
    private lateinit var calendarList: LinearLayout
    private lateinit var calendarEmptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        soundController = SoundController(this)
        statusText = findViewById(R.id.statusText)
        autoSilenceSwitch = findViewById(R.id.autoSilenceSwitch)
        warningCard = findViewById(R.id.warningCard)
        warningText = findViewById(R.id.warningText)
        calendarList = findViewById(R.id.calendarList)
        calendarEmptyText = findViewById(R.id.calendarEmptyText)

        findViewById<Button>(R.id.openOnboardingButton).setOnClickListener { openOnboarding() }

        if (!AutoSilencePreferences.isOnboardingDone(this)) openOnboarding()
    }

    override fun onResume() {
        super.onResume()

        autoSilenceSwitch.setOnCheckedChangeListener(null)
        autoSilenceSwitch.isChecked = AutoSilencePreferences.isEnabled(this)
        autoSilenceSwitch.setOnCheckedChangeListener { _, isChecked -> onSwitchChanged(isChecked) }

        CalendarSilenceScheduler.checkAndReschedule(this)

        updateStatus()
        updateWarning()
        updateCalendars()
    }

    private fun onSwitchChanged(isChecked: Boolean) {
        if (isChecked && !PermissionCheck.hasRequired(this)) {
            autoSilenceSwitch.isChecked = false
            openOnboarding()
            return
        }
        CalendarSilenceScheduler.setEnabled(this, isChecked)
        updateStatus()
    }

    private fun openOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
    }

    private fun updateStatus() {
        statusText.setText(
            when {
                soundController.isSilencedByApp() -> R.string.status_silenced_by_app
                soundController.isDndOn() -> R.string.status_dnd
                soundController.ringerMode() == AudioManager.RINGER_MODE_SILENT -> R.string.status_silent
                soundController.ringerMode() == AudioManager.RINGER_MODE_VIBRATE -> R.string.status_vibrate
                else -> R.string.status_normal
            }
        )
    }

    private fun updateWarning() {
        val message = when {
            !PermissionCheck.hasRequired(this) -> R.string.warning_required
            !PermissionCheck.hasAll(this) -> R.string.warning_optional
            else -> null
        }
        warningCard.visibility = if (message == null) View.GONE else View.VISIBLE
        message?.let { warningText.setText(it) }
    }

    private fun updateCalendars() {
        calendarList.removeAllViews()
        val calendars =
            if (PermissionCheck.hasCalendar(this)) CalendarRepository(this).calendars() else emptyList()
        calendarEmptyText.visibility = if (calendars.isEmpty()) View.VISIBLE else View.GONE
        calendars.forEach { calendarList.addView(calendarRow(it)) }
    }

    private fun calendarRow(calendar: CalendarInfo): View {
        val row = LayoutInflater.from(this).inflate(R.layout.item_calendar, calendarList, false)
        row.findViewById<View>(R.id.calendarColor).background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(calendar.color)
        }
        row.findViewById<TextView>(R.id.calendarName).text = calendar.displayName
        row.findViewById<TextView>(R.id.calendarAccount).text = calendar.accountName
        row.setOnClickListener {
            startActivity(CalendarSettingsActivity.intent(this, calendar.id))
        }
        return row
    }
}
