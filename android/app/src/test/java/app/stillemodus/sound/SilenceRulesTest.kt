package app.stillemodus.sound

import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
import app.stillemodus.sound.SilenceRules.SILENT_FILTER
import org.junit.Assert.assertEquals
import org.junit.Test

class SilenceRulesTest {

    @Test
    fun `event start silences the phone`() {
        assertEquals(
            SilenceDecision(active = true, filterToSet = SILENT_FILTER),
            SilenceRules.decide(true, active = false, currentFilter = INTERRUPTION_FILTER_ALL)
        )
    }

    @Test
    fun `event end restores sound`() {
        assertEquals(
            SilenceDecision(active = false, filterToSet = INTERRUPTION_FILTER_ALL),
            SilenceRules.decide(false, active = true, currentFilter = SILENT_FILTER)
        )
    }

    @Test
    fun `no event leaves the users own do not disturb alone`() {
        assertEquals(
            SilenceDecision(active = false),
            SilenceRules.decide(false, active = false, currentFilter = INTERRUPTION_FILTER_PRIORITY)
        )
    }

    @Test
    fun `users own do not disturb is not claimed by the app`() {
        assertEquals(
            SilenceDecision(active = false),
            SilenceRules.decide(true, active = false, currentFilter = INTERRUPTION_FILTER_PRIORITY)
        )
    }

    @Test
    fun `sound turned on by the user during an event is respected`() {
        assertEquals(
            SilenceDecision(active = true),
            SilenceRules.decide(true, active = true, currentFilter = INTERRUPTION_FILTER_ALL)
        )
        assertEquals(
            SilenceDecision(active = false),
            SilenceRules.decide(false, active = true, currentFilter = INTERRUPTION_FILTER_ALL)
        )
    }

    @Test
    fun `mode the user switched to during an event survives its end`() {
        assertEquals(
            SilenceDecision(active = false),
            SilenceRules.decide(false, active = true, currentFilter = INTERRUPTION_FILTER_PRIORITY)
        )
    }
}
