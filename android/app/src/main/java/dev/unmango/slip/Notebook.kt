package dev.unmango.slip

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import mobile.Client
import mobile.Config

/** Settings is everything the notebook needs that is not a note. */
data class Settings(
    val remoteUrl: String = "",
    val branch: String = "main",
    val username: String = "",
    val token: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
)

/**
 * Notebook is the only thing that knows where notes live and how to reach the
 * remote. Everything else asks it for a client.
 */
object Notebook {

    private const val FILE = "notebook"

    private const val REMOTE_URL = "remote_url"
    private const val BRANCH = "branch"
    private const val USERNAME = "username"
    private const val TOKEN = "token"
    private const val AUTHOR_NAME = "author_name"
    private const val AUTHOR_EMAIL = "author_email"

    private var cached: SharedPreferences? = null

    /**
     * dir is app-scoped external storage rather than private storage, so the
     * notebook is visible over MTP and to a file manager. It is not a folder
     * picked through the storage access framework, because go-git needs a real
     * filesystem path and SAF does not give one.
     */
    fun dir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir

        return File(base, "notebook").apply { mkdirs() }
    }

    fun settings(context: Context): Settings {
        val prefs = prefs(context)

        return Settings(
            remoteUrl = prefs.getString(REMOTE_URL, "").orEmpty(),
            branch = prefs.getString(BRANCH, "main").orEmpty(),
            username = prefs.getString(USERNAME, "").orEmpty(),
            token = prefs.getString(TOKEN, "").orEmpty(),
            authorName = prefs.getString(AUTHOR_NAME, "").orEmpty(),
            authorEmail = prefs.getString(AUTHOR_EMAIL, "").orEmpty(),
        )
    }

    fun save(context: Context, settings: Settings) {
        prefs(context).edit().apply {
            putString(REMOTE_URL, settings.remoteUrl.trim())
            putString(BRANCH, settings.branch.trim().ifEmpty { "main" })
            putString(USERNAME, settings.username.trim())
            putString(TOKEN, settings.token.trim())
            putString(AUTHOR_NAME, settings.authorName.trim())
            putString(AUTHOR_EMAIL, settings.authorEmail.trim())
        }.apply()
    }

    /**
     * client is built fresh each time, because settings change and a Client
     * holds the ones it was built with. It is cheap: it opens nothing until it
     * is asked to.
     */
    fun client(context: Context): Client {
        val settings = settings(context)

        return Client(
            Config().apply {
                dir = this@Notebook.dir(context).absolutePath
                remoteURL = settings.remoteUrl
                branch = settings.branch
                username = settings.username
                token = settings.token
                authorName = settings.authorName
                authorEmail = settings.authorEmail
            }
        )
    }

    /**
     * prefs holds a personal access token, so it is encrypted against a key in
     * the hardware keystore rather than left in the app's plain preferences,
     * which a rooted device or an adb backup reads straight off the disk.
     *
     * Building it derives a key, so the instance is kept.
     */
    private fun prefs(context: Context): SharedPreferences =
        cached ?: synchronized(this) {
            cached ?: run {
                val app = context.applicationContext
                val key = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

                EncryptedSharedPreferences.create(
                    app,
                    FILE,
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                ).also { cached = it }
            }
        }
}
