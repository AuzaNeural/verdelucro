package com.auza.giguclone.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecretCameraService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    companion object {
        private const val TAG = "SecretCameraService"
        private const val CHANNEL_ID = "gigu_security_camera"
        private const val NOTIF_ID = 2002

        const val ACTION_START_RECORDING = "com.auza.giguclone.START_CAMERA_RECORDING"
        const val ACTION_STOP_RECORDING = "com.auza.giguclone.STOP_CAMERA_RECORDING"

        var isCurrentlyRecording = false
            private set

        fun toggle(context: Context) {
            val action = if (isCurrentlyRecording) ACTION_STOP_RECORDING else ACTION_START_RECORDING
            val intent = Intent(context, SecretCameraService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, createDiscreetNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (isRecording) return

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
            val videoFile = File(storageDir, "SEGURANCA_$timeStamp.mp4")

            @Suppress("DEPRECATION")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(videoFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            isCurrentlyRecording = true
            Log.i(TAG, "Gravação de segurança iniciada: ${videoFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Falha ao iniciar gravação de segurança", e)
            stopRecording()
        }
    }

    private fun stopRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
                isCurrentlyRecording = false
                Log.i(TAG, "Gravação de segurança finalizada com sucesso.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao finalizar gravação", e)
        } finally {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Segurança do Motorista",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoramento e gravação discreta de segurança"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createDiscreetNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GigU Proteção")
            .setContentText("Monitoramento em segundo plano ativo")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}

