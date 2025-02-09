package app.myzel394.alibi.ui.models

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaRecorder
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import app.myzel394.alibi.db.LastRecording
import app.myzel394.alibi.enums.RecorderState
import app.myzel394.alibi.services.AudioRecorderService
import app.myzel394.alibi.services.RecorderService

class AudioRecorderModel: ViewModel() {
    var recorderState by mutableStateOf(RecorderState.IDLE)
        private set
    var recordingTime by mutableStateOf<Long?>(null)
        private set
    var amplitudes by mutableStateOf<List<Int>>(emptyList())
        private set

    var onAmplitudeChange: () -> Unit = {}

    val isInRecording: Boolean
        get() = recorderState !== RecorderState.IDLE && recordingTime != null

    val isPaused: Boolean
        get() = recorderState === RecorderState.PAUSED

    val progress: Float
        get() = (recordingTime!! / recorderService!!.settings!!.maxDuration).toFloat()

    var recorderService: AudioRecorderService? = null
        private set

    var lastRecording: LastRecording? by mutableStateOf<LastRecording?>(null)
        private set

    var onRecordingSave: () -> Unit = {}
    var onError: () -> Unit = {}

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            recorderService = ((service as RecorderService.RecorderBinder).getService() as AudioRecorderService).also {recorder ->
                recorder.onStateChange = { state ->
                    recorderState = state
                }
                recorder.onRecordingTimeChange = { time ->
                    recordingTime = time
                }
                recorder.onAmplitudeChange = { amps ->
                    amplitudes = amps
                    onAmplitudeChange()
                }
                recorder.onError = {
                    recorderService!!.createLastRecording()
                    onError()
                }
            }.also {
                it.startRecording()

                recorderState = it.state
                recordingTime = it.recordingTime
                amplitudes = it.amplitudes
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            recorderService = null
            reset()
        }
    }

    fun reset() {
        recorderState = RecorderState.IDLE
        recordingTime = null
        amplitudes = emptyList()
    }

    fun startRecording(context: Context) {
        runCatching {
            context.unbindService(connection)
        }

        val intent = Intent(context, AudioRecorderService::class.java)
        ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun stopRecording(context: Context, saveAsLastRecording: Boolean = true) {
        if (saveAsLastRecording) {
            lastRecording = recorderService!!.createLastRecording()
        }

        runCatching {
            context.unbindService(connection)
        }

        val intent = Intent(context, AudioRecorderService::class.java)
        context.stopService(intent)

        reset()
    }

    fun pauseRecording() {
        recorderService!!.changeState(RecorderState.PAUSED)
    }

    fun resumeRecording() {
        recorderService!!.changeState(RecorderState.RECORDING)
    }

    fun setMaxAmplitudesAmount(amount: Int) {
        recorderService?.amplitudesAmount = amount
    }

    fun bindToService(context: Context) {
        Intent(context, AudioRecorderService::class.java).also { intent ->
            context.bindService(intent, connection, 0)
        }
    }

    fun unbindFromService(context: Context) {
        runCatching {
            context.unbindService(connection)
        }
    }
}