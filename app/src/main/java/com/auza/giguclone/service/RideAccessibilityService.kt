package com.auza.giguclone.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.auza.giguclone.data.db.RideDatabaseHelper
import com.auza.giguclone.data.parser.RideOfferParser
import com.auza.giguclone.domain.calculator.ProfitabilityCalculator
import com.auza.giguclone.domain.model.*

class RideAccessibilityService : AccessibilityService() {

    private var ttsManager: TtsManager? = null
    private var dbHelper: RideDatabaseHelper? = null
    private var lastEvaluatedFare: Double = 0.0
    private var lastEvaluationTime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "RideAccessibilitySvc"
        var isServiceRunning = false
            private set

        var vehicleConfig = VehicleConfig()
        var driverTargets = DriverTargets()
        var safetySettings = SafetySettings()
        var autoRejectSettings = AutoRejectSettings()
        var voiceSettings = VoiceSettings()
        var overlaySettings = OverlaySettings()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        ttsManager = TtsManager(this)
        dbHelper = RideDatabaseHelper(this)
        Log.i(TAG, "Serviço de Acessibilidade GigU Clone Pro Conectado.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val rootNode = rootInActiveWindow ?: return

        try {
            val offer = RideOfferParser.parse(rootNode, packageName) ?: return

            val now = System.currentTimeMillis()
            if (offer.fare == lastEvaluatedFare && (now - lastEvaluationTime) < 5000) {
                return
            }

            lastEvaluatedFare = offer.fare
            lastEvaluationTime = now

            // 1. Avaliação completa
            val evaluation = ProfitabilityCalculator.evaluate(
                offer = offer,
                vehicle = vehicleConfig,
                targets = driverTargets,
                safety = safetySettings
            )

            // 2. Grava histórico
            dbHelper?.insertEvaluation(evaluation)

            // 3. Exibe Semáforo Flutuante
            FloatingOverlayService.overlaySettings = overlaySettings
            FloatingOverlayService.voiceSettings = voiceSettings
            FloatingOverlayService.show(this, evaluation)

            // 4. Anúncio em Voz inteligente
            ttsManager?.speakEvaluation(evaluation, voiceSettings)

            // 5. Auto-Rejeição de Corridas Vermelhas ou de Risco
            if (autoRejectSettings.enabled) {
                val shouldReject = (evaluation.color == TrafficLightColor.RED && autoRejectSettings.rejectRedOnly) ||
                        (evaluation.color == TrafficLightColor.PURPLE_RISK && autoRejectSettings.rejectRiskArea)

                if (shouldReject) {
                    val delayMs = autoRejectSettings.delaySeconds * 1000L
                    handler.postDelayed({
                        performAutoReject()
                    }, delayMs)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar nó de chamada", e)
        } finally {
            rootNode.recycle()
        }
    }

    private fun performAutoReject() {
        val root = rootInActiveWindow ?: return
        try {
            val declineTerms = listOf("Recusar", "Não aceitar", "Ignorar", "Decline", "✕")
            for (term in declineTerms) {
                val matchingNodes = root.findAccessibilityNodeInfosByText(term)
                for (node in matchingNodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.i(TAG, "Auto-rejeição executada: $term")
                        FloatingOverlayService.hide(this)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha na auto-rejeição", e)
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Acessibilidade Interrompida.")
    }

    override fun onDestroy() {
        isServiceRunning = false
        ttsManager?.destroy()
        ttsManager = null
        dbHelper?.close()
        super.onDestroy()
    }
}
