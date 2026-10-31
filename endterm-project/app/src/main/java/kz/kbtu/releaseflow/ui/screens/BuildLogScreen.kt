package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.kbtu.releaseflow.domain.util.timeText
import kz.kbtu.releaseflow.ui.AppViewModelProvider
import kz.kbtu.releaseflow.ui.components.AppPill
import kz.kbtu.releaseflow.ui.components.AppTextField
import kz.kbtu.releaseflow.ui.components.DangerButton
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.PagePadding
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.components.SecondaryButton
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * Live build log over a WebSocket: status pill, the stream of lines (auto-scrolled to the
 * newest), the pipeline replay and a field to push a line of your own through the socket.
 */
@Composable
fun BuildLogScreen(
    modifier: Modifier = Modifier,
    viewModel: BuildLogViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val colors = ReleaseFlowTheme.colors
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Follow the newest line, like a terminal.
    LaunchedEffect(ui.lines.size) {
        if (ui.lines.isNotEmpty()) listState.animateScrollToItem(ui.lines.size - 1)
    }

    val statusColor = when (ui.status) {
        SocketStatus.Connected -> colors.success
        SocketStatus.Connecting -> colors.warning
        SocketStatus.Failed -> colors.danger
        SocketStatus.Disconnected -> colors.textSecondary
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PagePadding)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        FlatCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = ui.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Text(text = ui.pipeline, style = MonoText.small, color = colors.textSecondary)
                }
                AppPill(
                    label = ui.status.label,
                    color = statusColor,
                    filled = ui.status == SocketStatus.Connected,
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (ui.status == SocketStatus.Connected) colors.onAccent else statusColor, CircleShape),
                        )
                    },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            AppTextField(
                value = ui.url,
                onValueChange = viewModel::onUrlChange,
                label = "WebSocket server",
                enabled = ui.status == SocketStatus.Disconnected || ui.status == SocketStatus.Failed,
                supportingText = ui.statusDetail ?: "A public echo server: every frame sent comes back as a log line",
                textStyle = MonoText.medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (ui.status) {
                    SocketStatus.Connected -> {
                        SecondaryButton(
                            text = if (ui.pipelineRunning) "Pipeline running…" else "Replay pipeline",
                            onClick = viewModel::runPipeline,
                            enabled = !ui.pipelineRunning,
                            modifier = Modifier.weight(1f),
                        )
                        DangerButton(text = "Disconnect", onClick = viewModel::disconnect, modifier = Modifier.weight(1f))
                    }
                    SocketStatus.Connecting -> SecondaryButton(text = "Connecting…", onClick = {}, enabled = false, modifier = Modifier.weight(1f))
                    else -> PrimaryButton(text = "Connect", onClick = viewModel::connect, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        // The log itself: a dark mono block, newest line at the bottom.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (colors.isDark) colors.surface else colors.textPrimary, MaterialTheme.shapes.large)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (ui.lines.isEmpty()) {
                item {
                    Text(
                        text = if (ui.status == SocketStatus.Connected) "waiting for frames…" else "no connection — tap Connect",
                        style = MonoText.small,
                        color = colors.textTertiary,
                    )
                }
            }
            items(ui.lines) { line ->
                Row {
                    Text(text = line.at.timeText(), style = MonoText.small, color = colors.textTertiary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = line.text,
                        style = MonoText.small,
                        color = when {
                            !line.fromServer -> colors.textTertiary
                            "SUCCESS" in line.text -> colors.success
                            "[manual]" in line.text -> colors.accent
                            else -> if (colors.isDark) colors.textPrimary else colors.surface
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                value = ui.draft,
                onValueChange = viewModel::onDraftChange,
                label = "Send a line",
                enabled = ui.status == SocketStatus.Connected,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendDraft() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(10.dp))
            SecondaryButton(text = "Send", onClick = viewModel::sendDraft, enabled = ui.status == SocketStatus.Connected && ui.draft.isNotBlank())
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
