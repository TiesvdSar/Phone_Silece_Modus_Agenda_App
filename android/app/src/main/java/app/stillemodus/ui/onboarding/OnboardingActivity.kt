package app.stillemodus.ui.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import app.stillemodus.PermissionCheck
import app.stillemodus.R
import app.stillemodus.settings.AutoSilencePreferences

class OnboardingActivity : Activity() {

    private class Step(
        val title: Int,
        val description: Int,
        val isDone: () -> Boolean,
        val open: () -> Unit
    )

    private lateinit var stepList: LinearLayout

    private val steps: List<Step> by lazy {
        buildList {
            add(
                Step(
                    R.string.step_calendar_title,
                    R.string.step_calendar_description,
                    { PermissionCheck.hasCalendar(this@OnboardingActivity) },
                    { requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), REQUEST_READ_CALENDAR) }
                )
            )
            add(
                Step(
                    R.string.step_dnd_title,
                    R.string.step_dnd_description,
                    { PermissionCheck.hasDndAccess(this@OnboardingActivity) },
                    { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    Step(
                        R.string.step_exact_title,
                        R.string.step_exact_description,
                        { PermissionCheck.canScheduleExact(this@OnboardingActivity) },
                        {
                            startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri())
                            )
                        }
                    )
                )
            }
            add(
                Step(
                    R.string.step_battery_title,
                    R.string.step_battery_description,
                    { PermissionCheck.ignoresBatteryOptimizations(this@OnboardingActivity) },
                    { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        stepList = findViewById(R.id.stepList)

        findViewById<Button>(R.id.doneButton).setOnClickListener {
            AutoSilencePreferences.setOnboardingDone(this)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        showSteps()
    }

    private fun showSteps() {
        stepList.removeAllViews()
        steps.forEach { step ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_onboarding_step, stepList, false)
            val done = step.isDone()
            row.findViewById<TextView>(R.id.stepTitle).setText(step.title)
            row.findViewById<TextView>(R.id.stepDescription).setText(step.description)
            row.findViewById<TextView>(R.id.stepDone).visibility = if (done) View.VISIBLE else View.GONE
            row.findViewById<Button>(R.id.stepButton).apply {
                visibility = if (done) View.GONE else View.VISIBLE
                setOnClickListener { step.open() }
            }
            stepList.addView(row)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_READ_CALENDAR) return

        val denied = grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED
        if (denied && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            // Android no longer shows the dialog after repeated denials.
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
        }
        showSteps()
    }

    private fun packageUri(): Uri = Uri.parse("package:$packageName")

    private companion object {
        const val REQUEST_READ_CALENDAR = 1
    }
}
