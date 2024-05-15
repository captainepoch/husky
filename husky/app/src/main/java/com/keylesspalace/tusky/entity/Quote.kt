package com.keylesspalace.tusky.entity

import android.text.Spanned
import com.google.gson.annotations.SerializedName

data class Quote(
    @SerializedName("content")
    val content: Spanned?,
    @SerializedName("emojis")
    val quoteEmojis: List<Emoji>?
)
