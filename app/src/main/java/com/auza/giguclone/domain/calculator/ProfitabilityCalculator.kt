package com.auza.giguclone.domain.calculator

import com.auza.giguclone.domain.model.*
import java.util.Locale

object ProfitabilityCalculator {

    /**
     * Avalia a rentabilidade e os riscos de uma oferta com base
     * nos critérios da régua dupla do semáforo (Ruim / Médio / Bom) e custos reais.
     */
    fun evaluate(
        offer: RideOffer,
        vehicle: VehicleConfig,
        targets: DriverTargets,
        safety: SafetySettings
    ): EvaluationResult {
        // 1. Verificação de Segurança (Área de Risco no Destino ou Embarque)
        val (isRiskArea, riskAreaMatched) = checkRiskArea(offer, safety.blacklistedNeighborhoods)

        // 2. Verificação de Nota do Passageiro
        val isLowRating = offer.passengerRating != null &&
                offer.passengerRating > 0.0 &&
                offer.passengerRating < safety.minPassengerRating

        // 3. Distância considerando penalidade de volta vazia em viagens longas
        var effectiveDistance = if (offer.totalDistanceKm > 0.0) offer.totalDistanceKm else 1.0
        if (offer.tripDistanceKm > 18.0 && targets.emptyReturnPenaltyKm > 0.0) {
            effectiveDistance += targets.emptyReturnPenaltyKm
        }

        val totalTimeMinutes = if (offer.totalTimeMinutes > 0) offer.totalTimeMinutes else 1

        // 4. Custos operacionais e Lucro Líquido Real
        val totalCost = effectiveDistance * vehicle.costPerKm
        val netProfit = offer.fare - totalCost
        val profitPercentage = if (offer.fare > 0.0) (netProfit / offer.fare) * 100.0 else 0.0

        // 5. Taxas por km e por hora
        val ratePerKm = offer.fare / effectiveDistance
        val ratePerHour = (offer.fare / totalTimeMinutes) * 60.0

        // 6. Decisão do Semáforo usando a régua de 2 marcadores (Ruim < Amarelo < Bom)
        val color: TrafficLightColor
        val verdict: String

        if (isRiskArea) {
            color = TrafficLightColor.PURPLE_RISK
            verdict = "⚠️ ALERTA DE SEGURANÇA: Destino em '$riskAreaMatched' cadastrado como área de risco!"
        } else if (isLowRating) {
            color = TrafficLightColor.PURPLE_RISK
            verdict = "⚠️ ALERTA: Passageiro com nota baixa (${offer.passengerRating}★) abaixo da meta (${safety.minPassengerRating}★)!"
        } else {
            color = determineColor(ratePerKm, ratePerHour, netProfit, targets.thresholds)
            verdict = when (color) {
                TrafficLightColor.GREEN -> "🟢 Boa! R$ ${format(ratePerKm)}/km | R$ ${format(ratePerHour)}/hr | Lucro: R$ ${format(netProfit)} (${profitPercentage.toInt()}%)."
                TrafficLightColor.YELLOW -> "🟡 Média. R$ ${format(ratePerKm)}/km | R$ ${format(ratePerHour)}/hr. Avalie o destino."
                TrafficLightColor.RED -> "🔴 Ruim! R$ ${format(ratePerKm)}/km. Prejuízo ou margem baixa (Lucro: R$ ${format(netProfit)})."
                TrafficLightColor.PURPLE_RISK -> "⚠️ Alerta de Risco!"
            }
        }

        return EvaluationResult(
            offer = offer,
            totalCost = totalCost,
            netProfit = netProfit,
            profitPercentage = profitPercentage,
            ratePerKm = ratePerKm,
            ratePerHour = ratePerHour,
            color = color,
            isRiskArea = isRiskArea,
            isLowRating = isLowRating,
            riskReason = if (isRiskArea) "Área de risco: $riskAreaMatched" else if (isLowRating) "Nota baixa: ${offer.passengerRating}★" else null,
            verdictSummary = verdict
        )
    }

    private fun checkRiskArea(offer: RideOffer, blacklisted: List<String>): Pair<Boolean, String?> {
        val dest = offer.destinationAddress?.lowercase() ?: ""
        val pickup = offer.pickupAddress?.lowercase() ?: ""

        for (banned in blacklisted) {
            val term = banned.trim().lowercase()
            if (term.isNotBlank() && (dest.contains(term) || pickup.contains(term))) {
                return true to banned
            }
        }
        return false to null
    }

    private fun determineColor(
        ratePerKm: Double,
        ratePerHour: Double,
        netProfit: Double,
        thresholds: TrafficThresholds
    ): TrafficLightColor {
        // Se a corrida der prejuízo absoluto ou estiver abaixo dos limites ruins:
        if (netProfit <= 0 || ratePerKm < thresholds.badRateKm || ratePerHour < thresholds.badRateHour) {
            return TrafficLightColor.RED
        }

        // Se superar os dois limites da faixa verde (Boa):
        if (ratePerKm >= thresholds.goodRateKm && ratePerHour >= thresholds.goodRateHour) {
            return TrafficLightColor.GREEN
        }

        // Faixa intermediária:
        return TrafficLightColor.YELLOW
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)
}
