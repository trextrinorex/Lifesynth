package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.health.HealthConnectHelper
import com.example.data.local.DailyRhythmEntity
import com.example.data.repository.LifeRhythmRepository
import com.example.data.usage.UsageAccessHelper
import com.example.model.DailyRhythmSummary
import com.example.model.DebugDataInfo
import com.example.model.YouVsYouMetric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LifeRhythmUiState(
    val isLoading: Boolean = true,
    val summary: DailyRhythmSummary? = null,
    val hasUsageAccess: Boolean = false,
    val youVsYouMetrics: List<YouVsYouMetric> = emptyList(),
    val observedPatterns: List<String> = emptyList(),
    val history: List<DailyRhythmEntity> = emptyList(),
    val isHealthConnectEnabled: Boolean = false,
    val isEveningDigestEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val nightWindowStartHour: Int = 21,
    val nightWindowEndHour: Int = 11,
    val debugInfo: DebugDataInfo? = null,
    val userNotice: String? = null
)

class LifeRhythmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LifeRhythmRepository(application)
    private val _uiState = MutableStateFlow(LifeRhythmUiState())
    val uiState: StateFlow<LifeRhythmUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        observeHistory()
    }

    private fun loadInitialState() {
        val context = getApplication<Application>()
        val hasPermission = UsageAccessHelper.hasUsageAccess(context)
        val prefs = repository.preferences

        _uiState.update {
            it.copy(
                hasUsageAccess = hasPermission,
                isHealthConnectEnabled = prefs.isHealthConnectEnabled,
                isEveningDigestEnabled = prefs.isEveningDigestEnabled,
                isOnboardingCompleted = prefs.isOnboardingCompleted,
                nightWindowStartHour = prefs.nightWindowStartHour,
                nightWindowEndHour = prefs.nightWindowEndHour
            )
        }

        refreshData()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.historicalRhythms.collect { entities ->
                _uiState.update { it.copy(history = entities) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val context = getApplication<Application>()
            val hasPerm = UsageAccessHelper.hasUsageAccess(context)
            val summary = repository.getTodaySummary()
            val youVsYou = repository.getYouVsYouMetrics(summary)
            val patterns = repository.getObservedPatterns()
            val debug = repository.getDebugInfo(summary)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasUsageAccess = hasPerm,
                    summary = summary,
                    youVsYouMetrics = youVsYou,
                    observedPatterns = patterns,
                    debugInfo = debug
                )
            }
        }
    }

    fun checkPermissions() {
        val hasPerm = UsageAccessHelper.hasUsageAccess(getApplication())
        _uiState.update { it.copy(hasUsageAccess = hasPerm) }
        refreshData()
    }

    fun completeOnboarding() {
        repository.preferences.isOnboardingCompleted = true
        _uiState.update { it.copy(isOnboardingCompleted = true) }
        refreshData()
    }

    fun setHealthConnectEnabled(enabled: Boolean) {
        repository.preferences.isHealthConnectEnabled = enabled
        _uiState.update { it.copy(isHealthConnectEnabled = enabled) }
        if (enabled) {
            HealthConnectHelper.openHealthConnect(getApplication())
        }
        refreshData()
    }

    fun openHealthConnect() {
        HealthConnectHelper.openHealthConnect(getApplication())
    }

    fun setEveningDigestEnabled(enabled: Boolean) {
        repository.preferences.isEveningDigestEnabled = enabled
        _uiState.update { it.copy(isEveningDigestEnabled = enabled) }
    }

    fun updateNightWindow(startHour: Int, endHour: Int) {
        repository.preferences.nightWindowStartHour = startHour
        repository.preferences.nightWindowEndHour = endHour
        _uiState.update {
            it.copy(
                nightWindowStartHour = startHour,
                nightWindowEndHour = endHour
            )
        }
        refreshData()
    }

    fun openUsageSettings() {
        UsageAccessHelper.openUsageAccessSettings(getApplication())
    }

    suspend fun exportJson(): String = repository.exportDataAsJson()

    suspend fun exportCsv(): String = repository.exportDataAsCsv()

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllData()
            refreshData()
            onComplete()
        }
    }

    fun clearUserNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun showNotice(msg: String) {
        _uiState.update { it.copy(userNotice = msg) }
    }
}
