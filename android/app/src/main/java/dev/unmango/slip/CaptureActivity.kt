package dev.unmango.slip

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CaptureActivity is the whole app. Reading, linking, and searching are zk's on
 * a machine with a keyboard; a phone is for getting the thought down.
 */
class CaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shared = sharedText(intent)

        setContent {
            MaterialTheme {
                CaptureScreen(
                    initial = shared,
                    // Arriving from a share means the person was doing
                    // something else, so capturing hands them back to it.
                    finishOnSave = shared.isNotEmpty(),
                    onFinish = ::finish,
                )
            }
        }
    }

    /**
     * sharedText reads the text another app handed over. A share from a browser
     * carries the page title as the subject and the URL as the text, and both
     * are worth keeping.
     */
    private fun sharedText(intent: Intent): String =
        when (intent.action) {
            Intent.ACTION_SEND -> {
                // Both are CharSequence, not String. An app sharing styled
                // text hands over a SpannedString, and getStringExtra answers
                // null for it, which is a share that silently captures nothing.
                val subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString().orEmpty()
                val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()

                listOf(subject, text).filter { it.isNotBlank() }.joinToString("\n\n")
            }

            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()

            else -> ""
        }
}

@Composable
private fun CaptureScreen(initial: String, finishOnSave: Boolean, onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }

    var body by remember { mutableStateOf(TextFieldValue(initial)) }
    var pending by remember { mutableStateOf(0L) }
    var status by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    suspend fun refresh() {
        withContext(Dispatchers.IO) {
            val client = Notebook.client(context)
            val count = client.pending()
            val err = client.lastError()

            withContext(Dispatchers.Main) {
                pending = count
                status = err
            }
        }
    }

    LaunchedEffect(Unit) {
        focus.requestFocus()
        refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextField(
            value = body,
            onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(focus),
        )

        Text(
            text = status.ifEmpty { "$pending pending" },
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    val text = body.text
                    saving = true

                    scope.launch {
                        try {
                            val error = withContext(Dispatchers.IO) {
                                runCatching { Notebook.client(context).capture(text) }
                                    .exceptionOrNull()
                            }

                            if (error != null) {
                                status = error.message.orEmpty()
                                return@launch
                            }

                            body = TextFieldValue("")
                            SyncWorker.enqueue(context)
                            refresh()

                            if (finishOnSave) onFinish()
                        } finally {
                            // Cleared on every path. The field is only emptied
                            // on success, so a second tap after a failure has
                            // to be able to retry the same text.
                            saving = false
                        }
                    }
                },
                // Two quick taps would otherwise launch two coroutines holding
                // the same text, before the first has emptied the field, and
                // capture the thought twice.
                enabled = body.text.isNotBlank() && !saving,
            ) {
                Text("Save")
            }

            TextButton(onClick = { SyncWorker.enqueue(context) }) {
                Text("Sync")
            }

            TextButton(
                onClick = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }
            ) {
                Text("Notebook")
            }
        }
    }
}
