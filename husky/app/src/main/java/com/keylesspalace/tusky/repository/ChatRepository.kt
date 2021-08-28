package com.keylesspalace.tusky.repository

import android.text.SpannedString
import androidx.core.text.parseAsHtml
import androidx.core.text.toHtml
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.keylesspalace.tusky.db.*
import com.keylesspalace.tusky.entity.*
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.repository.TimelineRequestMode.DISK
import com.keylesspalace.tusky.repository.TimelineRequestMode.NETWORK
import com.keylesspalace.tusky.util.Either
import com.keylesspalace.tusky.util.dec
import com.keylesspalace.tusky.util.inc
import com.keylesspalace.tusky.util.trimTrailingWhitespace
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.*

typealias ChatStatus = Either<Placeholder, Chat>
typealias ChatMesssageOrPlaceholder = Either<Placeholder, ChatMessage>

interface ChatRepository {
    fun getChats(maxId: String?, sinceId: String?, sincedIdMinusOne: String?, limit: Int,
                    requestMode: TimelineRequestMode): Single<out List<ChatStatus>>

    fun getChatMessages(chatId: String, maxId: String?, sinceId: String?, sincedIdMinusOne: String?, limit: Int, requestMode: TimelineRequestMode) : Single<out List<ChatMesssageOrPlaceholder>>
}

class ChatRepositoryImpl(
        private val chatsDao: ChatsDao,
        private val mastodonApi: MastodonApi,
        private val accountManager: AccountManager,
        private val gson: Gson
) : ChatRepository {

    override fun getChats(maxId: String?, sinceId: String?, sincedIdMinusOne: String?,
                          limit: Int, requestMode: TimelineRequestMode
    ): Single<out List<ChatStatus>> {
        val acc = accountManager.activeAccount ?: throw IllegalStateException()
        val accountId = acc.id

        return if (requestMode == DISK) {
            this.getChatsFromDb(accountId, maxId, sinceId, limit)
        } else {
            getChatsFromNetwork(maxId, sinceId, sincedIdMinusOne, limit, accountId, requestMode)
        }
    }

    override fun getChatMessages(chatId: String, maxId: String?, sinceId: String?, sincedIdMinusOne: String?, limit: Int, requestMode: TimelineRequestMode) : Single<out List<ChatMesssageOrPlaceholder>> {
        val acc = accountManager.activeAccount ?: throw IllegalStateException()
        val accountId = acc.id

        /*return if (requestMode == DISK) {
            getChatMessagesFromDb(chatId, accountId, maxId, sinceId, limit)
        } else {
            getChatMessagesFromNetwork(chatId, maxId, sinceId, sincedIdMinusOne, limit, accountId, requestMode)
        }*/

        return getChatMessagesFromNetwork(chatId, maxId, sinceId, sincedIdMinusOne, limit, accountId, requestMode)
    }

    private fun getChatsFromNetwork(maxId: String?, sinceId: String?,
                                       sinceIdMinusOne: String?, limit: Int,
                                       accountId: Long, requestMode: TimelineRequestMode
    ): Single<out List<ChatStatus>> {
        return mastodonApi.getChats(maxId, null, sinceIdMinusOne, 0, limit + 1)
                .map { chats ->
                    this.saveChatsToDb(accountId, chats, maxId, sinceId)
                }
                .flatMap { chats ->
                    this.addFromDbIfNeeded(accountId, chats, maxId, sinceId, limit, requestMode)
                }
                .onErrorResumeNext { error ->
                    if (error is IOException && requestMode != NETWORK) {
                        this.getChatsFromDb(accountId, maxId, sinceId, limit)
                    } else {
                        Single.error(error)
                    }
                }
    }

    private fun getChatMessagesFromNetwork(chatId: String, maxId: String?, sinceId: String?,
                                    sinceIdMinusOne: String?, limit: Int,
                                    accountId: Long, requestMode: TimelineRequestMode
    ): Single<out List<ChatMesssageOrPlaceholder>> {
        return mastodonApi.getChatMessages(chatId, maxId, null, sinceIdMinusOne, 0, limit + 1).map {
            it.mapTo(mutableListOf(), ChatMessage::lift)
        }
    }


    private fun addFromDbIfNeeded(accountId: Long, chats: List<ChatStatus>,
                                  maxId: String?, sinceId: String?, limit: Int,
                                  requestMode: TimelineRequestMode
    ): Single<List<ChatStatus>> {
        return if (requestMode != NETWORK && chats.size < 2) {
            val newMaxID = if (chats.isEmpty()) {
                maxId
            } else {
                chats.last { it.isRight() }.asRight().id
            }
            this.getChatsFromDb(accountId, newMaxID, sinceId, limit)
                    .map { fromDb ->
                        // If it's just placeholders and less than limit (so we exhausted both
                        // db and server at this point)
                        if (fromDb.size < limit && fromDb.all { !it.isRight() }) {
                            chats
                        } else {
                            chats + fromDb
                        }
                    }
        } else {
            Single.just(chats)
        }
    }

    private fun getChatsFromDb(accountId: Long, maxId: String?, sinceId: String?,
                                  limit: Int): Single<out List<ChatStatus>> {
        return chatsDao.getChatsForAccount(accountId, maxId, sinceId, limit)
                .subscribeOn(Schedulers.io())
                .map { chats ->
                    chats.map { it.toChat(gson) }
                }
    }


    private fun saveChatsToDb(accountId: Long, chats: List<Chat>,
                                 maxId: String?, sinceId: String?
    ): List<ChatStatus> {
        var placeholderToInsert: Placeholder? = null

        // Look for overlap
        val resultChats = if (chats.isNotEmpty() && sinceId != null) {
            val indexOfSince = chats.indexOfLast { it.id == sinceId }
            if (indexOfSince == -1) {
                // We didn't find the status which must be there. Add a placeholder
                placeholderToInsert = Placeholder(sinceId.inc())
                chats.mapTo(mutableListOf(), Chat::lift)
                        .apply {
                            add(Either.Left(placeholderToInsert))
                        }
            } else {
                // There was an overlap. Remove all overlapped statuses. No need for a placeholder.
                chats.mapTo(mutableListOf(), Chat::lift)
                        .apply {
                            subList(indexOfSince, size).clear()
                        }
            }
        } else {
            // Just a normal case.
            chats.map(Chat::lift)
        }

        Single.fromCallable {

            if(chats.isNotEmpty()) {
                chatsDao.deleteRange(accountId, chats.last().id, chats.first().id)
            }

            for (chat in chats) {
                val pair = chat.toEntity(accountId, gson)

                chatsDao.insertInTransaction(
                        pair.first,
                        pair.second,
                        chat.account.toEntity(accountId, gson)
                )
            }

            placeholderToInsert?.let {
                chatsDao.insertChatIfNotThere(it.toChatEntity(accountId))
            }

            // If we're loading in the bottom insert placeholder after every load
            // (for requests on next launches) but not return it.
            if (sinceId == null && chats.isNotEmpty()) {
                chatsDao.insertChatIfNotThere(
                        Placeholder(chats.last().id.dec()).toChatEntity(accountId))
            }

            // There may be placeholders which we thought could be from our TL but they are not
            if (chats.size > 2) {
                chatsDao.removeAllPlaceholdersBetween(accountId, chats.first().id,
                        chats.last().id)
            } else if (placeholderToInsert == null && maxId != null && sinceId != null) {
                chatsDao.removeAllPlaceholdersBetween(accountId, maxId, sinceId)
            }
        }
                .subscribeOn(Schedulers.io())
                .subscribe()

        return resultChats
    }
}

