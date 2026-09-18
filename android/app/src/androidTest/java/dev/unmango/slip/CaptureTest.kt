package dev.unmango.slip

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What compiling the app does not prove: that libgojni.so loads, that go-git
 * can work on the filesystem Android hands out, and that an error crossing the
 * JNI boundary arrives as something readable rather than a crash.
 */
@RunWith(AndroidJUnit4::class)
class CaptureTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun emptyNotebook() {
        Notebook.dir(context).deleteRecursively()
        Notebook.save(context, Settings(authorName = "Emulator", authorEmail = "emulator@example.com"))
    }

    @Test
    fun captureWritesANoteWithItsBodyVerbatim() {
        val body = "a thought from the emulator\n\nand the rest of it.\n"

        val path = Notebook.client(context).capture(body)
        val written = java.io.File(path).readText()

        assertTrue("wrote no frontmatter: $written", written.startsWith("---\n"))
        assertTrue("lost the body: $written", written.endsWith(body))
        assertTrue("wrote outside the notebook: $path", path.startsWith(Notebook.dir(context).absolutePath))
    }

    @Test
    fun captureRejectsWhitespace() {
        try {
            Notebook.client(context).capture("   \n\t")
            throw AssertionError("capturing whitespace returned a path")
        } catch (e: Exception) {
            assertTrue(e.message.orEmpty().isNotEmpty())
        }
    }

    /** Counting pending notes opens a repository, so this is go-git's first contact with the device. */
    @Test
    fun pendingCountsAnUnpublishedCapture() {
        val client = Notebook.client(context)

        assertEquals(0L, client.pending())

        client.capture("one\n")
        client.capture("two\n")

        assertEquals(2L, Notebook.client(context).pending())
    }

    @Test
    fun syncWithoutARemoteSaysWhyRatherThanCrashing() {
        val client = Notebook.client(context)
        client.capture("nowhere to go\n")

        try {
            client.sync()
            throw AssertionError("syncing without a remote succeeded")
        } catch (e: Exception) {
            assertTrue("empty message", e.message.orEmpty().isNotEmpty())
        }

        assertTrue("LastError is empty", client.lastError().isNotEmpty())
    }
}
