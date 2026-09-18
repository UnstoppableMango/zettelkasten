package dev.unmango.slip

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import mobile.Client
import mobile.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The publish path, end to end, on a real Android runtime.
 *
 * The remote is a git daemon on the host, which android/emulator-test.sh
 * starts and passes in. Without it these skip: a bare
 * `gradle connectedDebugAndroidTest` should still run everything it can.
 */
@RunWith(AndroidJUnit4::class)
class PublishTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val remote =
        InstrumentationRegistry.getArguments().getString("slipRemote").orEmpty()

    /**
     * A branch per test. The daemon serves one repository for the whole run,
     * and notes are only ever added, so tests sharing a branch see each other's
     * notes. An unused branch is also the empty-remote path, which is the state
     * a notebook starts in anyway.
     */
    private val branch = "test-" + UUID.randomUUID()

    @Before
    fun emptyNotebook() {
        assumeTrue("no remote; run android/emulator-test.sh", remote.isNotEmpty())

        Notebook.dir(context).deleteRecursively()
        scratch().deleteRecursively()

        Notebook.save(
            context,
            Settings(
                remoteUrl = remote,
                branch = branch,
                authorName = "Emulator",
                authorEmail = "emulator@example.com",
            ),
        )
    }

    @Test
    fun aCaptureReachesTheRemote() {
        val client = Notebook.client(context)
        client.capture("a thought from the emulator\n")

        assertEquals(1L, client.pending())

        client.sync()

        assertEquals("still pending after a sync: ${client.lastError()}", 0L, client.pending())

        // An empty second notebook has to find the note. Nothing else proves
        // the push landed rather than the first notebook forgetting about it.
        val other = scratch().apply { mkdirs() }
        clientIn(other).sync()

        val notes = other.listFiles { f -> f.name.endsWith(".md") }.orEmpty()

        assertEquals("the remote is carrying ${notes.size} notes", 1, notes.size)
        assertTrue(notes[0].readText().endsWith("a thought from the emulator\n"))
    }

    /** Publishing twice must not publish twice. */
    @Test
    fun syncingAgainChangesNothing() {
        val client = Notebook.client(context)
        client.capture("once\n")
        client.sync()
        client.sync()

        val other = scratch().apply { mkdirs() }
        clientIn(other).sync()

        assertEquals(1, other.listFiles { f -> f.name.endsWith(".md") }.orEmpty().size)
    }

    /**
     * Two notebooks capturing in the same minute collide on the id. Both
     * thoughts have to survive, which is the one piece of the sync model that
     * is worth confirming somewhere other than a unit test.
     */
    @Test
    fun twoNotebooksCapturingAtOnceBothSurvive() {
        val other = scratch().apply { mkdirs() }
        val theirs = clientIn(other)

        theirs.capture("from the other notebook\n")
        theirs.sync()

        val mine = Notebook.client(context)
        mine.capture("from this notebook\n")
        mine.sync()

        val third = File(context.cacheDir, "third").apply { deleteRecursively(); mkdirs() }
        clientIn(third).sync()

        val bodies = third.listFiles { f -> f.name.endsWith(".md") }.orEmpty()
            .map { it.readText() }

        assertEquals("published ${bodies.size} notes", 2, bodies.size)
        assertTrue(bodies.any { it.endsWith("from the other notebook\n") })
        assertTrue(bodies.any { it.endsWith("from this notebook\n") })
    }

    private fun scratch() = File(context.cacheDir, "other")

    private fun clientIn(dir: File) =
        Client(
            Config().apply {
                this.dir = dir.absolutePath
                remoteURL = remote
                branch = this@PublishTest.branch
                authorName = "Scratch"
                authorEmail = "scratch@example.com"
            }
        )
}
