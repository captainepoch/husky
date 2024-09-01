package com.keylesspalace.tusky.components.instance.data.models.data

import com.google.gson.annotations.SerializedName

data class MstdnConfiguration(
    @SerializedName("statuses")
    val statuses: MstdnConfigStatuses?,
    @SerializedName("media_attachments")
    val mediaAttachments: MstdnStatusesMediaAttachment?
)

data class MstdnConfigStatuses(
    @SerializedName("max_media_attachments")
    val maxMediaAttachments: Int?,
    @SerializedName("supported_mime_types")
    val postFormats: List<String>?
)

data class MstdnStatusesMediaAttachment(
    @SerializedName("image_size_limit")
    val imageSizeLimit: Long?,
    @SerializedName("video_size_limit")
    val videoSizeLimit: Long?
)
