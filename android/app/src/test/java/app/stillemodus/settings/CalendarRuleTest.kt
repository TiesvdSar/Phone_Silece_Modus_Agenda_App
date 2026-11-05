package app.stillemodus.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarRuleTest {

    @Test
    fun `keywords are split on commas, trimmed and empties dropped`() {
        assertEquals(listOf("les", "werk overleg"), CalendarRule.parseKeywords(" les, ,werk overleg ,"))
        assertEquals(emptyList<String>(), CalendarRule.parseKeywords("  "))
    }
}
