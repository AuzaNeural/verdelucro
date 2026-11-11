package com.auza.giguclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auza.giguclone.domain.model.*
import com.auza.giguclone.ui.theme.BackgroundDark
import com.auza.giguclone.ui.theme.CardBackground
import com.auza.giguclone.ui.theme.PrimaryDark
import com.auza.giguclone.ui.theme.SurfaceDark

@Composable
fun SettingsScreen(
    currentConfig: VehicleConfig,
    currentTargets: DriverTargets,
    currentSafety: SafetySettings,
    currentAutoReject: AutoRejectSettings,
    currentVoice: VoiceSettings,
    currentOverlay: OverlaySettings,
    onSave: (VehicleConfig, DriverTargets, SafetySettings, AutoRejectSettings, VoiceSettings, OverlaySettings) -> Unit,
    onBack: () -> Unit
) {
    // Régua do Semáforo (Ruim vs Bom)
    var badKm by remember { mutableFloatStateOf(currentTargets.thresholds.badRateKm.toFloat()) }
    var goodKm by remember { mutableFloatStateOf(currentTargets.thresholds.goodRateKm.toFloat()) }
    var badHour by remember { mutableFloatStateOf(currentTargets.thresholds.badRateHour.toFloat()) }
    var goodHour by remember { mutableFloatStateOf(currentTargets.thresholds.goodRateHour.toFloat()) }

    // Simulação de Ganhos Maiores (+0% a +50%)
    var simulateBoostPercent by remember { mutableFloatStateOf(0f) }

    // Estilo dos emojis e duração
    var emojiStyle by remember { mutableStateOf(currentOverlay.emojiStyle) }
    var durationMode by remember { mutableStateOf(currentOverlay.durationMode) }
    var autoScreenshot by remember { mutableStateOf(currentOverlay.autoScreenshot) }

    // Configurações de Voz (Filtros por categoria, velocidade e som)
    var voiceEnabled by remember { mutableStateOf(currentVoice.enabled) }
    var readGreen by remember { mutableStateOf(currentVoice.readGreen) }
    var readYellow by remember { mutableStateOf(currentVoice.readYellow) }
    var readRed by remember { mutableStateOf(currentVoice.readRed) }
    var speechRate by remember { mutableFloatStateOf(currentVoice.speechRate) }
    var soundTone by remember { mutableStateOf(currentVoice.soundTone) }

    // Combustíveis
    var gasPrice by remember { mutableStateOf(currentConfig.gasolineProfile.price.toString()) }
    var gasKml by remember { mutableStateOf(currentConfig.gasolineProfile.consumptionKmPerUnit.toString()) }
    var ethanolPrice by remember { mutableStateOf(currentConfig.ethanolProfile.price.toString()) }
    var ethanolKml by remember { mutableStateOf(currentConfig.ethanolProfile.consumptionKmPerUnit.toString()) }
    var gnvPrice by remember { mutableStateOf(currentConfig.gnvProfile.price.toString()) }
    var gnvKml by remember { mutableStateOf(currentConfig.gnvProfile.consumptionKmPerUnit.toString()) }

    var maintenance by remember { mutableStateOf(currentConfig.maintenancePerKm.toString()) }
    var depreciation by remember { mutableStateOf(currentConfig.depreciationPerKm.toString()) }

    // Segurança & Lista Negra
    var newNeighborhood by remember { mutableStateOf("") }
    var neighborhoods by remember { mutableStateOf(currentSafety.blacklistedNeighborhoods) }
    var minRating by remember { mutableStateOf(currentSafety.minPassengerRating.toString()) }

    // Auto-Rejeição
    var autoRejectEnabled by remember { mutableStateOf(currentAutoReject.enabled) }
    var autoRejectDelay by remember { mutableStateOf(currentAutoReject.delaySeconds.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cálculo de Ganhos & Ajustes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark
            )
            TextButton(onClick = onBack) {
                Text("Voltar", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SEÇÃO 1: RÉGUA DE CRITÉRIOS DO SEMÁFORO (Ruim < Amarelo < Bom) - Imagem 4
        Text("🚦 Critérios do Semáforo (Régua Dupla)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Ganho por Km
                Text("Ganho por Km:", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("👎 Ruim: < R$ ${String.format("%.2f", badKm)}", fontSize = 11.sp, color = Color(0xFFFF5252))
                    Text("👍 Bom: > R$ ${String.format("%.2f", goodKm)}", fontSize = 11.sp, color = PrimaryDark)
                }
                Spacer(modifier = Modifier.height(4.dp))
                RangeSlider(
                    value = badKm..goodKm,
                    onValueChange = { range ->
                        badKm = range.start
                        goodKm = range.endInclusive
                    },
                    valueRange = 1.0f..4.0f,
                    steps = 30,
                    colors = SliderDefaults.colors(thumbColor = PrimaryDark, activeTrackColor = PrimaryDark)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Ganho por Hora
                Text("Ganho por Hora:", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("👎 Ruim: < R$ ${badHour.toInt()}/hr", fontSize = 11.sp, color = Color(0xFFFF5252))
                    Text("👍 Bom: > R$ ${goodHour.toInt()}/hr", fontSize = 11.sp, color = PrimaryDark)
                }
                Spacer(modifier = Modifier.height(4.dp))
                RangeSlider(
                    value = badHour..goodHour,
                    onValueChange = { range ->
                        badHour = range.start
                        goodHour = range.endInclusive
                    },
                    valueRange = 20.0f..80.0f,
                    steps = 60,
                    colors = SliderDefaults.colors(thumbColor = PrimaryDark, activeTrackColor = PrimaryDark)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Simulador de Ganhos Maiores (Imagem 3)
                Text(
                    text = "Simule Ganhos Maiores: +${simulateBoostPercent.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFAB40)
                )
                Slider(
                    value = simulateBoostPercent,
                    onValueChange = { simulateBoostPercent = it },
                    valueRange = 0f..50f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFAB40), activeTrackColor = Color(0xFFFFAB40))
                )

                Button(
                    onClick = {
                        val factor = 1.0f + (simulateBoostPercent / 100f)
                        badKm *= factor
                        goodKm *= factor
                        badHour *= factor
                        goodHour *= factor
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Aplicar $/Km e $/Hr no Semáforo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SEÇÃO 2: AJUSTES VISUAIS DO SEMÁFORO (Imagem 6)
        Text("🎨 Estilo do Semáforo & Duração", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Estilo dos emojis:", color = Color.White, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = emojiStyle == EmojiStyle.CIRCLES,
                            onClick = { emojiStyle = EmojiStyle.CIRCLES },
                            label = { Text("🟡 🟢 🔴") }
                        )
                        FilterChip(
                            selected = emojiStyle == EmojiStyle.ICONS,
                            onClick = { emojiStyle = EmojiStyle.ICONS },
                            label = { Text("✅ ⚠️ ❌") }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Duração da janela:", color = Color.White, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = durationMode == DurationMode.TEMPORARY,
                            onClick = { durationMode = DurationMode.TEMPORARY },
                            label = { Text("Temporário") }
                        )
                        FilterChip(
                            selected = durationMode == DurationMode.PERSISTENT,
                            onClick = { durationMode = DurationMode.PERSISTENT },
                            label = { Text("Persistente") }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Captura de Tela Automática", color = Color.White, fontSize = 13.sp)
                        Text("Tira print de cada oferta para auditoria", color = Color.Gray, fontSize = 10.sp)
                    }
                    Switch(checked = autoScreenshot, onCheckedChange = { autoScreenshot = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SEÇÃO 3: RESUMO POR VOZ AVANÇADO (Imagem 6)
        Text("🔊 Resumo por Voz Personalizado", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Leitura em voz alta", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Switch(checked = voiceEnabled, onCheckedChange = { voiceEnabled = it })
                }

                if (voiceEnabled) {
                    Text("Selecione quais corridas ler em voz alta:", fontSize = 12.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = readGreen,
                            onClick = { readGreen = !readGreen },
                            label = { Text("🟢 Boa") }
                        )
                        FilterChip(
                            selected = readYellow,
                            onClick = { readYellow = !readYellow },
                            label = { Text("🟡 Média") }
                        )
                        FilterChip(
                            selected = readRed,
                            onClick = { readRed = !readRed },
                            label = { Text("🔴 Ruim") }
                        )
                    }

                    Text("Velocidade da voz: ${String.format("%.1f", speechRate)}x", fontSize = 12.sp, color = Color.White)
                    Slider(
                        value = speechRate,
                        onValueChange = { speechRate = it },
                        valueRange = 0.8f..2.0f,
                        steps = 6
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Som de aviso:", fontSize = 12.sp, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("GigU", "Sino", "Plim").forEach { tone ->
                                FilterChip(
                                    selected = soundTone == tone,
                                    onClick = { soundTone = tone },
                                    label = { Text(tone) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SEÇÃO 4: AUTO-REJEIÇÃO DE VERMELHAS & SEGURANÇA
        Text("🚨 Segurança & Auto-Rejeição", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Auto-Recusar Chamadas Vermelhas", fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Recusa chamadas com prejuízo sem tocar na tela", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = autoRejectEnabled, onCheckedChange = { autoRejectEnabled = it })
                }

                Text("Lista Negra de Áreas de Risco:", fontSize = 12.sp, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newNeighborhood,
                        onValueChange = { newNeighborhood = it },
                        label = { Text("Adicionar Bairro / Local") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    IconButton(
                        onClick = {
                            if (newNeighborhood.isNotBlank() && !neighborhoods.contains(newNeighborhood.trim())) {
                                neighborhoods = neighborhoods + newNeighborhood.trim()
                                newNeighborhood = ""
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .background(PrimaryDark, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.Black)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(neighborhoods) { item ->
                        InputChip(
                            selected = true,
                            onClick = { neighborhoods = neighborhoods - item },
                            label = { Text(item, fontSize = 11.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(14.dp)) },
                            colors = InputChipDefaults.inputChipColors(containerColor = CardBackground, labelColor = Color.White)
                        )
                    }
                }

                OutlinedTextField(
                    value = minRating,
                    onValueChange = { minRating = it },
                    label = { Text("Nota Mínima do Passageiro (★)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SEÇÃO 5: CUSTOS DE COMBUSTÍVEL
        Text("⛽ Perfis de Combustível (Gasolina, Etanol, GNV)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gasolina:", fontWeight = FontWeight.SemiBold, color = PrimaryDark, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gasPrice,
                        onValueChange = { gasPrice = it },
                        label = { Text("Preço R$/L") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = gasKml,
                        onValueChange = { gasKml = it },
                        label = { Text("Consumo Km/L") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Text("Etanol:", fontWeight = FontWeight.SemiBold, color = PrimaryDark, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ethanolPrice,
                        onValueChange = { ethanolPrice = it },
                        label = { Text("Preço R$/L") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = ethanolKml,
                        onValueChange = { ethanolKml = it },
                        label = { Text("Consumo Km/L") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Text("GNV:", fontWeight = FontWeight.SemiBold, color = PrimaryDark, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gnvPrice,
                        onValueChange = { gnvPrice = it },
                        label = { Text("Preço R$/m³") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = gnvKml,
                        onValueChange = { gnvKml = it },
                        label = { Text("Consumo Km/m³") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val updatedConfig = currentConfig.copy(
                    gasolineProfile = FuelProfile(FuelType.GASOLINA, gasPrice.toDoubleOrNull() ?: 5.89, gasKml.toDoubleOrNull() ?: 10.5),
                    ethanolProfile = FuelProfile(FuelType.ETANOL, ethanolPrice.toDoubleOrNull() ?: 3.99, ethanolKml.toDoubleOrNull() ?: 7.5),
                    gnvProfile = FuelProfile(FuelType.GNV, gnvPrice.toDoubleOrNull() ?: 4.49, gnvKml.toDoubleOrNull() ?: 13.0),
                    maintenancePerKm = maintenance.toDoubleOrNull() ?: 0.20,
                    depreciationPerKm = depreciation.toDoubleOrNull() ?: 0.15
                )

                val updatedTargets = currentTargets.copy(
                    thresholds = TrafficThresholds(
                        badRateKm = badKm.toDouble(),
                        goodRateKm = goodKm.toDouble(),
                        badRateHour = badHour.toDouble(),
                        goodRateHour = goodHour.toDouble()
                    )
                )

                val updatedSafety = SafetySettings(
                    blacklistedNeighborhoods = neighborhoods,
                    minPassengerRating = minRating.toDoubleOrNull() ?: 4.75
                )

                val updatedAutoReject = AutoRejectSettings(
                    enabled = autoRejectEnabled,
                    delaySeconds = autoRejectDelay.toIntOrNull() ?: 3
                )

                val updatedVoice = VoiceSettings(
                    enabled = voiceEnabled,
                    readGreen = readGreen,
                    readYellow = readYellow,
                    readRed = readRed,
                    speechRate = speechRate,
                    soundTone = soundTone
                )

                val updatedOverlay = OverlaySettings(
                    emojiStyle = emojiStyle,
                    durationMode = durationMode,
                    autoScreenshot = autoScreenshot
                )

                onSave(updatedConfig, updatedTargets, updatedSafety, updatedAutoReject, updatedVoice, updatedOverlay)
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Salvar Todas as Configurações", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
