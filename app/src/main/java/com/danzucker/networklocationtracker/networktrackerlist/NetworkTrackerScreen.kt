package com.danzucker.networklocationtracker.networktrackerlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danzucker.networklocationtracker.R
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkStatus
import com.danzucker.networklocationtracker.core.presentation.ObserveAsEvents
import com.danzucker.networklocationtracker.ui.theme.NetworkLocationTrackerTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun NetworkTrackerRoot(
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    viewModel: NetworkTrackerListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            NetworkTrackerListEvent.StartService -> onStartService()
            NetworkTrackerListEvent.StopService -> onStopService()
            is NetworkTrackerListEvent.ShowError ->
                snackbarHostState.showSnackbar(event.message.asString(context))
        }
    }

    NetworkTrackerScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkTrackerScreen(
    state: NetworkTrackerListState,
    snackbarHostState: SnackbarHostState,
    onAction: (NetworkTrackerListAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title)) },
                actions = {
                    IconButton(
                        onClick = {
                            onAction(
                                if (state.isTracking) NetworkTrackerListAction.OnStopTracking
                                else NetworkTrackerListAction.OnStartTracking
                            )
                        }
                    ) {
                        Icon(
                            imageVector = if (state.isTracking) Icons.Default.Clear else Icons.Default.PlayArrow,
                            contentDescription = stringResource(
                                if (state.isTracking) R.string.action_stop_tracking
                                else R.string.action_start_tracking
                            ),
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            StatusBanner(
                isTracking = state.isTracking,
                status = state.currentStatus,
            )

            if (state.outages.isEmpty() && state.ongoingOutage == null) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.ongoingOutage?.let { ongoing ->
                        item(key = "ongoing-${ongoing.id}") {
                            OutageCard(outage = ongoing)
                        }
                    }
                    items(items = state.outages, key = { it.id }) { outage ->
                        OutageCard(outage = outage)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(
    isTracking: Boolean,
    status: NetworkStatus?,
) {
    val (labelRes, color) = when {
        !isTracking -> R.string.status_paused to Color(0xFF616161)
        status == NetworkStatus.DISCONNECTED -> R.string.status_disconnected to Color(0xFFB00020)
        status == NetworkStatus.CONNECTED -> R.string.status_connected to Color(0xFF2E7D32)
        else -> R.string.status_determining to Color(0xFF8D6E00)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color,
    ) {
        Text(
            text = stringResource(labelRes),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun OutageCard(outage: NetworkOutageUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = outage.startTimeLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(8.dp))

            OutageRow(
                label = stringResource(R.string.label_lost),
                value = outage.startCoordinates ?: stringResource(R.string.label_location_unavailable),
                subtitle = outage.startAddress,
            )

            if (outage.isOngoing) {
                OutageRow(
                    label = stringResource(R.string.label_restored),
                    value = stringResource(R.string.label_ongoing),
                )
            } else {
                OutageRow(
                    label = stringResource(R.string.label_restored),
                    value = "${outage.endTimeLabel}  (${outage.endCoordinates ?: "—"})",
                    subtitle = outage.endAddress,
                )
                OutageRow(
                    label = stringResource(R.string.label_duration),
                    value = outage.durationLabel ?: "—",
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (outage.isServerReachable) R.string.label_server_reachable
                    else R.string.label_server_unreachable
                ),
                style = MaterialTheme.typography.labelMedium,
                color = if (outage.isServerReachable) Color(0xFF2E7D32) else Color(0xFFB00020),
            )
        }
    }
}

@Composable
private fun OutageRow(label: String, value: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.empty_outages),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
    }
}

// ---------- Previews ----------

private val previewOutages = listOf(
    NetworkOutageUi(
        id = 1L,
        startTimeLabel = "21 Apr, 2026 02:32 PM",
        endTimeLabel = "02:36 PM",
        durationLabel = "4m 12s",
        startCoordinates = "40.7128, -74.0060",
        endCoordinates = "40.7135, -74.0082",
        startAddress = "Broadway, New York, United States",
        endAddress = "Broadway, New York, United States",
        isServerReachable = false,
        isOngoing = false,
    ),
    NetworkOutageUi(
        id = 2L,
        startTimeLabel = "20 Apr, 2026 08:15 AM",
        endTimeLabel = "08:16 AM",
        durationLabel = "1m 03s",
        startCoordinates = "40.7130, -74.0058",
        endCoordinates = "40.7131, -74.0057",
        startAddress = "Wall St, New York, United States",
        endAddress = "Wall St, New York, United States",
        isServerReachable = true,
        isOngoing = false,
    ),
)

@Preview(showBackground = true)
@Composable
private fun NetworkTrackerScreenEmptyPreview() {
    NetworkLocationTrackerTheme {
        NetworkTrackerScreen(
            state = NetworkTrackerListState(
                isTracking = true,
                currentStatus = NetworkStatus.CONNECTED,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkTrackerScreenListPreview() {
    NetworkLocationTrackerTheme {
        NetworkTrackerScreen(
            state = NetworkTrackerListState(
                outages = previewOutages,
                isTracking = true,
                currentStatus = NetworkStatus.CONNECTED,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkTrackerScreenOngoingPreview() {
    NetworkLocationTrackerTheme {
        NetworkTrackerScreen(
            state = NetworkTrackerListState(
                outages = previewOutages,
                ongoingOutage = NetworkOutageUi(
                    id = 3L,
                    startTimeLabel = "22 Apr, 2026 10:05 AM",
                    endTimeLabel = null,
                    durationLabel = null,
                    startCoordinates = "40.7129, -74.0061",
                    endCoordinates = null,
                    startAddress = "Canal St, New York, United States",
                    endAddress = null,
                    isServerReachable = false,
                    isOngoing = true,
                ),
                isTracking = true,
                currentStatus = NetworkStatus.DISCONNECTED,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkTrackerScreenPausedPreview() {
    NetworkLocationTrackerTheme {
        NetworkTrackerScreen(
            state = NetworkTrackerListState(isTracking = false),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkTrackerScreenDeterminingPreview() {
    NetworkLocationTrackerTheme {
        NetworkTrackerScreen(
            state = NetworkTrackerListState(
                isTracking = true,
                currentStatus = null,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
