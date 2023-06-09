package com.keylesspalace.tusky.entity

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.keylesspalace.tusky.json.CustomDateTypeAdapter
import java.util.Date

/**
 * API type for saving the scroll position of a timeline.
 */
data class Marker(
    @SerializedName("last_read_id") val lastReadId: String,
    @SerializedName("version") val version: Int,
    @JsonAdapter(CustomDateTypeAdapter::class)
    @SerializedName("updated_at")
    val updatedAt: Date
)
