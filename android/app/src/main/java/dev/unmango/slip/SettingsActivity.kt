package dev.unmango.slip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * SettingsActivity is where the notebook's remote is configured. It is a form
 * because there is nothing to discover: the person knows their own repository.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { MaterialTheme { SettingsScreen(onDone = ::finish) } }
    }
}

@Composable
private fun SettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { Notebook.settings(context) }

    var remoteUrl by remember { mutableStateOf(saved.remoteUrl) }
    var branch by remember { mutableStateOf(saved.branch) }
    var username by remember { mutableStateOf(saved.username) }
    var token by remember { mutableStateOf(saved.token) }
    var authorName by remember { mutableStateOf(saved.authorName) }
    var authorEmail by remember { mutableStateOf(saved.authorEmail) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Field("Remote URL", remoteUrl) { remoteUrl = it }
        Field("Branch", branch) { branch = it }

        // A token over HTTPS rather than a key over SSH: go-git needs a private
        // key in process memory, which gives up the hardware keystore.
        Field("Username", username) { username = it }
        Field("Token", token, secret = true) { token = it }

        Field("Author name", authorName) { authorName = it }
        Field("Author email", authorEmail) { authorEmail = it }

        Text(
            text = "Notes: ${Notebook.dir(context).absolutePath}",
            style = MaterialTheme.typography.bodySmall,
        )

        Button(
            onClick = {
                Notebook.save(
                    context,
                    Settings(remoteUrl, branch, username, token, authorName, authorEmail),
                )
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    secret: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation =
            if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}