private val emojisListTypeToken = object : TypeToken<List<Emoji>>() {}

fun Placeholder.toChatEntity(timelineUserId: Long): ChatEntity {
    return ChatEntity(
            localId = timelineUserId,
            chatId = this.id,
            accountId = "",
            unread = 0L,
            updatedAt = 0L,
            lastMessageId = null
    )
}

fun ChatMessage.toEntity(timelineUserId: Long, gson: Gson) : ChatMessageEntity {
    return ChatMessageEntity(
            localId = timelineUserId,
            messageId = this.id,
            content = this.content?.toHtml(),
            chatId = this.chatId,
            accountId = this.accountId,
            createdAt = this.createdAt.time,
            attachment = this.attachment?.let { gson.toJson(it, Attachment::class.java) },
            emojis = gson.toJson(this.emojis)
    )
}

fun Chat.toEntity(timelineUserId: Long, gson: Gson): Pair<ChatEntity, ChatMessageEntity?> {
    return Pair(ChatEntity(
            localId = timelineUserId,
            chatId = this.id,
            accountId = this.account.id,
            unread = this.unread,
            updatedAt = this.updatedAt.time,
            lastMessageId = this.lastMessage?.id
    ), this.lastMessage?.toEntity(timelineUserId, gson))
}

fun ChatMessageEntity.toChatMessage(gson: Gson) : ChatMessage {
    return ChatMessage(
            id = this.messageId,
            content = this.content?.let { it.parseAsHtml().trimTrailingWhitespace() },
            chatId = this.chatId,
            accountId = this.accountId,
            createdAt = Date(this.createdAt),
            attachment = this.attachment?.let { gson.fromJson(it, Attachment::class.java) },
            emojis = gson.fromJson(this.emojis, object : TypeToken<List<Emoji>>() {}.type ),
            card = null /* don't care about card */
    )
}

fun ChatEntityWithAccount.toChat(gson: Gson) : ChatStatus {
    if(account == null || chat.accountId.isEmpty() || chat.updatedAt == 0L)
        return Either.Left(Placeholder(chat.chatId))

    return Chat(
            account = this.account?.toAccount(gson) ?: Account("", "", "", "", SpannedString(""), "", "", "" ),
            id = this.chat.chatId,
            unread = this.chat.unread,
            updatedAt = Date(this.chat.updatedAt),
            lastMessage = this.lastMessage?.toChatMessage(gson)
    ).lift()
}

fun ChatMessage.lift(): ChatMesssageOrPlaceholder = Either.Right(this)

fun Chat.lift(): ChatStatus = Either.Right(this)
