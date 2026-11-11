package com.auza.giguclone.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import com.auza.giguclone.domain.model.EvaluationResult
import com.auza.giguclone.domain.model.TrafficLightColor
import com.auza.giguclone.domain.model.VoiceSettings
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            // Alguns dispositivos restringem instanciar ToneGenerator diretamente
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun speakEvaluation(evaluation: EvaluationResult, settings: VoiceSettings) {
        if (!isInitialized || !settings.enabled) return

        // Filtro: verifica se a categoria atual está marcada para ser lida
        val shouldSpeak = when (evaluation.color) {
            TrafficLightColor.GREEN -> settings.readGreen
            TrafficLightColor.YELLOW -> settings.readYellow
            TrafficLightColor.RED -> settings.readRed
            TrafficLightColor.PURPLE_RISK -> true // Risco sempre é anunciado por segurança
        }

        if (!shouldSpeak) return

        // Ajusta velocidade da fala
        tts?.setSpeechRate(settings.speechRate)

        // Bip sonoro prévio conforme tom escolhido
        playTone(settings.soundTone)

        val speech: String = if (evaluation.isRiskArea) {
            "Alerta de segurança! Corrida com destino em área de risco! Recomenda-se recusar."
        } else if (evaluation.isLowRating) {
            "Atenção motorista! Passageiro com nota baixa: ${evaluation.offer.passengerRating} estrelas."
        } else {
            val colorWord = when (evaluation.color) {
                TrafficLightColor.GREEN -> "Boa!"
                TrafficLightColor.YELLOW -> "Média."
                TrafficLightColor.RED -> "Ruim, prejuízo!"
                TrafficLightColor.PURPLE_RISK -> "Atenção!"
            }

            val surgeText = if (evaluation.offer.surgeMultiplier != null) {
                "Dinâmico de ${evaluation.offer.surgeMultiplier} vezes. "
            } else ""

            "${evaluation.offer.platform.displayName}. $surgeText" +
                    "Valor: ${String.format(Locale("pt", "BR"), "%.2f", evaluation.offer.fare)} reais. " +
                    "Distância: ${String.format(Locale("pt", "BR"), "%.1f", evaluation.offer.totalDistanceKm)} quilômetros. " +
                    "Ganho por quilômetro: ${String.format(Locale("pt", "BR"), "%.2f", evaluation.ratePerKm)} reais. " +
                    "Lucro líquido: ${String.format(Locale("pt", "BR"), "%.2f", evaluation.netProfit)} reais, ${evaluation.profitPercentage.toInt()} por cento. $colorWord"
        }

        tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "RIDE_OFFER_ANNOUNCEMENT")
    }

    private fun playTone(toneName: String) {
        try {
            when (toneName) {
                "Sino" -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                "Plim" -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 120)
                else -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 140) // GigU
            }
        } catch (e: Exception) {
            // Ignora falhas de áudio secundárias
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        toneGenerator?.release()
        toneGenerator = null
        isInitialized = false
    }
}
