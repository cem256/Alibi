package app.myzel394.alibi.ui.components.AudioRecorder.molecules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.myzel394.alibi.R
import app.myzel394.alibi.services.RecorderService
import app.myzel394.alibi.ui.BIG_PRIMARY_BUTTON_SIZE
import app.myzel394.alibi.ui.components.AudioRecorder.atoms.ConfirmDeletionDialog
import app.myzel394.alibi.ui.components.AudioRecorder.atoms.RealtimeAudioVisualizer
import app.myzel394.alibi.ui.components.AudioRecorder.atoms.SaveRecordingButton
import app.myzel394.alibi.ui.components.atoms.Pulsating
import app.myzel394.alibi.ui.models.AudioRecorderModel
import app.myzel394.alibi.ui.utils.KeepScreenOn
import app.myzel394.alibi.ui.utils.formatDuration
import kotlinx.coroutines.delay
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun RecordingStatus(
    audioRecorder: AudioRecorderModel,
) {
    val context = LocalContext.current

    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(900)
        }
    }

    // Only show animation when the recording has just started
    val recordingJustStarted = audioRecorder.recordingTime!! <= 1000L
    var progressVisible by remember { mutableStateOf(!recordingJustStarted) }
    LaunchedEffect(Unit) {
        progressVisible = true
    }

    KeepScreenOn()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {}
        RealtimeAudioVisualizer(audioRecorder = audioRecorder)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Pulsating {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = formatDuration(audioRecorder.recordingTime!!),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = progressVisible,
                enter = expandHorizontally(
                    tween(1000)
                )
            ) {
                LinearProgressIndicator(
                    progress = audioRecorder.progress,
                    modifier = Modifier
                        .width(300.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            var showDeleteDialog by remember { mutableStateOf(false) }

            if (showDeleteDialog) {
                ConfirmDeletionDialog(
                    onDismiss = {
                        showDeleteDialog = false
                    },
                    onConfirm = {
                        showDeleteDialog = false
                        audioRecorder.stopRecording(context, saveAsLastRecording = false)
                    },
                )
            }
            val label = stringResource(R.string.ui_audioRecorder_action_delete_label)
            Button(
                modifier = Modifier
                    .semantics {
                        contentDescription = label
                    },
                onClick = {
                    showDeleteDialog = true
                },
                colors = ButtonDefaults.textButtonColors(),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(label)
            }
        }

        val pauseLabel = stringResource(R.string.ui_audioRecorder_action_pause_label)
        val resumeLabel = stringResource(R.string.ui_audioRecorder_action_resume_label)
        LargeFloatingActionButton(
            modifier = Modifier
                .semantics {
                    contentDescription = if (audioRecorder.isPaused) resumeLabel else pauseLabel
                },
            onClick = {
                if (audioRecorder.isPaused) {
                    audioRecorder.resumeRecording()
                } else {
                    audioRecorder.pauseRecording()
                }
            },
        ) {
            Icon(
                if (audioRecorder.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = null,
            )
        }

        val alpha by animateFloatAsState(if (progressVisible) 1f else 0f, tween(1000))
        val label = stringResource(R.string.ui_audioRecorder_action_save_label)

        Button(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(BIG_PRIMARY_BUTTON_SIZE)
                .alpha(alpha)
                .semantics {
                    contentDescription = label
                },
            onClick = {
                runCatching {
                    audioRecorder.stopRecording(context)
                }
                audioRecorder.onRecordingSave()
            },
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.ui_audioRecorder_action_save_label))
        }
    }
}