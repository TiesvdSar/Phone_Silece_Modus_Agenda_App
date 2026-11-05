package app.stillemodus.settings

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object AutoSilencePreferences {
    private const val PREFS_NAME = "stille_modus_prefs"
    private const val KEY_ENABLED = "auto_silence_enabled"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_SILENCE_ACTIVE = "silence_active"
    private const val KEY_RULE_PREFIX = "calendar_rule_"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isOnboardingDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun isSilenceActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SILENCE_ACTIVE, false)

    fun setSilenceActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_SILENCE_ACTIVE, active).apply()
    }

    fun rule(context: Context, calendarId: Long): CalendarRule {
        val json = prefs(context).getString(KEY_RULE_PREFIX + calendarId, null)
            ?: return CalendarRule()
        val rule = JSONObject(json)
        return CalendarRule(
            excludedColors = rule.getJSONArray("excludedColors").toList { getInt(it) }.toSet(),
            includeKeywords = rule.getJSONArray("includeKeywords").toList { getString(it) },
            excludeKeywords = rule.getJSONArray("excludeKeywords").toList { getString(it) },
            marginMinutes = rule.getInt("marginMinutes"),
            marginAfterMinutes = rule.optInt("marginAfterMinutes", 0),
            includeAllDay = rule.getBoolean("includeAllDay")
        )
    }

    fun setRule(context: Context, calendarId: Long, rule: CalendarRule) {
        val json = JSONObject()
            .put("excludedColors", JSONArray(rule.excludedColors.toList()))
            .put("includeKeywords", JSONArray(rule.includeKeywords))
            .put("excludeKeywords", JSONArray(rule.excludeKeywords))
            .put("marginMinutes", rule.marginMinutes)
            .put("marginAfterMinutes", rule.marginAfterMinutes)
            .put("includeAllDay", rule.includeAllDay)
        prefs(context).edit().putString(KEY_RULE_PREFIX + calendarId, json.toString()).apply()
    }

    private fun <T> JSONArray.toList(read: JSONArray.(Int) -> T): List<T> =
        (0 until length()).map { read(it) }
}
