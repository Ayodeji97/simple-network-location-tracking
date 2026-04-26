package com.danzucker.networklocationtracker.networktrackerlist

import com.danzucker.networklocationtracker.core.presentation.UiText

sealed interface NetworkTrackerListEvent {
    data object StartService : NetworkTrackerListEvent
    data object StopService : NetworkTrackerListEvent
    data class ShowError(val message: UiText) : NetworkTrackerListEvent
}
