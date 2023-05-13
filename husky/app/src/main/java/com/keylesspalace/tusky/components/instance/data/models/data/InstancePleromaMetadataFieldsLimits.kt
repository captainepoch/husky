package com.keylesspalace.tusky.components.instance.data.models.data

import com.google.gson.annotations.SerializedName

data class InstancePleromaMetadataFieldsLimits(
    @SerializedName("max_fields") val maxFields: Int
)
