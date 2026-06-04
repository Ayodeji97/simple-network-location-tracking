package com.danzucker.networklocationtracker.networktrackerlist

sealed interface NetworkTrackerListAction {
    data object OnStartTracking : NetworkTrackerListAction
    data object OnStopTracking : NetworkTrackerListAction
    data object OnClearAll : NetworkTrackerListAction
    data object OnDismissError : NetworkTrackerListAction
}
