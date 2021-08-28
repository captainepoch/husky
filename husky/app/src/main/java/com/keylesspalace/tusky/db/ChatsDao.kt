package com.keylesspalace.tusky.db

import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import io.reactivex.Single

@Dao
abstract class ChatsDao {

    @Query("""SELECT c.chatId, c.localId, c.accountId, c.lastMessageId, c.unread, c.updatedAt,
a.serverId as 'a_serverId', a.timelineUserId as 'a_timelineUserId',
a.localUsername as 'a_localUsername', a.username as 'a_username',
a.displayName as 'a_displayName', a.url as 'a_url', a.avatar as 'a_avatar',
a.emojis as 'a_emojis', a.bot as 'a_bot',
msg.accountId as 'msg_accountId', msg.localId as 'msg_localId',
msg.chatId as 'msg_chatId', msg.attachment as 'msg_attachment',
msg.content as 'msg_content', msg.createdAt as 'msg_createdAt', msg.emojis as 'msg_emojis',
msg.messageId as 'msg_messageId'
FROM ChatEntity c
LEFT JOIN TimelineAccountEntity a ON (a.timelineUserId == :localId AND a.serverId = c.accountId)
LEFT JOIN ChatMessageEntity msg ON (msg.localId == :localId AND msg.chatId == c.chatId)
WHERE c.localId = :localId
AND (CASE WHEN :maxId IS NOT NULL THEN
(LENGTH(c.chatId) < LENGTH(:maxId) OR LENGTH(c.chatId) == LENGTH(:maxId) AND c.chatId < :maxId)
ELSE 1 END)
AND (CASE WHEN :sinceId IS NOT NULL THEN
(LENGTH(c.chatId) > LENGTH(:sinceId) OR LENGTH(c.chatId) == LENGTH(:sinceId) AND c.chatId > :sinceId)
ELSE 1 END)
ORDER BY c.updatedAt DESC
LIMIT :limit
    """)
    abstract fun getChatsForAccount(localId: Long, maxId: String?, sinceId: String?, limit: Int) : Single<List<ChatEntityWithAccount>>

    @Insert(onConflict = REPLACE)
    abstract fun insertChat(chatEntity: ChatEntity) : Long

    @Insert(onConflict = IGNORE)
    abstract fun insertChatIfNotThere(chatEntity: ChatEntity): Long

    @Insert(onConflict = REPLACE)
    abstract fun insertAccount(accountEntity: TimelineAccountEntity) : Long

    @Insert(onConflict = REPLACE)
    abstract fun insertChatMessage(chatMessageEntity: ChatMessageEntity) : Long

    @Transaction
    open fun insertInTransaction(chatEntity: ChatEntity, lastMessage: ChatMessageEntity?, accountEntity: TimelineAccountEntity) {
        insertAccount(accountEntity)
        lastMessage?.let(this::insertChatMessage)
        insertChat(chatEntity)
    }

    @Transaction
    open fun setLastMessage(accountId: Long, chatId: String, lastMessageEntity: ChatMessageEntity) {
        insertChatMessage(lastMessageEntity)
        setLastMessageId(accountId, chatId, lastMessageEntity.messageId)
    }

    @Query("""UPDATE ChatEntity SET lastMessageId = :messageId WHERE localId = :localId AND chatId = :chatId""")
    abstract fun setLastMessageId(localId: Long, chatId: String, messageId: String)

    @Query("""DELETE FROM ChatEntity WHERE accountId = ""
AND localId = :account AND
(LENGTH(chatId) < LENGTH(:maxId) OR LENGTH(chatId) == LENGTH(:maxId) AND chatId < :maxId)
AND
(LENGTH(chatId) > LENGTH(:sinceId) OR LENGTH(chatId) == LENGTH(:sinceId) AND chatId > :sinceId)
""")
    abstract fun removeAllPlaceholdersBetween(account: Long, maxId: String, sinceId: String)

    @Query("""DELETE FROM ChatEntity WHERE localId = :accountId AND
(LENGTH(chatId) < LENGTH(:maxId) OR LENGTH(chatId) == LENGTH(:maxId) AND chatId < :maxId)
AND
(LENGTH(chatId) > LENGTH(:minId) OR LENGTH(chatId) == LENGTH(:minId) AND chatId > :minId)
    """)
    abstract fun deleteRange(accountId: Long, minId: String, maxId: String)


    @Query("""DELETE FROM ChatEntity WHERE localId = :localId AND accountId = :accountId""")
    abstract fun deleteChatByAccount(localId: Long, accountId: String)

    @Query("""DELETE FROM ChatEntity WHERE localId = :localId AND chatId = :chatId""")
    abstract fun deleteChat(localId: Long, chatId: String)
}