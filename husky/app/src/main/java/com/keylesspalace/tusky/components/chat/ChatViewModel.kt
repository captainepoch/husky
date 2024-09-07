package com.keylesspalace.tusky.components.chat

import com.keylesspalace.tusky.components.common.CommonComposeViewModel
import com.keylesspalace.tusky.components.common.MediaUploader
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.instance.domain.repository.InstanceRepository
import com.keylesspalace.tusky.network.MastodonApi

open class ChatViewModel(
    api: MastodonApi,
    instanceRepository: InstanceRepository,
    mediaUploader: MediaUploader
) : CommonComposeViewModel(api, mediaUploader, instanceRepository) {

    fun getSingleMedia(): ComposeActivity.QueuedMedia? {
        return if (media.value?.isNotEmpty() == true) {
            media.value?.get(0)
        } else {
            null
        }
    }
}
