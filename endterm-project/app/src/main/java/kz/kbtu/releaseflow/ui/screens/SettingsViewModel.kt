package kz.kbtu.releaseflow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.releaseflow.data.repository.ReleaseRepository
import kz.kbtu.releaseflow.data.repository.Settings
import kz.kbtu.releaseflow.data.repository.SettingsRepository
import kz.kbtu.releaseflow.data.work.StoreSyncWorker
import kz.kbtu.releaseflow.data.work.SyncScheduler
import kz.kbtu.releaseflow.ui.UiMessage

data class SettingsUiState(
    val settings: Settings = Settings(),
    val manualState: WorkInfo.State? = null,        // the "Sync now" request
    val manualResult: String? = null,               // its output data (summary) when finished
    val periodicState: WorkInfo.State? = null,      // the scheduled request
    val periodicNextRunAt: Long? = null,
    val resetting: Boolean = false,
    val message: UiMessage? = null,
) {
    val isSyncing: Boolean
        get() = manualState == WorkInfo.State.RUNNING || manualState == WorkInfo.State.ENQUEUED
}

/**
 * Settings = DataStore (what the user chose) + WorkManager (what the system is doing with
 * it). Both are Flows; `combine` merges them into one state and `stateIn` turns that into
 * a `StateFlow` that is only collected while a screen observes it.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    private val releaseRepository: ReleaseRepository,
) : ViewModel() {

    private val local = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        syncScheduler.manualStatus,
        syncScheduler.periodicStatus,
        local,
    ) { settings, manual, periodic, localState ->
        localState.copy(
            settings = settings,
            manualState = manual?.state,
            manualResult = manual?.outputData?.getString(StoreSyncWorker.KEY_SUMMARY),
            periodicState = periodic?.state,
            periodicNextRunAt = periodic?.nextScheduleTimeMillis?.takeIf { it != Long.MAX_VALUE },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setSyncEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSyncEnabled(enabled)
        if (enabled) syncScheduler.schedulePeriodic(uiState.value.settings.syncIntervalMinutes) else syncScheduler.cancelPeriodic()
    }

    fun setSyncInterval(minutes: Int) = viewModelScope.launch {
        settingsRepository.setSyncInterval(minutes)
        if (uiState.value.settings.syncEnabled) syncScheduler.schedulePeriodic(minutes)
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setNotificationsEnabled(enabled)
    }

    fun setStoreCountry(country: String) = viewModelScope.launch {
        settingsRepository.setStoreCountry(country)
    }

    fun syncNow() = syncScheduler.syncNow()

    fun resetDemoData() = viewModelScope.launch {
        local.update { it.copy(resetting = true) }
        releaseRepository.resetToSample()
        local.update { it.copy(resetting = false, message = UiMessage(System.nanoTime(), "Demo data restored")) }
    }

    fun onMessageShown(id: Long) = local.update { if (it.message?.id == id) it.copy(message = null) else it }
}
