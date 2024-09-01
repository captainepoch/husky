package com.keylesspalace.tusky.core.extensions

fun Long.toMB(): Long {
    return (this / (1000 * 1000))
}
