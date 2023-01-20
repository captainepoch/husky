package com.keylesspalace.tusky.components.instance

import com.google.gson.annotations.SerializedName

data class PollLimits(
    @SerializedName("max_options") val maxOptions: Int?,
    @SerializedName("max_option_chars") val maxOptionChars: Int?
)
