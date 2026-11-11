package com.auza.giguclone.domain.model

enum class PlatformType(val displayName: String, val shortName: String) {
    UBER("Uber", "Uber"),
    NOVENTA_E_NOVE("99", "99"),
    IFOOD("iFood Entregas", "iFood"),
    INDRIVE("inDrive", "inDrive"),
    KEETA("KeeTa", "KeeTa"),
    URBANO_NORTE("Urbano Norte", "Urbano"),
    UNKNOWN("Desconhecido", "Outro")
}

enum class TrafficLightColor {
    GREEN,           // Boa / Excelente
    YELLOW,          // Média / Atenção
    RED,             // Ruim / Prejuízo
    PURPLE_RISK      // Alerta de Segurança (Área de Risco / Nota Baixa)
}

enum class EmojiStyle {
    CIRCLES, // 🟢 🟡 🔴
    ICONS    // ✅ ⚠️ ❌
}

enum class DurationMode {
    TEMPORARY, // Fecha automaticamente após alguns segundos
    PERSISTENT // Permanece na tela até fechar ou aceitar/recusar
}

enum class OverlayLayoutMode {
    HORIZONTAL, // Card completo de 4 colunas (R$/km, R$/hora, Lucro, Lucro %)
    COMPACT     // Card vertical compacto
}

enum class FuelType(val displayName: String) {
    GASOLINA("Gasolina"),
    ETANOL("Etanol"),
    GNV("GNV")
}

data class FuelProfile(
    val fuelType: FuelType,
    val price: Double,
    val consumptionKmPerUnit: Double
) {
    val costPerKm: Double
        get() = if (consumptionKmPerUnit > 0) price / consumptionKmPerUnit else 0.0
}

data class VehicleConfig(
    val activeFuel: FuelType = FuelType.GASOLINA,
    val gasolineProfile: FuelProfile = FuelProfile(FuelType.GASOLINA, price = 5.89, consumptionKmPerUnit = 10.5),
    val ethanolProfile: FuelProfile = FuelProfile(FuelType.ETANOL, price = 3.99, consumptionKmPerUnit = 7.5),
    val gnvProfile: FuelProfile = FuelProfile(FuelType.GNV, price = 4.49, consumptionKmPerUnit = 13.0),
    val maintenancePerKm: Double = 0.20,
    val depreciationPerKm: Double = 0.15
) {
    val currentFuelProfile: FuelProfile
        get() = when (activeFuel) {
            FuelType.GASOLINA -> gasolineProfile
            FuelType.ETANOL -> ethanolProfile
            FuelType.GNV -> gnvProfile
        }

    val costPerKm: Double
        get() = currentFuelProfile.costPerKm + maintenancePerKm + depreciationPerKm
}

data class TrafficThresholds(
    val badRateKm: Double = 1.85,    // Abaixo disso é Ruim (Vermelho)
    val goodRateKm: Double = 2.35,   // Acima disso é Boa (Verde)
    val badRateHour: Double = 35.0,  // Abaixo disso é Ruim (Vermelho)
    val goodRateHour: Double = 45.0  // Acima disso é Boa (Verde)
)

data class MonthlyGoal(
    val grossTarget: Double = 10833.0,
    val costTarget: Double = 2879.0
) {
    val netTarget: Double
        get() = grossTarget - costTarget

    val profitPercentage: Double
        get() = if (grossTarget > 0) (netTarget / grossTarget) * 100.0 else 0.0
}

data class VoiceSettings(
    val enabled: Boolean = true,
    val readGreen: Boolean = true,
    val readYellow: Boolean = true,
    val readRed: Boolean = true,
    val speechRate: Float = 1.0f,
    val soundTone: String = "GigU" // GigU, Sino, Plim
)

data class OverlaySettings(
    val emojiStyle: EmojiStyle = EmojiStyle.CIRCLES,
    val durationMode: DurationMode = DurationMode.PERSISTENT,
    val layoutMode: OverlayLayoutMode = OverlayLayoutMode.HORIZONTAL,
    val autoScreenshot: Boolean = false
)

data class SafetySettings(
    val blacklistedNeighborhoods: List<String> = listOf("Heliópolis", "Paraisópolis", "Cracolândia", "Zona Perigosa"),
    val minPassengerRating: Double = 4.75,
    val secretCameraEnabled: Boolean = true
)

data class AutoRejectSettings(
    val enabled: Boolean = false,
    val delaySeconds: Int = 3,
    val rejectRedOnly: Boolean = true,
    val rejectRiskArea: Boolean = true
)

data class DriverTargets(
    val thresholds: TrafficThresholds = TrafficThresholds(),
    val monthlyGoal: MonthlyGoal = MonthlyGoal(),
    val emptyReturnPenaltyKm: Double = 0.0
)

data class RideOffer(
    val id: String,
    val platform: PlatformType,
    val fare: Double,
    val pickupDistanceKm: Double,
    val tripDistanceKm: Double,
    val pickupTimeMinutes: Int,
    val tripTimeMinutes: Int,
    val pickupAddress: String? = null,
    val destinationAddress: String? = null,
    val passengerRating: Double? = null,
    val surgeMultiplier: Double? = null,  // Ex: 1.4x (Dinâmico)
    val surgeAdditionalValue: Double? = null, // Ex: R$ 3.74
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalDistanceKm: Double
        get() = pickupDistanceKm + tripDistanceKm

    val totalTimeMinutes: Int
        get() = pickupTimeMinutes + tripTimeMinutes

    val formattedDuration: String
        get() {
            val hours = totalTimeMinutes / 60
            val minutes = totalTimeMinutes % 60
            return "${hours}h${minutes.toString().padStart(2, '0')}m"
        }
}

data class EvaluationResult(
    val offer: RideOffer,
    val totalCost: Double,
    val netProfit: Double,
    val profitPercentage: Double,    // Ex: 75%
    val ratePerKm: Double,
    val ratePerHour: Double,
    val color: TrafficLightColor,
    val isRiskArea: Boolean = false,
    val isLowRating: Boolean = false,
    val riskReason: String? = null,
    val verdictSummary: String
)

data class DailyStatistics(
    val totalOffers: Int = 0,
    val greenCount: Int = 0,
    val yellowCount: Int = 0,
    val redCount: Int = 0,
    val riskCount: Int = 0,
    val totalGrossFare: Double = 0.0,
    val totalNetProfit: Double = 0.0,
    val totalKmDriven: Double = 0.0,
    val totalTimeMinutes: Int = 0
) {
    val avgGrossPerKm: Double
        get() = if (totalKmDriven > 0) totalGrossFare / totalKmDriven else 0.0

    val avgCostPerKm: Double
        get() = if (totalKmDriven > 0) (totalGrossFare - totalNetProfit) / totalKmDriven else 0.0

    val avgGrossPerHour: Double
        get() = if (totalTimeMinutes > 0) (totalGrossFare / totalTimeMinutes) * 60.0 else 0.0

    val avgCostPerHour: Double
        get() = if (totalTimeMinutes > 0) ((totalGrossFare - totalNetProfit) / totalTimeMinutes) * 60.0 else 0.0
}
