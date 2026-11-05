package app.stillemodus.sound

import android.media.AudioManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundControllerTest {

    @Test
    fun ringerModeIsReadable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val validModes = setOf(
            AudioManager.RINGER_MODE_SILENT,
            AudioManager.RINGER_MODE_VIBRATE,
            AudioManager.RINGER_MODE_NORMAL
        )

        assertTrue(SoundController(context).ringerMode() in validModes)
    }
}
