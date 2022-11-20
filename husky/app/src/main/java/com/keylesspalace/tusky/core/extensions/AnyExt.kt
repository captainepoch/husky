package com.keylesspalace.tusky.core.extensions

fun Any?.notNull(): Boolean {
    return (this != null)
}
