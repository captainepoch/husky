package com.keylesspalace.tusky.core.extensions

import kotlinx.coroutines.Job

fun Job?.cancelIfActive() {
    if (this?.isActive == true) {
        this.cancel()
    }
}
