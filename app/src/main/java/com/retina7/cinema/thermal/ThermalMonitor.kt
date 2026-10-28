package com.retina7.cinema.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThermalState(
    val status: Int = PowerManager.THERMAL_STATUS_NONE,
    val statusLabel: String = "NORMAL",
    val isThrottling: Boolean = false,
    val bitrateReductionFactor: Float = 1.0f // 0.8f when throttling (-20%)
)

class ThermalMonitor(context: Context) {

    private val TAG = "ThermalMonitor"
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _thermalState = MutableStateFlow(ThermalState())
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()

    private var listener: PowerManager.OnThermalStatusChangedListener? = null

    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            listener = PowerManager.OnThermalStatusChangedListener { status ->
                handleThermalStatus(status)
            }
            try {
                powerManager.addThermalStatusChangedListener(listener!!)
                // Initial check
                handleThermalStatus(powerManager.currentThermalStatus)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register thermal listener: ${e.message}")
            }
        }
    }

    private fun handleThermalStatus(status: Int) {
        val (label, isThrottled, reduction) = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> Triple("NORMAL", false, 1.0f)
            PowerManager.THERMAL_STATUS_LIGHT -> Triple("LIGHT", false, 1.0f)
            PowerManager.THERMAL_STATUS_MODERATE -> Triple("MODERATE", true, 0.8f) // -20% bitrate per Prompt.txt
            PowerManager.THERMAL_STATUS_SEVERE -> Triple("SEVERE", true, 0.7f)
            PowerManager.THERMAL_STATUS_CRITICAL -> Triple("CRITICAL", true, 0.6f)
            PowerManager.THERMAL_STATUS_EMERGENCY -> Triple("EMERGENCY", true, 0.5f)
            PowerManager.THERMAL_STATUS_SHUTDOWN -> Triple("SHUTDOWN", true, 0.4f)
            else -> Triple("UNKNOWN", false, 1.0f)
        }

        Log.w(TAG, "Thermal Status changed: $label (Throttling: $isThrottled, Bitrate Factor: $reduction)")
        _thermalState.value = ThermalState(
            status = status,
            statusLabel = label,
            isThrottling = isThrottled,
            bitrateReductionFactor = reduction
        )
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && listener != null) {
            try {
                powerManager.removeThermalStatusChangedListener(listener!!)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister thermal listener: ${e.message}")
            }
        }
    }
}
