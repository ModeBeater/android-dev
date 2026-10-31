package kz.kbtu.releaseflow.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import kz.kbtu.releaseflow.data.repository.Settings
import kz.kbtu.releaseflow.data.work.Notifications as AppNotifications
import kz.kbtu.releaseflow.domain.util.ageText
import kz.kbtu.releaseflow.domain.util.dateTimeText
import kz.kbtu.releaseflow.ui.AppViewModelProvider
import kz.kbtu.releaseflow.ui.components.AppSwitch
import kz.kbtu.releaseflow.ui.components.ConfirmDialog
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.Hairline
import kz.kbtu.releaseflow.ui.components.KeyValueRow
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.components.SecondaryButton
import kz.kbtu.releaseflow.ui.components.SectionHeader
import kz.kbtu.releaseflow.ui.components.SegmentedPills
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * Settings tab: the DataStore preferences (sync, interval, notifications, store country),
 * what WorkManager is doing with them (status of the periodic and the manual request),
 * and housekeeping (reset the demo data). Every switch writes to DataStore and the screen
 * re-renders from its Flow — there is no local copy of the settings.
 */
@Composable
fun SettingsScreen(
    onShowMessage: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val colors = ReleaseFlowTheme.colors
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = ui.settings
    val context = LocalContext.current
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    // Android 13+: notifications need a runtime permission; ask when the switch is turned on.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setNotificationsEnabled(granted)
    }
    val notificationsAllowed = AppNotifications.canNotify(context)

    LaunchedEffect(ui.message?.id) {
        val message = ui.message ?: return@LaunchedEffect
        try {
            onShowMessage(message.text)
        } finally {
            viewModel.onMessageShown(message.id)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PageListPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                SectionHeader(title = "Store sync")
                FlatCard(padding = PaddingValues(0.dp)) {
                    KeyValueRow(
                        label = "Check the App Store in the background",
                        caption = "WorkManager · needs network, battery not low",
                        icon = Icons.Default.Refresh,
                        trailing = { AppSwitch(checked = settings.syncEnabled, onCheckedChange = viewModel::setSyncEnabled) },
                    )
                    Hairline()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(text = "Every", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        SegmentedPills(
                            options = Settings.intervals,
                            selected = settings.syncIntervalMinutes,
                            label = { intervalLabel(it) },
                            onSelect = viewModel::setSyncInterval,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = periodicStatusText(ui.periodicState, ui.periodicNextRunAt, settings.syncEnabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Hairline()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(text = "App Store country", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            text = "Which storefront the lookup asks — an app may be published in one and not another",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SegmentedPills(
                            options = Settings.countries,
                            selected = settings.storeCountry,
                            label = { it.uppercase() },
                            onSelect = viewModel::setStoreCountry,
                        )
                    }
                }
            }
        }

        item {
            FlatCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Last sync", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        Text(
                            text = settings.lastSyncAt?.let { "${it.dateTimeText()} · ${it.ageText()} ago" } ?: "never",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        settings.lastSyncSummary?.let {
                            Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                    if (ui.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(
                    text = when (ui.manualState) {
                        WorkInfo.State.ENQUEUED -> "Waiting for network…"
                        WorkInfo.State.RUNNING -> "Syncing…"
                        else -> "Sync now"
                    },
                    onClick = viewModel::syncNow,
                    enabled = !ui.isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "One-time work request · " + manualStatusText(ui.manualState, ui.manualResult),
                    style = MonoText.small,
                    color = colors.textTertiary,
                )
            }
        }

        item {
            Column {
                SectionHeader(title = "Notifications")
                FlatCard(padding = PaddingValues(0.dp)) {
                    KeyValueRow(
                        label = "New store versions",
                        caption = if (notificationsAllowed) "Posted by the sync worker when a version changes"
                        else "Allow notifications for ReleaseFlow in system settings",
                        icon = Icons.Default.Notifications,
                        iconTint = colors.warning,
                        trailing = {
                            AppSwitch(
                                checked = settings.notificationsEnabled && notificationsAllowed,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setNotificationsEnabled(enabled)
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "Data")
                FlatCard {
                    Text(
                        text = "Everything you see is stored in a local Room database (SQLite) and survives restarts. " +
                            "Reset drops it and inserts the demo apps and releases again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = if (ui.resetting) "Resetting…" else "Reset demo data",
                        onClick = { showResetDialog = true },
                        enabled = !ui.resetting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "About")
                FlatCard(padding = PaddingValues(0.dp)) {
                    KeyValueRow(label = "ReleaseFlow", caption = "KBTU · Mobile Programming · endterm project", value = "2.0", mono = true, valueColor = colors.textSecondary)
                    Hairline()
                    KeyValueRow(label = "Persistence", caption = "Room (6 tables) + Preferences DataStore")
                    Hairline()
                    KeyValueRow(label = "Network", caption = "Retrofit + kotlinx.serialization · iTunes Lookup, GitHub REST · OkHttp WebSocket")
                    Hairline()
                    KeyValueRow(label = "Background", caption = "WorkManager periodic + one-time requests, notifications")
                    Hairline()
                    KeyValueRow(label = "Architecture", caption = "MVVM · Repository · manual DI (AppContainer) · StateFlow")
                }
            }
        }
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset demo data?",
            text = "All apps and releases you added will be deleted and the four demo apps restored.",
            confirmLabel = "Reset",
            onConfirm = {
                showResetDialog = false
                viewModel.resetDemoData()
            },
            onDismiss = { showResetDialog = false },
        )
    }
}

private fun intervalLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % (24 * 60) == 0 -> "${minutes / (24 * 60)} d"
    else -> "${minutes / 60} h"
}

private fun periodicStatusText(state: WorkInfo.State?, nextRunAt: Long?, enabled: Boolean): String = when {
    !enabled -> "Periodic work cancelled"
    state == null -> "Scheduling…"
    state == WorkInfo.State.RUNNING -> "Periodic work running now"
    nextRunAt != null -> "Periodic work ${state.name.lowercase()} · next run ≈ ${nextRunAt.dateTimeText()}"
    else -> "Periodic work ${state.name.lowercase()}"
}

private fun manualStatusText(state: WorkInfo.State?, result: String?): String = when (state) {
    null -> "not started yet"
    WorkInfo.State.SUCCEEDED -> "succeeded" + (result?.let { " ($it)" } ?: "")
    else -> state.name.lowercase()
}
