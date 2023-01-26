package com.keylesspalace.tusky.core.network

sealed class ApiResponse<out T> {

    data class Success<out T>(val data: T) : ApiResponse<T>()
}
