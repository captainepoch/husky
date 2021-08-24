package com.keylesspalace.tusky.viewdata

import android.text.Spanned
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.entity.Card
import com.keylesspalace.tusky.entity.Emoji
import java.util.*


abstract class ChatViewData {
    abstract fun getViewDataId() : Int
    abstract fun deepEquals(o: ChatViewData) : Boolean

    class Concrete(val account : Account,
        val id: String,
        val unread: Long,
        val lastMessage: ChatMessageViewData.Concrete?,
        val updatedAt: Date ) : ChatViewData() {
        override fun getViewDataId(): Int {
            return id.hashCode()
        }

        override fun deepEquals(o: ChatViewData): Boolean {
            if (o !is Concrete) return false
            return Objects.equals(o.account, account)
                    && Objects.equals(o.id, id)
                    && o.unread == unread
                    && (lastMessage == o.lastMessage || (lastMessage != null && o.lastMessage != null && o.lastMessage.deepEquals(lastMessage)))
                    && Objects.equals(o.updatedAt, updatedAt)
        }

        override fun hashCode(): Int {
            return Objects.hash(account, id, unread, lastMessage, updatedAt)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            return deepEquals(other as Concrete)
        }
    }

    class Placeholder(val id: String, val isLoading: Boolean) : ChatViewData() {
        override fun getViewDataId(): Int {
            return id.hashCode()
        }

        override fun deepEquals(o: ChatViewData): Boolean {
            if( o !is Placeholder ) return false
            return o.isLoading == isLoading && o.id == id
        }

        override fun hashCode(): Int {
            var result = if (isLoading) 1 else 0
            result = 31 * result + id.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            return deepEquals(other as Placeholder)
        }
    }
}

abstract class ChatMessageViewData {
    abstract fun getViewDataId() : Int
    abstract fun deepEquals(o: ChatMessageViewData) : Boolean

    class Concrete(val id: String,
                   val content: Spanned?,
                   val chatId: String,
                   val accountId: String,
                   val createdAt: Date,
                   val attachment: Attachment?,
                   val emojis: List<Emoji>,
                   val card: Card?) : ChatMessageViewData()
    {
        override fun getViewDataId(): Int {
            return id.hashCode()
        }

        override fun deepEquals(o: ChatMessageViewData): Boolean {
            if( o !is Concrete ) return false

            return Objects.equals(o.id, id)
                    && Objects.equals(o.content, content)
                    && Objects.equals(o.chatId, chatId)
                    && Objects.equals(o.accountId, accountId)
                    && Objects.equals(o.createdAt, createdAt)
                    && Objects.equals(o.attachment, attachment)
                    && Objects.equals(o.emojis, emojis)
                    && Objects.equals(o.card, card)
        }

        override fun hashCode() : Int {
            return Objects.hash(id, content, chatId, accountId, createdAt, attachment, card)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            return deepEquals(other as Concrete)
        }
    }

    class Placeholder(val id: String, val isLoading: Boolean) : ChatMessageViewData() {
        override fun getViewDataId(): Int {
            return id.hashCode()
        }

        override fun deepEquals(o: ChatMessageViewData): Boolean {
            if( o !is Placeholder) return false
            return o.isLoading == isLoading && o.id == id
        }

        override fun hashCode(): Int {
            var result = if (isLoading) 1 else 0
            result = 31 * result + id.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            return deepEquals(other as Placeholder)
        }
    }
}