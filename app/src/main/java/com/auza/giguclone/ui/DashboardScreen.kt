package com.auza.giguclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auza.giguclone.domain.calculator.ProfitabilityCalculator
import com.auza.giguclone.domain.model.*
import com.auza.giguclone.ui.theme.*

@Composable
fun DashboardScreen(
    isOverlayPermissionGranted: Boolean,
    isAccessibilityEnabled: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    vehicleConfig: VehicleConfig,
    driverTargets: DriverTargets,
    safetySettings: SafetySettings,
    dailyStats: DailyStatistics,
    onSelectFuel: (FuelType) -> Unit,
    onSimulateOverlay: (EvaluationResult) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var simFare by remember { mutableStateOf("32.50") }
    var simTripKm by remember { mutableStateOf("6.2") }
    var simPickupKm by remember { mutableStateOf("1.2") }
    var simTime by remember { mutableStateOf("16") }
    var simDestAddress by remember { mutableStateOf("Av. Paulista, 1000") }
    var simRating by remember { mutableStateOf("4.88") }
    var evaluationResult by remember { mutableStateOf<EvaluationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Topo: Cabeçalho com ações
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GigU Copiloto Pro",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "Proteção e Inteligência de Corridas",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNavigateToHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("📊 Histórico", fontSize = 12.sp, color = Color.White)
                }

                Button(
                    onClick = onNavigateToSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("⚙️ Metas", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Seletor Rápido de Combustível em 1 Toque
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Combustível Ativo no Tanque:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Custo: R$ ${String.format("%.2f", vehicleConfig.costPerKm)}/km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SemafaroYellow
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FuelOptionButton(
                        title = "⛽ Gasolina",
                        subtitle = "R$ ${String.format("%.2f", vehicleConfig.gasolineProfile.price)}",
                        isSelected = vehicleConfig.activeFuel == FuelType.GASOLINA,
                        onClick = { onSelectFuel(FuelType.GASOLINA) },
                        modifier = Modifier.weight(1f)
                    )
                    FuelOptionButton(
                        title = "🌱 Etanol",
                        subtitle = "R$ ${String.format("%.2f", vehicleConfig.ethanolProfile.price)}",
                        isSelected = vehicleConfig.activeFuel == FuelType.ETANOL,
                        onClick = { onSelectFuel(FuelType.ETANOL) },
                        modifier = Modifier.weight(1f)
                    )
                    FuelOptionButton(
                        title = "⚡ GNV",
                        subtitle = "R$ ${String.format("%.2f", vehicleConfig.gnvProfile.price)}",
                        isSelected = vehicleConfig.activeFuel == FuelType.GNV,
                        onClick = { onSelectFuel(FuelType.GNV) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card de Resumo Diário
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Lucro Líquido Real Hoje", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "R$ ${String.format("%.2f", dailyStats.totalNetProfit)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Faturamento Bruto", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "R$ ${String.format("%.2f", dailyStats.totalGrossFare)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Corridas: ${dailyStats.totalOffers} (🟢 ${dailyStats.greenCount} | 🟡 ${dailyStats.yellowCount} | 🔴 ${dailyStats.redCount})",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "${String.format("%.1f", dailyStats.totalKmDriven)} km rodados",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card de Permissões
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Status dos Serviços do Android",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                PermissionRow(
                    title = "Janela Flutuante (SYSTEM_ALERT_WINDOW)",
                    isGranted = isOverlayPermissionGranted,
                    onRequest = onRequestOverlayPermission
                )
                Spacer(modifier = Modifier.height(6.dp))
                PermissionRow(
                    title = "Acessibilidade (Uber, 99 e inDrive)",
                    isGranted = isAccessibilityEnabled,
                    onRequest = onRequestAccessibilityPermission
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Simulador de Chamada com Risco e Nota
        Text(
            text = "Simulador Completo de Ofertas",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Teste a reação do Semáforo, detecção de áreas de risco e nota de passageiro.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = simFare,
                onValueChange = { simFare = it },
                label = { Text("Valor (R$)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = simTripKm,
                onValueChange = { simTripKm = it },
                label = { Text("Viagem (km)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = simTime,
                onValueChange = { simTime = it },
                label = { Text("Tempo (min)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = simDestAddress,
                onValueChange = { simDestAddress = it },
                label = { Text("Endereço / Bairro de Destino") },
                modifier = Modifier.weight(2f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = simRating,
                onValueChange = { simRating = it },
                label = { Text("Nota ★") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = { simDestAddress = "Paraisópolis, São Paulo" },
                label = { Text("Simular Área de Risco", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { simRating = "4.62" },
                label = { Text("Simular Nota Baixa", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = {
                    simFare = "48.00"
                    simTripKm = "8.0"
                    simDestAddress = "Jardins, São Paulo"
                    simRating = "4.95"
                },
                label = { Text("Corrida Top", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val fare = simFare.toDoubleOrNull() ?: 25.0
                val trip = simTripKm.toDoubleOrNull() ?: 5.0
                val pickup = simPickupKm.toDoubleOrNull() ?: 1.0
                val time = simTime.toIntOrNull() ?: 15
                val rating = simRating.toDoubleOrNull() ?: 4.85

                val offer = RideOffer(
                    id = "SIM-${System.currentTimeMillis()}",
                    platform = PlatformType.UBER,
                    fare = fare,
                    pickupDistanceKm = pickup,
                    tripDistanceKm = trip,
                    pickupTimeMinutes = 3,
                    tripTimeMinutes = time,
                    destinationAddress = simDestAddress,
                    passengerRating = rating
                )

                val result = ProfitabilityCalculator.evaluate(
                    offer = offer,
                    vehicle = vehicleConfig,
                    targets = driverTargets,
                    safety = safetySettings
                )
                evaluationResult = result
                onSimulateOverlay(result)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                "Avaliar Rentabilidade & Abrir Semáforo Flutuante",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        evaluationResult?.let { result ->
            Spacer(modifier = Modifier.height(14.dp))

            val bannerColor = when (result.color) {
                TrafficLightColor.GREEN -> SemafaroGreen
                TrafficLightColor.YELLOW -> SemafaroYellow
                TrafficLightColor.RED -> SemafaroRed
                TrafficLightColor.PURPLE_RISK -> SemafaroPurple
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bannerColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = result.verdictSummary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Ganho / km: R$ ${String.format("%.2f", result.ratePerKm)}/km | Ganho / hora: R$ ${String.format("%.0f", result.ratePerHour)}/h",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "• Lucro Líquido Real: R$ ${String.format("%.2f", result.netProfit)} (Descontado custo de R$ ${String.format("%.2f", result.totalCost)})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FuelOptionButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryDark else CardBackground
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = subtitle,
                color = if (isSelected) Color.Black else Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
