package com.keylesspalace.tusky.components.instance.data.models

enum class InstanceFeatures(private val value: String) {
    QUOTE_POSTING("quote_posting");

    companion object {

        fun getInstanceFeature(value: String): InstanceFeatures? {
            return when (value) {
                QUOTE_POSTING.value -> QUOTE_POSTING
                else -> null
            }
        }
    }
}
