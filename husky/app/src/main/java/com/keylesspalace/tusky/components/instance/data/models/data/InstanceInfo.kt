package com.keylesspalace.tusky.components.instance.data.models.data

import com.keylesspalace.tusky.core.utils.InstanceConstants
import com.keylesspalace.tusky.util.PostFormat

data class InstanceInfo(
    var isLoadingInfo: Boolean = true,
    val maxTootLength: Int = InstanceConstants.DEFAULT_CHARACTER_LIMIT,
    val maxBioLength: Int = InstanceConstants.DEFAULT_BIO_LENGTH,
    val maxBioFields: Int = InstanceConstants.DEFAULT_BIO_MAX_FIELDS,
    val quotePosting: Boolean = false,
    val maxMediaAttachments: Int = InstanceConstants.DEFAULT_STATUS_MEDIA_ITEMS,
    val imageSizeLimit: Long = InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE,
    val videoSizeLimit: Long = InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE,
    val postFormats: List<PostFormat> = emptyList()
)
