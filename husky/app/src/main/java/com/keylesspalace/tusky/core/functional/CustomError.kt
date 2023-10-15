package com.keylesspalace.tusky.core.functional

sealed class CustomError {

    data object GenericError : CustomError()
    data object NetworkError : CustomError()
}
