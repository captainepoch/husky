/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2018  Tusky Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.keylesspalace.tusky.repository

import android.text.Spanned
import android.text.SpannedString
import androidx.core.text.parseAsHtml
import androidx.core.text.toHtml
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.keylesspalace.tusky.core.extensions.empty
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.db.TimelineAccountEntity
import com.keylesspalace.tusky.db.TimelineDao
import com.keylesspalace.tusky.db.TimelineStatusEntity
import com.keylesspalace.tusky.db.TimelineStatusWithAccount
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.entity.Poll
import com.keylesspalace.tusky.entity.Quote
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.repository.TimelineRequestMode.DISK
import com.keylesspalace.tusky.repository.TimelineRequestMode.NETWORK
import com.keylesspalace.tusky.util.dec
import com.keylesspalace.tusky.util.inc
import com.keylesspalace.tusky.util.trimTrailingWhitespace
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

data class Placeholder(val id: String)

typealias TimelineStatus = Either<Placeholder, Status>

enum class TimelineRequestMode {
    DISK, NETWORK, ANY
}

interface TimelineRepository {
    fun getStatuses(
        maxId: String?,
        sinceId: String?,
        sincedIdMinusOne: String?,
        limit: Int,
        requestMode: TimelineRequestMode
    ): Single<out List<TimelineStatus>>

    companion object {
        val CLEANUP_INTERVAL = TimeUnit.DAYS.toMillis(14)
    }
}

