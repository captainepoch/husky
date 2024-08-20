package com.keylesspalace.tusky.components.instance.data.models.data

import com.google.gson.annotations.SerializedName

data class MstdnConfiguration(
    @SerializedName("statuses") val statuses: MstdnConfigStatused?
)

data class MstdnConfigStatused(
    @SerializedName("max_media_attachments") val maxMediaAttachments: Int?
)
