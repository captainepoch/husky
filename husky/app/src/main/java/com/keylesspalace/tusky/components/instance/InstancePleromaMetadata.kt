package com.keylesspalace.tusky.components.instance

import com.google.gson.annotations.SerializedName

data class InstancePleromaMetadata(
    val features: List<String>,
    @SerializedName("fields_limits") val fieldsLimits: InstancePleromaMetadataFieldsLimits
    // TODO(InstanceCapabilities)
    // @SerializedName("post_formats") val postsFormats: List<String>
)
