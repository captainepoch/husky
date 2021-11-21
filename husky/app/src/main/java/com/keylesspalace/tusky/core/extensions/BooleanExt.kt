package com.keylesspalace.tusky.core.extensions

/**
 * Returns false if a Boolean is null.
 *
 * @return False if null, the value otherwise.
 */
fun Boolean?.orEmpty(): Boolean {
    return (this ?: false)
}
