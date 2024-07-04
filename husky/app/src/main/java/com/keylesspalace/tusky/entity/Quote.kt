package com.keylesspalace.tusky.entity

import android.text.Spanned
import com.google.gson.annotations.SerializedName

data class Quote(
    @SerializedName("id")
    val quotedStatusId: String?,
    @SerializedName("url")
    val quotedStatusUrl: String?,
    @SerializedName("content")
    val content: Spanned?,
    @SerializedName("emojis")
    val quoteEmojis: List<Emoji>?,
    @SerializedName("account")
    val account: Account?
)