class TimelineRepositoryImpl(
    private val timelineDao: TimelineDao,
    private val mastodonApi: MastodonApi,
    private val accountManager: AccountManager,
    private val gson: Gson
) : TimelineRepository {

    init {
        this.cleanup()
    }

    override fun getStatuses(
        maxId: String?,
        sinceId: String?,
        sincedIdMinusOne: String?,
        limit: Int,
        requestMode: TimelineRequestMode
    ): Single<out List<TimelineStatus>> {
        val acc = accountManager.activeAccount ?: throw IllegalStateException()
        val accountId = acc.id

        return if (requestMode == DISK) {
            this.getStatusesFromDb(accountId, maxId, sinceId, limit)
        } else {
            getStatusesFromNetwork(maxId, sinceId, sincedIdMinusOne, limit, accountId, requestMode)
        }
    }

    private fun getStatusesFromNetwork(
        maxId: String?,
        sinceId: String?,
        sinceIdMinusOne: String?,
        limit: Int,
        accountId: Long,
        requestMode: TimelineRequestMode
    ): Single<out List<TimelineStatus>> {
        return mastodonApi.homeTimelineSingle(maxId, sinceIdMinusOne, limit + 1)
            .map { statuses ->
                this.saveStatusesToDb(accountId, statuses, maxId, sinceId)
            }
            .flatMap { statuses ->
                this.addFromDbIfNeeded(accountId, statuses, maxId, sinceId, limit, requestMode)
            }
            .onErrorResumeNext { error ->
                if (error is IOException && requestMode != NETWORK) {
                    this.getStatusesFromDb(accountId, maxId, sinceId, limit)
                } else {
                    Single.error(error)
                }
            }
    }

    private fun addFromDbIfNeeded(
        accountId: Long,
        statuses: List<Either<Placeholder, Status>>,
        maxId: String?,
        sinceId: String?,
        limit: Int,
        requestMode: TimelineRequestMode
    ): Single<List<TimelineStatus>>? {
        return if (requestMode != NETWORK && statuses.size < 2) {
            val newMaxID = if (statuses.isEmpty()) {
                maxId
            } else {
                statuses.last { it.isRight() }.asRight().id
            }
            this.getStatusesFromDb(accountId, newMaxID, sinceId, limit)
                .map { fromDb ->
                    // If it's just placeholders and less than limit (so we exhausted both
                    // db and server at this point)
                    if (fromDb.size < limit && fromDb.all { !it.isRight() }) {
                        statuses
                    } else {
                        statuses + fromDb
                    }
                }
        } else {
            Single.just(statuses)
        }
    }

    private fun getStatusesFromDb(
        accountId: Long,
        maxId: String?,
        sinceId: String?,
        limit: Int
    ): Single<out List<TimelineStatus>> {
        return timelineDao.getStatusesForAccount(accountId, maxId, sinceId, limit)
            .subscribeOn(Schedulers.io())
            .map { statuses ->
                statuses.map { it.toStatus() }
            }
    }

    private fun saveStatusesToDb(
        accountId: Long,
        statuses: List<Status>,
        maxId: String?,
        sinceId: String?
    ): List<Either<Placeholder, Status>> {
        var placeholderToInsert: Placeholder? = null

        // Look for overlap
        val resultStatuses = if (statuses.isNotEmpty() && sinceId != null) {
            val indexOfSince = statuses.indexOfLast { it.id == sinceId }
            if (indexOfSince == -1) {
                // We didn't find the status which must be there. Add a placeholder
                placeholderToInsert = Placeholder(sinceId.inc())
                statuses.mapTo(mutableListOf(), Status::lift)
                    .apply {
                        add(Either.Left(placeholderToInsert))
                    }
            } else {
                // There was an overlap. Remove all overlapped statuses. No need for a placeholder.
                statuses.mapTo(mutableListOf(), Status::lift)
                    .apply {
                        subList(indexOfSince, size).clear()
                    }
            }
        } else {
            // Just a normal case.
            statuses.map(Status::lift)
        }

        Single.fromCallable {
            if (statuses.isNotEmpty()) {
                timelineDao.deleteRange(accountId, statuses.last().id, statuses.first().id)
            }

            for (status in statuses) {
                timelineDao.insertInTransaction(
                    status.toEntity(accountId, gson),
                    status.account.toEntity(accountId, gson),
                    status.reblog?.account?.toEntity(accountId, gson)
                )
            }

            placeholderToInsert?.let {
                timelineDao.insertStatusIfNotThere(placeholderToInsert.toEntity(accountId))
            }

            // If we're loading in the bottom insert placeholder after every load
            // (for requests on next launches) but not return it.
            if (sinceId == null && statuses.isNotEmpty()) {
                timelineDao.insertStatusIfNotThere(
                    Placeholder(statuses.last().id.dec()).toEntity(accountId)
                )
            }

            // There may be placeholders which we thought could be from our TL but they are not
            if (statuses.size > 2) {
                timelineDao.removeAllPlaceholdersBetween(
                    accountId,
                    statuses.first().id,
                    statuses.last().id
                )
            } else if (placeholderToInsert == null && maxId != null && sinceId != null) {
                timelineDao.removeAllPlaceholdersBetween(accountId, maxId, sinceId)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe()

        return resultStatuses
    }

    private fun cleanup() {
        Schedulers.io().scheduleDirect {
            val olderThan = System.currentTimeMillis() - TimelineRepository.CLEANUP_INTERVAL
            timelineDao.cleanup(olderThan)
        }
    }

    private fun TimelineStatusWithAccount.toStatus(): TimelineStatus {
        if (this.status.authorServerId == null) {
            return Either.Left(Placeholder(this.status.serverId))
        }

        val attachments: ArrayList<Attachment> = gson.fromJson(
            status.attachments,
            object : TypeToken<List<Attachment>>() {}.type
        ) ?: ArrayList()
        val mentions: Array<Status.Mention> = gson.fromJson(
            status.mentions,
            Array<Status.Mention>::class.java
        ) ?: arrayOf()
        val application = gson.fromJson(status.application, Status.Application::class.java)
        val emojis: List<Emoji> = gson.fromJson(
            status.emojis,
            object : TypeToken<List<Emoji>>() {}.type
        ) ?: listOf()
        val quoteEmojis: List<Emoji> = gson.fromJson(
            status.quoteEmojis,
            object : TypeToken<List<Emoji>>() {}.type
        ) ?: listOf()
        val quotedAccountEmojis: List<Emoji> = gson.fromJson(
            status.quotedAccountEmojis,
            object : TypeToken<List<Emoji>>() {}.type
        ) ?: listOf()
        val poll: Poll? = gson.fromJson(status.poll, Poll::class.java)
        val pleroma = gson.fromJson(status.pleroma, Status.PleromaStatus::class.java)

        val reblog = status.reblogServerId?.let { id ->
            Status(
                id = id,
                url = status.url,
                uri = status.url,
                account = account.toAccount(gson),
                inReplyToId = status.inReplyToId,
                inReplyToAccountId = status.inReplyToAccountId,
                reblog = null,
                content = status.content?.parseAsHtml()?.trimTrailingWhitespace()
                    ?: SpannedString(""),
                quote = status.quote?.parseAsHtml()?.trimTrailingWhitespace()?.let { quote ->
                    val account =
                        if (status.quoteFullName != null && status.quoteUsername != null) {
                            Account(
                                "",
                                // Needed
                                status.quoteFullName!!,
                                // Needed
                                status.quoteUsername!!,
                                status.quoteFullName,
                                SpannedString(""),
                                "",
                                "",
                                "",
                                emojis = quotedAccountEmojis
                            )
                        } else {
                            null
                        }
                    Quote(
                        status.quotedStatusId,
                        status.quotedStatusUrl,
                        quote,
                        quoteEmojis,
                        account
                    )
                },
                createdAt = Date(status.createdAt),
                editedAt = status.editedAt?.let { Date(it) },
                emojis = emojis,
                reblogsCount = status.reblogsCount,
                favouritesCount = status.favouritesCount,
                reblogged = status.reblogged,
                favourited = status.favourited,
                bookmarked = status.bookmarked,
                sensitive = status.sensitive,
                spoilerText = status.spoilerText!!,
                visibility = status.visibility!!,
                attachments = attachments,
                mentions = mentions,
                application = application,
                pinned = false,
                poll = poll,
                card = null,
                pleroma = pleroma,
            )
        }
        val status = if (reblog != null) {
            Status(
                id = status.serverId,
                url = null, // no url for reblogs
                uri = null,
                account = this.reblogAccount!!.toAccount(gson),
                inReplyToId = null,
                inReplyToAccountId = null,
                reblog = reblog,
                content = SpannedString(""),
                createdAt = Date(status.createdAt), // lie but whatever?
                editedAt = null,
                emojis = listOf(),
                reblogsCount = 0,
                favouritesCount = 0,
                reblogged = false,
                favourited = false,
                bookmarked = false,
                sensitive = false,
                spoilerText = "",
                visibility = status.visibility!!,
                attachments = ArrayList(),
                mentions = arrayOf(),
                application = null,
                pinned = false,
                poll = null,
                card = null,
                pleroma = null
            )
        } else {
            Status(
                id = status.serverId,
                url = status.url,
                uri = status.url,
                account = account.toAccount(gson),
                inReplyToId = status.inReplyToId,
                inReplyToAccountId = status.inReplyToAccountId,
                reblog = null,
                content = status.content?.parseAsHtml()?.trimTrailingWhitespace()
                    ?: SpannedString(""),
                createdAt = Date(status.createdAt),
                editedAt = status.editedAt?.let { Date(it) },
                emojis = emojis,
                reblogsCount = status.reblogsCount,
                favouritesCount = status.favouritesCount,
                reblogged = status.reblogged,
                favourited = status.favourited,
                bookmarked = status.bookmarked,
                sensitive = status.sensitive,
                spoilerText = status.spoilerText!!,
                visibility = status.visibility!!,
                attachments = attachments,
                mentions = mentions,
                application = application,
                pinned = false,
                poll = poll,
                card = null,
                pleroma = pleroma
            )
        }
        return Either.Right(status)
    }
}

private val emojisListTypeToken = object : TypeToken<List<Emoji>>() {}

fun Account.toEntity(accountId: Long, gson: Gson): TimelineAccountEntity {
    return TimelineAccountEntity(
        serverId = id,
        timelineUserId = accountId,
        localUsername = localUsername,
        username = username,
        displayName = displayName.orEmpty(),
        url = url,
        avatar = avatar,
        emojis = gson.toJson(emojis),
        bot = bot
    )
}

fun TimelineAccountEntity.toAccount(gson: Gson): Account {
    return Account(
        id = serverId,
        localUsername = localUsername,
        username = username,
        displayName = displayName,
        note = SpannedString(""),
        url = url,
        avatar = avatar,
        header = "",
        locked = false,
        followingCount = 0,
        followersCount = 0,
        statusesCount = 0,
        source = null,
        bot = bot,
        emojis = gson.fromJson(this.emojis, emojisListTypeToken.type),
        fields = null,
        moved = null
    )
}

fun Placeholder.toEntity(timelineUserId: Long): TimelineStatusEntity {
    return TimelineStatusEntity(
        serverId = this.id,
        url = null,
        timelineUserId = timelineUserId,
        authorServerId = null,
        inReplyToId = null,
        inReplyToAccountId = null,
        content = null,
        createdAt = 0L,
        editedAt = null,
        emojis = null,
        reblogsCount = 0,
        favouritesCount = 0,
        reblogged = false,
        favourited = false,
        bookmarked = false,
        sensitive = false,
        spoilerText = null,
        visibility = null,
        attachments = null,
        mentions = null,
        application = null,
        reblogServerId = null,
        reblogAccountId = null,
        poll = null,
        pleroma = null,
        quote = null,
        quotedStatusId = null,
        quotedStatusUrl = null,
        quoteEmojis = null,
        quoteFullName = null,
        quoteUsername = null,
        quotedAccountEmojis = null
    )
}

fun Status.toEntity(
    timelineUserId: Long,
    gson: Gson
): TimelineStatusEntity {
    val actionable = actionableStatus
    return TimelineStatusEntity(
        serverId = this.id,
        url = (actionable.url ?: actionableStatus.uri),
        timelineUserId = timelineUserId,
        authorServerId = actionable.account.id,
        inReplyToId = actionable.inReplyToId,
        inReplyToAccountId = actionable.inReplyToAccountId,
        content = actionable.content.toHtml(),
        quote = actionable.quote?.content?.toHtml(),
        quotedStatusId = actionable.quote?.quotedStatusId,
        quotedStatusUrl = actionable.quote?.quotedStatusUrl,
        quoteEmojis = actionable.quote?.quoteEmojis?.let { gson.toJson(it) } ?: gson.toJson(listOf<Emoji>()),
        quoteFullName = actionable.quote?.account?.name,
        quoteUsername = actionable.quote?.account?.username,
        quotedAccountEmojis = actionable.quote?.account?.emojis?.let { gson.toJson(it) } ?: gson.toJson(listOf<Emoji>()),
        createdAt = actionable.createdAt.time,
        editedAt = actionable.editedAt?.time,
        emojis = actionable.emojis.let(gson::toJson),
        reblogsCount = actionable.reblogsCount,
        favouritesCount = actionable.favouritesCount,
        reblogged = actionable.reblogged,
        favourited = actionable.favourited,
        bookmarked = actionable.bookmarked,
        sensitive = actionable.sensitive,
        spoilerText = actionable.spoilerText,
        visibility = actionable.visibility,
        attachments = actionable.attachments.let(gson::toJson),
        mentions = actionable.mentions.let(gson::toJson),
        application = actionable.application.let(gson::toJson),
        reblogServerId = reblog?.id,
        reblogAccountId = reblog?.let { this.account.id },
        poll = actionable.poll.let(gson::toJson),
        pleroma = actionable.pleroma.let(gson::toJson)
    )
}

fun Status.lift(): Either<Placeholder, Status> = Either.Right(this)
