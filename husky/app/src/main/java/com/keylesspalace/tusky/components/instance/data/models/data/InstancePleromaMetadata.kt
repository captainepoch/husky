package com.keylesspalace.tusky.components.instance.data.models.data

import com.google.gson.annotations.SerializedName

data class InstancePleromaMetadata(
    @SerializedName("features")
    val features: List<String>,
    @SerializedName("fields_limits")
    val fieldsLimits: InstancePleromaMetadataFieldsLimits,
    @SerializedName("post_formats")
    val postsFormats: List<String>?
)
