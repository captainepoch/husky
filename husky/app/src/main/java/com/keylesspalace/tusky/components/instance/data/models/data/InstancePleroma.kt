package com.keylesspalace.tusky.components.instance.data.models.data

import com.google.gson.annotations.SerializedName

data class InstancePleroma(
    @SerializedName("metadata") val metadata: InstancePleromaMetadata
)
