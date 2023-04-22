package com.keylesspalace.tusky.components.instance

import com.google.gson.annotations.SerializedName

data class InstancePleromaMetadataFieldsLimits(
    @SerializedName("max_fields") val maxFields: Int
)
