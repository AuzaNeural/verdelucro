package com.auza.giguclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auza.giguclone.domain.model.EvaluationResult
import com.auza.giguclone.domain.model.TrafficLightColor
import com.auza.giguclone.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    rides: List<EvaluationResult>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Ofertas", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        if (rides.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma oferta registrada ainda.\nDeixe o Uber, 99 ou inDrive aberto para registrar.",
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(rides) { result ->
                    GigUStyleHistoryCard(result)
                }
            }
        }
    }
}

/**
 * Card de Histórico com design fiel ao GigU oficial (Imagem 5 dos screenshots)
 */
@Composable
fun GigUStyleHistoryCard(result: EvaluationResult) {
    val barColor = when (result.color) {
        TrafficLightColor.GREEN -> Color(0xFF00C853)
        TrafficLightColor.YELLOW -> Color(0xFFFFD600)
        TrafficLightColor.RED -> Color(0xFFD50000)
        TrafficLightColor.PURPLE_RISK -> Color(0xFF9C27B0)
    }

    val dateFormat = SimpleDateFormat("d 'de' MMM, yyyy", Locale("pt", "BR")).format(Date(result.offer.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Linha Topo: Badge da Plataforma + Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (result.offer.platform.displayName == "99") Color(0xFFFFEB3B) else Color.Black,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = result.offer.platform.displayName,
                            color = if (result.offer.platform.displayName == "99") Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = dateFormat,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Colunas R$/km e R$/hr
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("R$/km", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = String.format(Locale.US, "%.2f", result.ratePerKm).replace(".", ","),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("R$/hr", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = String.format(Locale.US, "%.2f", result.ratePerHour).replace(".", ","),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Valor da corrida
            Text(
                text = String.format(Locale.US, "R$ %.2f", result.offer.fare).replace(".", ","),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Barra colorida do Semáforo (Verde / Amarela / Vermelha / Roxa)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(barColor, RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Distância e Tempo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "— ${String.format(Locale.US, "%.1f km", result.offer.totalDistanceKm).replace(".", ",")}   — ${result.offer.totalTimeMinutes} min",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Líquido: R$ ${String.format(Locale.US, "%.2f", result.netProfit)} (${result.profitPercentage.toInt()}%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (result.netProfit > 0) Color(0xFF2E7D32) else Color(0xFFD50000)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Endereços de Origem e Destino com ícones
            val pickup = if (!result.offer.pickupAddress.isNullOrBlank()) result.offer.pickupAddress else "Ponto de Embarque"
            val dest = if (!result.offer.destinationAddress.isNullOrBlank()) result.offer.destinationAddress else "Ponto de Desembarque"

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = pickup, fontSize = 11.sp, color = Color(0xFF616161), maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏁", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = dest, fontSize = 11.sp, color = Color(0xFF212121), fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
