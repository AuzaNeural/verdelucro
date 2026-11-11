package com.auza.giguclone.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.auza.giguclone.data.db.RideDatabaseHelper
import com.auza.giguclone.domain.model.*
import com.auza.giguclone.service.FloatingOverlayService
import com.auza.giguclone.service.RideAccessibilityService
import com.auza.giguclone.ui.theme.GigUCloneTheme

class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: RideDatabaseHelper

    private var vehicleConfig by mutableStateOf(VehicleConfig())
    private var driverTargets by mutableStateOf(DriverTargets())
    private var safetySettings by mutableStateOf(SafetySettings())
    private var autoRejectSettings by mutableStateOf(AutoRejectSettings())
    private var voiceSettings by mutableStateOf(VoiceSettings())
    private var overlaySettings by mutableStateOf(OverlaySettings())

    private var dailyStats by mutableStateOf(DailyStatistics())
    private var recentHistory by mutableStateOf<List<EvaluationResult>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = RideDatabaseHelper(this)
        loadData()

        setContent {
            GigUCloneTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                var isOverlayGranted by remember { mutableStateOf(checkOverlayPermission()) }
                var isAccessibilityGranted by remember { mutableStateOf(checkAccessibilityPermission()) }

                when (currentScreen) {
                    "dashboard" -> {
                        DashboardScreen(
                            isOverlayPermissionGranted = isOverlayGranted,
                            isAccessibilityEnabled = isAccessibilityGranted,
                            onRequestOverlayPermission = { requestOverlayPermission() },
                            onRequestAccessibilityPermission = { requestAccessibilityPermission() },
                            vehicleConfig = vehicleConfig,
                            driverTargets = driverTargets,
                            safetySettings = safetySettings,
                            dailyStats = dailyStats,
                            onSelectFuel = { fuel ->
                                vehicleConfig = vehicleConfig.copy(activeFuel = fuel)
                                syncServiceSettings()
                            },
                            onSimulateOverlay = { evaluation ->
                                dbHelper.insertEvaluation(evaluation)
                                loadData()
                                FloatingOverlayService.overlaySettings = overlaySettings
                                FloatingOverlayService.voiceSettings = voiceSettings
                                FloatingOverlayService.show(this@MainActivity, evaluation)
                            },
                            onNavigateToSettings = { currentScreen = "settings" },
                            onNavigateToHistory = {
                                loadData()
                                currentScreen = "history"
                            }
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            currentConfig = vehicleConfig,
                            currentTargets = driverTargets,
                            currentSafety = safetySettings,
                            currentAutoReject = autoRejectSettings,
                            currentVoice = voiceSettings,
                            currentOverlay = overlaySettings,
                            onSave = { newConfig, newTargets, newSafety, newAutoReject, newVoice, newOverlay ->
                                vehicleConfig = newConfig
                                driverTargets = newTargets
                                safetySettings = newSafety
                                autoRejectSettings = newAutoReject
                                voiceSettings = newVoice
                                overlaySettings = newOverlay
                                syncServiceSettings()
                            },
                            onBack = { currentScreen = "dashboard" }
                        )
                    }
                    "history" -> {
                        HistoryScreen(
                            rides = recentHistory,
                            onBack = { currentScreen = "dashboard" }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        dailyStats = dbHelper.getTodayStatistics()
        recentHistory = dbHelper.getRecentHistory(40)
        syncServiceSettings()
    }

    private fun syncServiceSettings() {
        RideAccessibilityService.vehicleConfig = vehicleConfig
        RideAccessibilityService.driverTargets = driverTargets
        RideAccessibilityService.safetySettings = safetySettings
        RideAccessibilityService.autoRejectSettings = autoRejectSettings
        RideAccessibilityService.voiceSettings = voiceSettings
        RideAccessibilityService.overlaySettings = overlaySettings

        FloatingOverlayService.overlaySettings = overlaySettings
        FloatingOverlayService.voiceSettings = voiceSettings
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun checkAccessibilityPermission(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}
