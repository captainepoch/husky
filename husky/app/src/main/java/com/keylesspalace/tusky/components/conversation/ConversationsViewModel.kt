package com.keylesspalace.tusky.components.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.PagedList
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.network.TimelineCases
import com.keylesspalace.tusky.util.Listing
import com.keylesspalace.tusky.util.NetworkState
import com.keylesspalace.tusky.util.RxAwareViewModel
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class ConversationsViewModel(
    private val repository: ConversationsRepository,
    private val timelineCases: TimelineCases,
    private val database: AppDatabase,
    private val accountManager: AccountManager
) : RxAwareViewModel() {

    private val repoResult = MutableLiveData<Listing<ConversationEntity>>()

    val conversations: LiveData<PagedList<ConversationEntity>> = repoResult.switchMap { it.pagedList }
    val networkState: LiveData<NetworkState> = repoResult.switchMap { it.networkState }
    val refreshState: LiveData<NetworkState> = repoResult.switchMap { it.refreshState }

    fun load() {
        val accountId = accountManager.activeAccount?.id ?: return
        if (repoResult.value == null) {
            repository.refresh(accountId, false)
        }
        repoResult.value = repository.conversations(accountId)
    }

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun retry() {
        repoResult.value?.retry?.invoke()
    }

    fun favourite(favourite: Boolean, position: Int) {
        conversations.value?.getOrNull(position)?.let { conversation ->
            timelineCases.favourite(conversation.lastStatus.toStatus(), favourite)
                .flatMap {
                    val newConversation = conversation.copy(
                        lastStatus = conversation.lastStatus.copy(favourited = favourite)
                    )

                    database.conversationDao().insert(newConversation)
                }
                .subscribeOn(Schedulers.io())
                .doOnError { t -> Timber.w("Failed to favourite conversation: ${t.message}") }
                .onErrorReturnItem(0)
                .subscribe()
                .autoDispose()
        }
    }

    fun bookmark(bookmark: Boolean, position: Int) {
        conversations.value?.getOrNull(position)?.let { conversation ->
            timelineCases.bookmark(conversation.lastStatus.toStatus(), bookmark)
                .flatMap {
                    val newConversation = conversation.copy(
                        lastStatus = conversation.lastStatus.copy(bookmarked = bookmark)
                    )

                    database.conversationDao().insert(newConversation)
                }
                .subscribeOn(Schedulers.io())
                .doOnError { t -> Timber.w("Failed to bookmark conversation: ${t.message}") }
                .onErrorReturnItem(0)
                .subscribe()
                .autoDispose()
        }
    }

    fun voteInPoll(position: Int, choices: MutableList<Int>) {
        conversations.value?.getOrNull(position)?.let { conversation ->
            timelineCases.voteInPoll(conversation.lastStatus.toStatus(), choices)
                .flatMap { poll ->
                    val newConversation = conversation.copy(
                        lastStatus = conversation.lastStatus.copy(poll = poll)
                    )

                    database.conversationDao().insert(newConversation)
                }
                .subscribeOn(Schedulers.io())
                .doOnError { t -> Timber.w("Failed to favourite conversation: ${t.message}") }
                .onErrorReturnItem(0)
                .subscribe()
                .autoDispose()
        }
    }

    fun expandHiddenStatus(expanded: Boolean, position: Int) {
        conversations.value?.getOrNull(position)?.let { conversation ->
            val newConversation = conversation.copy(
                lastStatus = conversation.lastStatus.copy(expanded = expanded)
            )
            saveConversationToDb(newConversation)
        }
    }

    fun collapseLongStatus(collapsed: Boolean, position: Int) {
        conversations.value?.getOrNull(position)?.let { conversation ->
            val newConversation = conversation.copy(
                lastStatus = conversation.lastStatus.copy(collapsed = collapsed)
            )
            saveConversationToDb(newConversation)
        }
    }

    fun showContent(showing: Boolean, position: Int) {
        conversations.value?.getOrNull(position)?.let { conversation ->
            val newConversation = conversation.copy(
                lastStatus = conversation.lastStatus.copy(showingHiddenContent = showing)
            )
            saveConversationToDb(newConversation)
        }
    }

    fun remove(position: Int) {
        conversations.value?.getOrNull(position)?.let {
            refresh()
        }
    }

    private fun saveConversationToDb(conversation: ConversationEntity) {
        database.conversationDao().insert(conversation)
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}
