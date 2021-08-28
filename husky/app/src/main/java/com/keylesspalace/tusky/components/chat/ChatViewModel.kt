package com.keylesspalace.tusky.components.chat

import com.keylesspalace.tusky.components.common.CommonComposeViewModel
import com.keylesspalace.tusky.components.common.MediaUploader
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.service.ServiceClient
import com.keylesspalace.tusky.util.*
import javax.inject.Inject

open class ChatViewModel
@Inject constructor(
        private val api: MastodonApi,
        private val accountManager: AccountManager,
        private val mediaUploader: MediaUploader,
        private val serviceClient: ServiceClient,
        private val saveTootHelper: SaveTootHelper,
        private val db: AppDatabase
) : CommonComposeViewModel(api, accountManager, mediaUploader, db) {

    fun getSingleMedia() : ComposeActivity.QueuedMedia? {
        return if(media.value?.isNotEmpty() == true)
            media.value?.get(0)
        else null
    }

}