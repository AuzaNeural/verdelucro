package com.auza.giguclone.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.auza.giguclone.GigUCloneApplication
import com.auza.giguclone.R
import com.auza.giguclone.domain.model.*
import java.util.Locale

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var ttsManager: TtsManager? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val ACTION_SHOW_OFFER = "com.auza.giguclone.SHOW_OFFER"
        const val ACTION_HIDE_OFFER = "com.auza.giguclone.HIDE_OFFER"
        var currentEvaluation: EvaluationResult? = null
        var overlaySettings = OverlaySettings()
        var voiceSettings = VoiceSettings()

        fun show(context: Context, evaluation: EvaluationResult) {
            currentEvaluation = evaluation
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_SHOW_OFFER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun hide(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_HIDE_OFFER
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ttsManager = TtsManager(this)
        startForeground(1001, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OFFER -> {
                currentEvaluation?.let { displayOverlay(it) }
            }
            ACTION_HIDE_OFFER -> {
                removeOverlay()
            }
        }
        return START_STICKY
    }

    private fun displayOverlay(evaluation: EvaluationResult) {
        if (overlayView != null) {
            updateOverlayData(evaluation)
            return
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_traffic_light_card, null)
        setupTouchListener(overlayView!!)
        updateOverlayData(evaluation)

        windowManager?.addView(overlayView, params)

        // Se configurado como Duração Temporária, fecha automaticamente após 10 segundos
        if (overlaySettings.durationMode == DurationMode.TEMPORARY) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ removeOverlay() }, 10000L)
        }
    }

    private fun updateOverlayData(evaluation: EvaluationResult) {
        overlayView?.let { view ->
            val cardContainer = view.findViewById<View>(R.id.cardContainer)
            val txtRateKm = view.findViewById<TextView>(R.id.txtRateKm)
            val txtRateHour = view.findViewById<TextView>(R.id.txtRateHour)
            val txtNetProfit = view.findViewById<TextView>(R.id.txtNetProfit)
            val txtProfitPercent = view.findViewById<TextView>(R.id.txtProfitPercent)
            val txtPlatform = view.findViewById<TextView>(R.id.txtPlatformBadge)
            val txtTripStats = view.findViewById<TextView>(R.id.txtTripStats)
            val txtSurge = view.findViewById<TextView>(R.id.txtSurgeBadge)
            val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
            val btnMaps = view.findViewById<View>(R.id.btnOpenMaps)
            val btnCamera = view.findViewById<TextView>(R.id.btnSecretCamera)
            val btnSpeak = view.findViewById<View>(R.id.btnSpeak)

            val barKm = view.findViewById<View>(R.id.indicatorBarKm)
            val barHour = view.findViewById<View>(R.id.indicatorBarHour)
            val barProfit = view.findViewById<View>(R.id.indicatorBarProfit)
            val barPercent = view.findViewById<View>(R.id.indicatorBarPercent)

            // Valores formatados conforme o layout real do GigU (Imagem 1)
            txtRateKm?.text = String.format(Locale.US, "%.2f", evaluation.ratePerKm).replace(".", ",")
            txtRateHour?.text = String.format(Locale.US, "%.2f", evaluation.ratePerHour).replace(".", ",")
            txtNetProfit?.text = String.format(Locale.US, "%.2f", evaluation.netProfit).replace(".", ",")
            txtProfitPercent?.text = "${evaluation.profitPercentage.toInt()}%"

            txtPlatform?.text = evaluation.offer.platform.displayName
            txtTripStats?.text = "${evaluation.offer.formattedDuration} • ${String.format(Locale.US, "%.1fkm", evaluation.offer.totalDistanceKm).replace(".", ",")}"

            // Tarifa Dinâmica / Mais Alta
            if (evaluation.offer.surgeMultiplier != null) {
                txtSurge?.visibility = View.VISIBLE
                txtSurge?.text = "⚡ x${evaluation.offer.surgeMultiplier}"
            } else {
                txtSurge?.visibility = View.GONE
            }

            // Cor e Borda do Semáforo
            val colorInt = when (evaluation.color) {
                TrafficLightColor.GREEN -> Color.parseColor("#00C853")
                TrafficLightColor.YELLOW -> Color.parseColor("#FFD600")
                TrafficLightColor.RED -> Color.parseColor("#D50000")
                TrafficLightColor.PURPLE_RISK -> Color.parseColor("#9C27B0")
            }

            // Atualiza cor das barras verticais indicadoras
            listOf(barKm, barHour, barProfit, barPercent).forEach { bar ->
                bar?.setBackgroundColor(colorInt)
            }

            // Borda colorida arredondada
            val borderDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.WHITE)
                setStroke(6, colorInt)
            }
            cardContainer?.background = borderDrawable

            // Ações
            btnClose?.setOnClickListener { removeOverlay() }

            btnMaps?.setOnClickListener {
                openMapNavigation(evaluation.offer.destinationAddress)
            }

            btnCamera?.text = if (SecretCameraService.isCurrentlyRecording) "🔴 Gravando" else "📹 Câmera"
            btnCamera?.setOnClickListener {
                SecretCameraService.toggle(this)
                btnCamera.text = if (SecretCameraService.isCurrentlyRecording) "🔴 Gravando" else "📹 Câmera"
                Toast.makeText(
                    this,
                    if (SecretCameraService.isCurrentlyRecording) "Gravação discreta iniciada!" else "Gravação finalizada.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            btnSpeak?.setOnClickListener {
                ttsManager?.speakEvaluation(evaluation, voiceSettings)
            }
        }
    }

    private fun openMapNavigation(address: String?) {
        val target = if (!address.isNullOrBlank()) address else "Destino"
        val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(target))
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Nenhum app de mapa instalado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTouchListener(view: View) {
        var initialY = 0
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params?.y ?: 0
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, GigUCloneApplication.CHANNEL_ID)
            .setContentTitle("GigU Clone Pro Ativo")
            .setContentText("Avaliando ofertas com semáforo em tempo real")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        removeOverlay()
        ttsManager?.destroy()
        ttsManager = null
        super.onDestroy()
    }
}
