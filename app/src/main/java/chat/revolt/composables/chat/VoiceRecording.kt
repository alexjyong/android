package chat.revolt.composables.chat

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import chat.revolt.R
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

sealed class VoiceRecordingState {
    data object Idle : VoiceRecordingState()
    data object Ready : VoiceRecordingState()
    data object Recording : VoiceRecordingState()
    data class Recorded(val file: File, val duration: Long) : VoiceRecordingState()
}

class VoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0

    fun startRecording(): File? {
        if (!hasAudioPermission()) {
            return null
        }

        return try {
            val outputDir = File(context.cacheDir, "voice_messages")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(outputDir, "voice_message_$timestamp.ogg")
            outputFile = file

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.OGG)
                setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(48000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            startTime = System.currentTimeMillis()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun stopRecording(): Pair<File?, Long> {
        val duration = System.currentTimeMillis() - startTime
        val file = outputFile

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaRecorder = null
        return Pair(file, duration)
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED
    }
}

@Composable
fun VoiceRecordingWaveform(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "waveform_animation"
    )

    val amplitudes = remember {
        mutableStateOf(List(20) { Random.nextFloat() * 0.8f + 0.2f })
    }

    if (isRecording) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(100)
                amplitudes.value = List(20) { Random.nextFloat() * 0.8f + 0.2f }
            }
        }
    }

    Canvas(
        modifier = modifier.height(40.dp)
    ) {
        drawWaveform(
            amplitudes = amplitudes.value,
            progress = if (isRecording) animationProgress else 0f,
            color = if (isRecording) Color.Red else Color.Gray
        )
    }
}

private fun DrawScope.drawWaveform(
    amplitudes: List<Float>,
    progress: Float,
    color: Color
) {
    val barWidth = size.width / amplitudes.size
    val centerY = size.height / 2

    amplitudes.forEachIndexed { index, amplitude ->
        val barHeight = amplitude * size.height * 0.8f
        val x = index * barWidth + barWidth / 2
        val alpha = if (progress > index.toFloat() / amplitudes.size) 1f else 0.3f

        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(x, centerY - barHeight / 2),
            end = Offset(x, centerY + barHeight / 2),
            strokeWidth = barWidth * 0.6f
        )
    }
}

@Composable
fun VoiceRecordingButton(
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onShowRecordingUI: () -> Unit = {},
    state: VoiceRecordingState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onShowRecordingUI()
        } else {
            permissionDenied = true
        }
    }

    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PermissionChecker.PERMISSION_GRANTED

    Icon(
        painter = painterResource(R.drawable.icn_mic_24dp),
        contentDescription = when (state) {
            is VoiceRecordingState.Idle -> stringResource(R.string.start_voice_recording)
            is VoiceRecordingState.Ready -> stringResource(R.string.start_voice_recording)
            is VoiceRecordingState.Recording -> stringResource(R.string.stop_voice_recording)
            is VoiceRecordingState.Recorded -> stringResource(R.string.start_voice_recording)
        },
        tint = when (state) {
            is VoiceRecordingState.Idle -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            is VoiceRecordingState.Ready -> MaterialTheme.colorScheme.primary
            is VoiceRecordingState.Recording -> Color.Red
            is VoiceRecordingState.Recorded -> MaterialTheme.colorScheme.primary
        },
        modifier = modifier
            .clip(CircleShape)
            .size(32.dp)
            .clickable {
                when (state) {
                    is VoiceRecordingState.Idle -> {
                        if (hasPermission) {
                            onShowRecordingUI()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    is VoiceRecordingState.Ready -> {
                        onStartRecording()
                    }
                    is VoiceRecordingState.Recording -> {
                        onStopRecording()
                    }
                    is VoiceRecordingState.Recorded -> {
                        onShowRecordingUI()
                    }
                }
            }
            .padding(4.dp)
    )
}

@Composable
fun VoiceRecordingUI(
    state: VoiceRecordingState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDeleteRecording: () -> Unit,
    onShowRecordingUI: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (state) {
        is VoiceRecordingState.Idle -> {
            VoiceRecordingButton(
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onShowRecordingUI = onShowRecordingUI,
                state = state,
                modifier = modifier
            )
        }

        is VoiceRecordingState.Ready -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .clickable { onStartRecording() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icn_mic_24dp),
                        contentDescription = stringResource(R.string.start_voice_recording),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Tap to start recording",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDeleteRecording() }
                )
            }
        }

        is VoiceRecordingState.Recording -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .clickable { onStopRecording() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.White, RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                VoiceRecordingWaveform(
                    isRecording = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = "Recording...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        is VoiceRecordingState.Recorded -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.icn_mic_24dp),
                    contentDescription = "Voice message",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                VoiceRecordingWaveform(
                    isRecording = false,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = "${state.duration / 1000}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_delete),
                    contentDescription = "Delete recording",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDeleteRecording() }
                )
            }
        }
    }
}