package dev.unmango.slip

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The token goes through EncryptedSharedPreferences, which derives a key in the
 * hardware keystore. That either works on the device or it does not, and it
 * cannot be tested anywhere else.
 */
@RunWith(AndroidJUnit4::class)
class SettingsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun settingsRoundTripThroughTheKeystore() {
        val settings = Settings(
            remoteUrl = "https://example.com/notebook.git",
            branch = "trunk",
            username = "x-access-token",
            token = "a-secret-nobody-should-read-off-the-disk",
            authorName = "Emulator",
            authorEmail = "emulator@example.com",
        )

        Notebook.save(context, settings)

        assertEquals(settings, Notebook.settings(context))
    }

    @Test
    fun anEmptyBranchFallsBackToMain() {
        Notebook.save(context, Settings(branch = ""))

        assertEquals("main", Notebook.settings(context).branch)
    }
}
