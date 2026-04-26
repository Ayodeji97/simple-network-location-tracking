package com.danzucker.networklocationtracker.core.domain.util

sealed interface DataError: Error {
    enum class Local: DataError {
        DISK_FULL,
        DATABASE_ERROR,
        UNKNOWN
    }
}