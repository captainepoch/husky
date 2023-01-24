package com.keylesspalace.tusky.components.instance

import com.keylesspalace.tusky.core.utils.InstanceConstants

data class InstanceInfo(
    val maxBioLength: Int = InstanceConstants.DEFAULT_BIO_LENGTH,
    val maxTootLength: Int = InstanceConstants.DEFAULT_CHARACTER_LIMIT
)
