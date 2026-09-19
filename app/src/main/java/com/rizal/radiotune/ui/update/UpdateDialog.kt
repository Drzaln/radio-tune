package com.rizal.radiotune.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rizal.radiotune.BuildConfig
import com.rizal.radiotune.R

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val update = state.update ?: return

    AlertDialog(
        onDismissRequest = { if (!state.downloading) onDismiss() },
        icon = { Icon(painterResource(R.drawable.ic_download), contentDescription = null) },
        title = { Text("Update available") },
        text = {
            Column {
                Text(
                    "RadioTune ${update.versionName} is available. " +
                        "You have ${BuildConfig.VERSION_NAME}.",
                )
                if (state.downloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Downloading… ${state.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                state.error?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onInstall, enabled = !state.downloading) {
                Text(if (state.downloading) "Downloading" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.downloading) {
                Text("Later")
            }
        },
    )
}
