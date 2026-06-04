package com.danzucker.networklocationtracker.networktrackerlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danzucker.networklocationtracker.core.data.networktracker.NetworkWithLocationTracker
import com.danzucker.networklocationtracker.core.data.networktracker.TrackingEvent
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutageRepository
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkStatus
import com.danzucker.networklocationtracker.core.presentation.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkTrackerListViewModel(
    private val repository: NetworkOutageRepository,
    private val tracker: NetworkWithLocationTracker,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        NetworkTrackerListState(
            isTracking = savedStateHandle[KEY_IS_TRACKING] ?: false,
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<NetworkTrackerListEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeOutages()
        observeTrackingEvents()
        observeNetworkStatus()
    }

    fun onAction(action: NetworkTrackerListAction) {
        when (action) {
            NetworkTrackerListAction.OnStartTracking -> setTracking(true)
            NetworkTrackerListAction.OnStopTracking -> setTracking(false)
            NetworkTrackerListAction.OnClearAll -> viewModelScope.launch {
                repository.deleteAllNetworkOutages()
            }
            NetworkTrackerListAction.OnDismissError -> Unit
        }
    }

    private fun setTracking(tracking: Boolean) {
        savedStateHandle[KEY_IS_TRACKING] = tracking
        _state.update { it.copy(isTracking = tracking) }
        if (!tracking) {
            tracker.resetStatus()
            _state.update { it.copy(currentStatus = null) }
        }
        viewModelScope.launch {
            _events.send(
                if (tracking) NetworkTrackerListEvent.StartService
                else NetworkTrackerListEvent.StopService
            )
        }
    }

    private fun observeOutages() {
        _state.update { it.copy(isLoading = true) }
        repository.getNetworkOutages()
            .onEach { outages ->
                _state.update { current ->
                    val uiOutages = outages.map { it.toNetworkOutageUi() }
                    current.copy(
                        outages = uiOutages.filterNot { it.isOngoing },
                        ongoingOutage = uiOutages.firstOrNull { it.isOngoing },
                        isLoading = false,
                    )
                }
            }
            .catch { error ->
                _state.update { it.copy(isLoading = false) }
                _events.send(
                    NetworkTrackerListEvent.ShowError(
                        UiText.Dynamic("Failed to load outages: ${error.message}")
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeTrackingEvents() {
        tracker.trackingEvents
            .onEach { event ->
                if (event is TrackingEvent.Error) {
                    _events.send(
                        NetworkTrackerListEvent.ShowError(UiText.Dynamic(event.message))
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeNetworkStatus() {
        tracker.currentStatus
            .onEach { status -> _state.update { it.copy(currentStatus = status) } }
            .launchIn(viewModelScope)
    }

    private companion object {
        const val KEY_IS_TRACKING = "is_tracking"
    }
}

data class NetworkTrackerListState(
    val outages: List<NetworkOutageUi> = emptyList(),
    val ongoingOutage: NetworkOutageUi? = null,
    val isTracking: Boolean = false,
    val isLoading: Boolean = false,
    val currentStatus: NetworkStatus? = null,
)
