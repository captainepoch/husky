package com.keylesspalace.tusky.core.functional

sealed class CommonState {

    data object Nothing : CommonState()
    data object NetworkError : CommonState()
}
