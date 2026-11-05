package app.stillemodus.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import app.stillemodus.PermissionCheck
import app.stillemodus.R
import app.stillemodus.calendar.CalendarInfo
import app.stillemodus.calendar.CalendarRepository
import app.stillemodus.scheduling.CalendarSilenceScheduler
import app.stillemodus.settings.AutoSilencePreferences
import app.stillemodus.settings.CalendarRule

class CalendarSettingsActivity : Activity() {

    private var calendarId = NO_CALENDAR
    private val excludedColors = mutableSetOf<Int>()

    private lateinit var includeKeywords: EditText
    private lateinit var excludeKeywords: EditText
    private lateinit var margin: EditText
    private lateinit var marginAfter: EditText
    private lateinit var allDaySwitch: Switch
    private lateinit var invitedSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestedId = intent.getLongExtra(EXTRA_CALENDAR_ID, NO_CALENDAR)
        val repository = CalendarRepository(this)
        val calendar = if (PermissionCheck.hasCalendar(this)) {
            repository.calendars().firstOrNull { it.id == requestedId }
        } else {
            null
        }
        if (calendar == null) {
            finish()
            return
        }
        calendarId = calendar.id

        setContentView(R.layout.activity_calendar_settings)
        includeKeywords = findViewById(R.id.includeKeywords)
        excludeKeywords = findViewById(R.id.excludeKeywords)
        margin = findViewById(R.id.marginMinutes)
        marginAfter = findViewById(R.id.marginAfterMinutes)
        allDaySwitch = findViewById(R.id.allDaySwitch)
        invitedSwitch = findViewById(R.id.invitedSwitch)

        val rule = AutoSilencePreferences.rule(this, calendar.id)
        excludedColors.addAll(rule.excludedColors)

        findViewById<TextView>(R.id.calendarTitle).text = calendar.displayName
        findViewById<TextView>(R.id.calendarSubtitle).text = calendar.accountName
        includeKeywords.setText(rule.includeKeywords.joinToString(", "))
        excludeKeywords.setText(rule.excludeKeywords.joinToString(", "))
        margin.setText(rule.marginMinutes.toString())
        marginAfter.setText(rule.marginAfterMinutes.toString())
        allDaySwitch.isChecked = rule.includeAllDay
        invitedSwitch.isChecked = rule.includeInvited

        showColors(calendar, repository.colorsFor(calendar, System.currentTimeMillis()))
    }

    private fun showColors(calendar: CalendarInfo, colors: List<Int>) {
        val colorList = findViewById<LinearLayout>(R.id.colorList)
        // A color excluded earlier may no longer be in use, but must stay switchable.
        (colors + excludedColors).distinct().forEachIndexed { index, color ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_color_toggle, colorList, false)
            row.findViewById<View>(R.id.colorSwatch).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            row.findViewById<Switch>(R.id.colorSwitch).apply {
                text = if (color == calendar.color) {
                    getString(R.string.color_calendar)
                } else {
                    getString(R.string.color_numbered, index + 1)
                }
                isChecked = color !in excludedColors
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) excludedColors.remove(color) else excludedColors.add(color)
                }
            }
            colorList.addView(row)
        }
    }

    override fun onPause() {
        super.onPause()
        if (calendarId == NO_CALENDAR) return

        val rule = CalendarRule(
            excludedColors = excludedColors.toSet(),
            includeKeywords = CalendarRule.parseKeywords(includeKeywords.text.toString()),
            excludeKeywords = CalendarRule.parseKeywords(excludeKeywords.text.toString()),
            marginMinutes = minutesIn(margin),
            marginAfterMinutes = minutesIn(marginAfter),
            includeAllDay = allDaySwitch.isChecked,
            includeInvited = invitedSwitch.isChecked
        )
        AutoSilencePreferences.setRule(this, calendarId, rule)
        CalendarSilenceScheduler.checkAndReschedule(this)
    }

    private fun minutesIn(field: EditText): Int =
        (field.text.toString().toIntOrNull() ?: 0).coerceIn(0, CalendarRule.MAX_MARGIN_MINUTES)

    companion object {
        private const val EXTRA_CALENDAR_ID = "calendar_id"
        private const val NO_CALENDAR = -1L

        fun intent(context: Context, calendarId: Long): Intent =
            Intent(context, CalendarSettingsActivity::class.java)
                .putExtra(EXTRA_CALENDAR_ID, calendarId)
    }
}
