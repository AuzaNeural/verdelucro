package com.auza.giguclone.data.parser

import android.view.accessibility.AccessibilityNodeInfo
import com.auza.giguclone.domain.model.PlatformType
import com.auza.giguclone.domain.model.RideOffer
import java.util.UUID
import java.util.regex.Pattern

object RideOfferParser {

    private val MONEY_PATTERN = Pattern.compile("""R\$\s*([0-9]+[,\.][0-9]{2})""", Pattern.CASE_INSENSITIVE)
    private val DISTANCE_PATTERN = Pattern.compile("""([0-9]+[,\.][0-9]*)\s*km""", Pattern.CASE_INSENSITIVE)
    private val TIME_PATTERN = Pattern.compile("""([0-9]+)\s*min""", Pattern.CASE_INSENSITIVE)
    private val RATING_PATTERN = Pattern.compile("""([4-5][,\.][0-9]{1,2})\s*(?:★|estrelas)?""", Pattern.CASE_INSENSITIVE)
    private val SURGE_MULTIPLIER_PATTERN = Pattern.compile("""(?:Preço\s*)?[xX]\s*([1-3][,\.][0-9]{1,2})""")

    /**
     * Analisa o nó raiz da janela de acessibilidade para identificar se é uma oferta ativa
     * de qualquer um dos 6 apps suportados (Uber, 99, iFood, inDrive, KeeTa, Urbano Norte).
     */
    fun parse(rootNode: AccessibilityNodeInfo, packageName: String): RideOffer? {
        val platform = resolvePlatform(packageName)
        if (platform == PlatformType.UNKNOWN) return null

        val textElements = mutableListOf<String>()
        collectAllText(rootNode, textElements)

        val fullText = textElements.joinToString(" \n ")

        // 1. Extração do Valor da Corrida (R$)
        val fare = extractMoney(fullText) ?: return null

        // 2. Extração de Distâncias (busca + viagem)
        val distances = extractDistances(fullText)
        val pickupDistance = if (distances.size > 1) distances[0] else 0.0
        val tripDistance = if (distances.size > 1) distances[1] else (distances.firstOrNull() ?: 3.0)

        // 3. Extração de Tempos Estimados
        val times = extractTimes(fullText)
        val pickupTime = if (times.size > 1) times[0] else 2
        val tripTime = if (times.size > 1) times[1] else (times.firstOrNull() ?: 10)

        // 4. Nota do passageiro
        val rating = extractRating(fullText)

        // 5. Dinâmico / Tarifa Mais Alta (Surge)
        val surgeMultiplier = extractSurgeMultiplier(fullText)
        val surgeAdditional = extractSurgeAdditional(fullText, fare)

        // 6. Extração de Endereços de Origem e Destino
        val (pickupAddr, destAddr) = extractAddresses(textElements)

        return RideOffer(
            id = UUID.randomUUID().toString(),
            platform = platform,
            fare = fare,
            pickupDistanceKm = pickupDistance,
            tripDistanceKm = tripDistance,
            pickupTimeMinutes = pickupTime,
            tripTimeMinutes = tripTime,
            pickupAddress = pickupAddr,
            destinationAddress = destAddr,
            passengerRating = rating,
            surgeMultiplier = surgeMultiplier,
            surgeAdditionalValue = surgeAdditional
        )
    }

    private fun resolvePlatform(packageName: String): PlatformType {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("ubercab") -> PlatformType.UBER
            pkg.contains("taxis99") -> PlatformType.NOVENTA_E_NOVE
            pkg.contains("ifood") -> PlatformType.IFOOD
            pkg.contains("indrive") || pkg.contains("indriver") -> PlatformType.INDRIVE
            pkg.contains("keeta") -> PlatformType.KEETA
            pkg.contains("urbanonorte") -> PlatformType.URBANO_NORTE
            else -> PlatformType.UNKNOWN
        }
    }

    private fun collectAllText(node: AccessibilityNodeInfo?, output: MutableList<String>) {
        if (node == null) return

        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { output.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { output.add(it) }

        for (i in 0 until node.childCount) {
            collectAllText(node.getChild(i), output)
        }
    }

    private fun extractMoney(text: String): Double? {
        val matcher = MONEY_PATTERN.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(".", "")?.replace(",", ".") ?: return null
            return raw.toDoubleOrNull()
        }
        return null
    }

    private fun extractDistances(text: String): List<Double> {
        val list = mutableListOf<Double>()
        val matcher = DISTANCE_PATTERN.matcher(text)
        while (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", ".") ?: continue
            raw.toDoubleOrNull()?.let { list.add(it) }
        }
        return list
    }

    private fun extractTimes(text: String): List<Int> {
        val list = mutableListOf<Int>()
        val matcher = TIME_PATTERN.matcher(text)
        while (matcher.find()) {
            val raw = matcher.group(1) ?: continue
            raw.toIntOrNull()?.let { list.add(it) }
        }
        return list
    }

    private fun extractRating(text: String): Double? {
        val matcher = RATING_PATTERN.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", ".") ?: return null
            val value = raw.toDoubleOrNull() ?: return null
            if (value in 4.0..5.0) return value
        }
        return null
    }

    private fun extractSurgeMultiplier(text: String): Double? {
        val matcher = SURGE_MULTIPLIER_PATTERN.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(",", ".") ?: return null
            return raw.toDoubleOrNull()
        }
        return null
    }

    private fun extractSurgeAdditional(text: String, mainFare: Double): Double? {
        // Procura por segundo valor em R$ após menção de tarifa alta ou dinâmico
        if (text.contains("Tarifa mais alta") || text.contains("Preço x")) {
            val matcher = MONEY_PATTERN.matcher(text)
            var count = 0
            while (matcher.find()) {
                count++
                if (count == 2) {
                    val raw = matcher.group(1)?.replace(".", "")?.replace(",", ".") ?: return null
                    val candidate = raw.toDoubleOrNull()
                    if (candidate != null && candidate < mainFare) return candidate
                }
            }
        }
        return null
    }

    private fun extractAddresses(elements: List<String>): Pair<String?, String?> {
        var pickup: String? = null
        var destination: String? = null

        for (i in elements.indices) {
            val item = elements[i].lowercase()
            if (item.contains("para") || item.contains("destino") || item.contains("desembarque")) {
                if (i + 1 < elements.size) destination = elements[i + 1]
            } else if (item.contains("embarque") || item.contains("buscar")) {
                if (i + 1 < elements.size) pickup = elements[i + 1]
            }
        }

        if (destination == null && elements.size >= 2) {
            destination = elements.lastOrNull { it.length > 5 && !it.contains("R$") && !it.contains("km") }
        }

        return pickup to destination
    }
}
