package com.keylesspalace.tusky.network

import com.keylesspalace.tusky.components.instance.Instance
import com.keylesspalace.tusky.entity.AccessToken
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.Announcement
import com.keylesspalace.tusky.entity.AppCredentials
import com.keylesspalace.tusky.entity.Attachment
import com.keylesspalace.tusky.entity.Chat
import com.keylesspalace.tusky.entity.ChatMessage
import com.keylesspalace.tusky.entity.Conversation
import com.keylesspalace.tusky.entity.DeletedStatus
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.entity.EmojiReaction
import com.keylesspalace.tusky.entity.Filter
import com.keylesspalace.tusky.entity.IdentityProof
import com.keylesspalace.tusky.entity.Marker
import com.keylesspalace.tusky.entity.MastoList
import com.keylesspalace.tusky.entity.NewChatMessage
import com.keylesspalace.tusky.entity.NewStatus
import com.keylesspalace.tusky.entity.NodeInfo
import com.keylesspalace.tusky.entity.NodeInfoLinks
import com.keylesspalace.tusky.entity.Notification
import com.keylesspalace.tusky.entity.Notification.Type
import com.keylesspalace.tusky.entity.Poll
import com.keylesspalace.tusky.entity.Relationship
import com.keylesspalace.tusky.entity.ScheduledStatus
import com.keylesspalace.tusky.entity.SearchResult
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.entity.StatusContext
import com.keylesspalace.tusky.entity.StickerPack
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.MultipartBody.Part
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit

class MastodonService(private val retrofit: Retrofit) : MastodonApi {

    private val api by lazy { retrofit.create(MastodonApi::class.java) }

    override fun getLists(): Single<List<MastoList>> {
        return api.getLists()
    }

    override fun getCustomEmojis(): Single<List<Emoji>> {
        return api.getCustomEmojis()
    }

    override fun getInstance(): Single<Instance> {
        return api.getInstance()
    }

    override suspend fun getInstanceData(): Response<Instance> {
        return api.getInstanceData()
    }

    override fun getFilters(): Call<List<Filter>> {
        return api.getFilters()
    }

    override fun homeTimeline(maxId: String?, sinceId: String?, limit: Int?): Call<List<Status>> {
        return api.homeTimeline(maxId, sinceId, limit)
    }

    override fun homeTimelineSingle(
        maxId: String?,
        sinceId: String?,
        limit: Int?
    ): Single<List<Status>> {
        return api.homeTimelineSingle(maxId, sinceId, limit)
    }

    override fun publicTimeline(
        local: Boolean?,
        maxId: String?,
        sinceId: String?,
        limit: Int?
    ): Call<List<Status>> {
        return api.publicTimeline(local, maxId, sinceId, limit)
    }

    override fun bubbleTimeline(maxId: String?, sinceId: String?, limit: Int?): Call<List<Status>> {
        return api.bubbleTimeline(maxId, sinceId, limit)
    }

    override fun hashtagTimeline(
        hashtag: String,
        any: List<String>?,
        local: Boolean?,
        maxId: String?,
        sinceId: String?,
        limit: Int?
    ): Call<List<Status>> {
        return api.hashtagTimeline(hashtag, any, local, maxId, sinceId, limit)
    }

    override fun listTimeline(
        listId: String,
        maxId: String?,
        sinceId: String?,
        limit: Int?
    ): Call<List<Status>> {
        return api.listTimeline(listId, maxId, sinceId, limit)
    }

    override fun notifications(
        maxId: String?,
        sinceId: String?,
        limit: Int?,
        excludes: Set<Type>?,
        withMuted: Boolean?
    ): Call<List<Notification>> {
        return api.notifications(maxId, sinceId, limit, excludes, withMuted)
    }

    override fun markersWithAuth(
        auth: String,
        domain: String,
        timelines: List<String>
    ): Single<Map<String, Marker>> {
        return api.markersWithAuth(auth, domain, timelines)
    }

    override fun notificationsWithAuth(
        auth: String,
        domain: String,
        sinceId: String?,
        includeTypes: List<String>?
    ): Single<List<Notification>> {
        return api.notificationsWithAuth(auth, domain, sinceId, includeTypes)
    }

    override fun clearNotifications(): Call<ResponseBody> {
        return api.clearNotifications()
    }

    override fun notification(notificationId: String): Call<Notification> {
        return api.notification(notificationId)
    }

    override fun uploadMedia(file: Part, description: Part?): Single<Attachment> {
        return api.uploadMedia(file, description)
    }

    override fun updateMedia(mediaId: String, description: String): Single<Attachment> {
        return api.updateMedia(mediaId, description)
    }

    override fun createStatus(
        auth: String,
        domain: String,
        idempotencyKey: String,
        status: NewStatus
    ): Call<Status> {
        return api.createStatus(auth, domain, idempotencyKey, status)
    }

    override fun status(statusId: String): Call<Status> {
        return api.status(statusId)
    }

    override fun statusSingle(statusId: String): Single<Status> {
        return api.statusSingle(statusId)
    }

    override fun statusContext(statusId: String): Call<StatusContext> {
        return api.statusContext(statusId)
    }

    override fun statusRebloggedBy(
        statusId: String,
        maxId: String?
    ): Single<Response<List<Account>>> {
        return api.statusRebloggedBy(statusId, maxId)
    }

    override fun statusFavouritedBy(
        statusId: String,
        maxId: String?
    ): Single<Response<List<Account>>> {
        return api.statusFavouritedBy(statusId, maxId)
    }

    override fun deleteStatus(statusId: String): Single<DeletedStatus> {
        return api.deleteStatus(statusId)
    }

    override fun reblogStatus(statusId: String): Single<Status> {
        return api.reblogStatus(statusId)
    }

    override fun unreblogStatus(statusId: String): Single<Status> {
        return api.unreblogStatus(statusId)
    }

    override fun favouriteStatus(statusId: String): Single<Status> {
        return api.favouriteStatus(statusId)
    }

    override fun unfavouriteStatus(statusId: String): Single<Status> {
        return api.unfavouriteStatus(statusId)
    }

    override fun bookmarkStatus(statusId: String): Single<Status> {
        return api.bookmarkStatus(statusId)
    }

    override fun unbookmarkStatus(statusId: String): Single<Status> {
        return api.unbookmarkStatus(statusId)
    }

    override fun pinStatus(statusId: String): Single<Status> {
        return api.pinStatus(statusId)
    }

    override fun unpinStatus(statusId: String): Single<Status> {
        return api.unpinStatus(statusId)
    }

    override fun muteConversation(statusId: String): Single<Status> {
        return api.muteConversation(statusId)
    }

    override fun unmuteConversation(statusId: String): Single<Status> {
        return api.unmuteConversation(statusId)
    }

    override fun scheduledStatuses(limit: Int?, maxId: String?): Single<List<ScheduledStatus>> {
        return api.scheduledStatuses(limit, maxId)
    }

    override fun deleteScheduledStatus(scheduledStatusId: String): Single<ResponseBody> {
        return api.deleteScheduledStatus(scheduledStatusId)
    }

    override fun accountVerifyCredentials(): Single<Account> {
        return api.accountVerifyCredentials()
    }

    override fun accountUpdateSource(privacy: String?, sensitive: Boolean?): Call<Account> {
        return api.accountUpdateSource(privacy, sensitive)
    }

    override fun accountUpdateCredentials(
        displayName: RequestBody?,
        note: RequestBody?,
        locked: RequestBody?,
        avatar: Part?,
        header: Part?,
        fieldName0: RequestBody?,
        fieldValue0: RequestBody?,
        fieldName1: RequestBody?,
        fieldValue1: RequestBody?,
        fieldName2: RequestBody?,
        fieldValue2: RequestBody?,
        fieldName3: RequestBody?,
        fieldValue3: RequestBody?
    ): Call<Account> {
        return api.accountUpdateCredentials(
            displayName,
            note,
            locked,
            avatar,
            header,
            fieldName0,
            fieldValue0,
            fieldName1,
            fieldValue1,
            fieldName2,
            fieldValue2,
            fieldName3,
            fieldValue3
        )
    }

    override fun searchAccounts(
        query: String,
        resolve: Boolean?,
        limit: Int?,
        following: Boolean?
    ): Single<List<Account>> {
        return api.searchAccounts(query, resolve, limit, following)
    }

    override fun account(accountId: String): Single<Account> {
        return api.account(accountId)
    }

    override fun accountStatuses(
        accountId: String,
        maxId: String?,
        sinceId: String?,
        limit: Int?,
        excludeReplies: Boolean?,
        onlyMedia: Boolean?,
        pinned: Boolean?
    ): Call<List<Status>> {
        return api.accountStatuses(
            accountId,
            maxId,
            sinceId,
            limit,
            excludeReplies,
            onlyMedia,
            pinned
        )
    }

    override fun accountFollowers(
        accountId: String,
        maxId: String?
    ): Single<Response<List<Account>>> {
        return api.accountFollowers(accountId, maxId)
    }

    override fun accountFollowing(
        accountId: String,
        maxId: String?
    ): Single<Response<List<Account>>> {
        return api.accountFollowing(accountId, maxId)
    }

    override fun followAccount(
        accountId: String,
        showReblogs: Boolean?,
        notify: Boolean?
    ): Single<Relationship> {
        return api.followAccount(accountId, showReblogs, notify)
    }

    override fun unfollowAccount(accountId: String): Single<Relationship> {
        return api.unfollowAccount(accountId)
    }

    override fun blockAccount(accountId: String): Single<Relationship> {
        return api.blockAccount(accountId)
    }

    override fun unblockAccount(accountId: String): Single<Relationship> {
        return api.unblockAccount(accountId)
    }

    override fun muteAccount(
        accountId: String,
        notifications: Boolean?,
        duration: Int?
    ): Single<Relationship> {
        return api.muteAccount(accountId, notifications, duration)
    }

    override fun unmuteAccount(accountId: String): Single<Relationship> {
        return api.unmuteAccount(accountId)
    }

    override fun relationships(accountIds: List<String>): Single<List<Relationship>> {
        return api.relationships(accountIds)
    }

    override fun identityProofs(accountId: String): Single<List<IdentityProof>> {
        return api.identityProofs(accountId)
    }

    override fun subscribeAccount(accountId: String): Single<Relationship> {
        return api.subscribeAccount(accountId)
    }

    override fun unsubscribeAccount(accountId: String): Single<Relationship> {
        return api.unsubscribeAccount(accountId)
    }

    override fun blocks(maxId: String?): Single<Response<List<Account>>> {
        return api.blocks(maxId)
    }

    override fun mutes(maxId: String?): Single<Response<List<Account>>> {
        return api.mutes(maxId)
    }

    override fun domainBlocks(
        maxId: String?,
        sinceId: String?,
        limit: Int?
    ): Single<Response<List<String>>> {
        return api.domainBlocks(maxId, sinceId, limit)
    }

    override fun blockDomain(domain: String): Call<Any> {
        return api.blockDomain(domain)
    }

    override fun unblockDomain(domain: String): Call<Any> {
        return api.unblockDomain(domain)
    }

    override fun favourites(maxId: String?, sinceId: String?, limit: Int?): Call<List<Status>> {
        return api.favourites(maxId, sinceId, limit)
    }

    override fun bookmarks(maxId: String?, sinceId: String?, limit: Int?): Call<List<Status>> {
        return api.bookmarks(maxId, sinceId, limit)
    }

    override fun followRequests(maxId: String?): Single<Response<List<Account>>> {
        return api.followRequests(maxId)
    }

    override fun authorizeFollowRequest(accountId: String): Call<Relationship> {
        return api.authorizeFollowRequest(accountId)
    }

    override fun rejectFollowRequest(accountId: String): Call<Relationship> {
        return api.rejectFollowRequest(accountId)
    }

    override fun authorizeFollowRequestObservable(accountId: String): Single<Relationship> {
        return api.authorizeFollowRequestObservable(accountId)
    }

    override fun rejectFollowRequestObservable(accountId: String): Single<Relationship> {
        return api.rejectFollowRequestObservable(accountId)
    }

    override fun authenticateApp(
        domain: String,
        clientName: String,
        redirectUris: String,
        scopes: String,
        website: String
    ): Call<AppCredentials> {
        return api.authenticateApp(domain, clientName, redirectUris, scopes, website)
    }

    override fun fetchOAuthToken(
        domain: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String,
        code: String,
        grantType: String
    ): Call<AccessToken> {
        return api.fetchOAuthToken(domain, clientId, clientSecret, redirectUri, code, grantType)
    }

    override fun createList(title: String): Single<MastoList> {
        return api.createList(title)
    }

    override fun updateList(listId: String, title: String): Single<MastoList> {
        return api.updateList(listId, title)
    }

    override fun deleteList(listId: String): Completable {
        return api.deleteList(listId)
    }

    override fun getAccountsInList(listId: String, limit: Int): Single<List<Account>> {
        return api.getAccountsInList(listId, limit)
    }

    override fun deleteAccountFromList(listId: String, accountIds: List<String>): Completable {
        return api.deleteAccountFromList(listId, accountIds)
    }

    override fun addCountToList(listId: String, accountIds: List<String>): Completable {
        return api.addCountToList(listId, accountIds)
    }

    override fun getConversations(maxId: String?, limit: Int): Call<List<Conversation>> {
        return api.getConversations(maxId, limit)
    }

    override fun createFilter(
        phrase: String,
        context: List<String>,
        irreversible: Boolean?,
        wholeWord: Boolean?,
        expiresInSeconds: Int?
    ): Call<Filter> {
        return api.createFilter(phrase, context, irreversible, wholeWord, expiresInSeconds)
    }

    override fun updateFilter(
        id: String,
        phrase: String,
        context: List<String>,
        irreversible: Boolean?,
        wholeWord: Boolean?,
        expiresInSeconds: Int?
    ): Call<Filter> {
        return api.updateFilter(id, phrase, context, irreversible, wholeWord, expiresInSeconds)
    }

    override fun deleteFilter(id: String): Call<ResponseBody> {
        return api.deleteFilter(id)
    }

    override fun voteInPoll(id: String, choices: List<Int>): Single<Poll> {
        return api.voteInPoll(id, choices)
    }

    override fun listAnnouncements(withDismissed: Boolean): Single<List<Announcement>> {
        return api.listAnnouncements(withDismissed)
    }

    override fun dismissAnnouncement(announcementId: String): Single<ResponseBody> {
        return api.dismissAnnouncement(announcementId)
    }

    override fun addAnnouncementReaction(
        announcementId: String,
        name: String
    ): Single<ResponseBody> {
        return api.addAnnouncementReaction(announcementId, name)
    }

    override fun removeAnnouncementReaction(
        announcementId: String,
        name: String
    ): Single<ResponseBody> {
        return api.removeAnnouncementReaction(announcementId, name)
    }

    override fun reportObservable(
        accountId: String,
        statusIds: List<String>,
        comment: String,
        isNotifyRemote: Boolean?
    ): Single<ResponseBody> {
        return api.reportObservable(accountId, statusIds, comment, isNotifyRemote)
    }

    override fun accountStatusesObservable(
        accountId: String,
        maxId: String?,
        sinceId: String?,
        limit: Int?,
        excludeReblogs: Boolean?
    ): Single<List<Status>> {
        return api.accountStatusesObservable(accountId, maxId, sinceId, limit, excludeReblogs)
    }

    override fun statusObservable(statusId: String): Single<Status> {
        return api.statusObservable(statusId)
    }

    override fun searchObservable(
        query: String?,
        type: String?,
        resolve: Boolean?,
        limit: Int?,
        offset: Int?,
        following: Boolean?
    ): Single<SearchResult> {
        return api.searchObservable(query, type, resolve, limit, offset, following)
    }

    override fun getNodeinfoLinks(): Single<NodeInfoLinks> {
        return api.getNodeinfoLinks()
    }

    override fun getNodeinfo(url: String): Single<NodeInfo> {
        return api.getNodeinfo(url)
    }

    override fun reactWithEmoji(statusId: String, emoji: String): Single<Status> {
        return api.reactWithEmoji(statusId, emoji)
    }

    override fun unreactWithEmoji(statusId: String, emoji: String): Single<Status> {
        return api.unreactWithEmoji(statusId, emoji)
    }

    override fun statusReactedBy(
        statusId: String,
        emoji: String
    ): Single<Response<List<EmojiReaction>>> {
        return api.statusReactedBy(statusId, emoji)
    }

    override fun getStickers(): Single<Map<String, String>> {
        return api.getStickers()
    }

    override fun getStickerPack(path: String): Single<Response<StickerPack>> {
        return api.getStickerPack(path)
    }

    override fun markChatMessageAsRead(chatId: String, messageId: String): Single<ChatMessage> {
        return api.markChatMessageAsRead(chatId, messageId)
    }

    override fun deleteChatMessage(chatId: String, messageId: String): Single<ChatMessage> {
        return api.deleteChatMessage(chatId, messageId)
    }

    override fun getChats(
        maxId: String?,
        minId: String?,
        sinceId: String?,
        offset: Int?,
        limit: Int?
    ): Single<List<Chat>> {
        return api.getChats(maxId, minId, sinceId, offset, limit)
    }

    override fun getChatMessages(
        chatId: String,
        maxId: String?,
        minId: String?,
        sinceId: String?,
        offset: Int?,
        limit: Int?
    ): Single<List<ChatMessage>> {
        return api.getChatMessages(chatId, maxId, minId, sinceId, offset, limit)
    }

    override fun createChatMessage(
        auth: String,
        domain: String,
        chatId: String,
        chatMessage: NewChatMessage
    ): Call<ChatMessage> {
        return api.createChatMessage(auth, domain, chatId, chatMessage)
    }

    override fun markChatAsRead(chatId: String, lastReadId: String?): Single<Chat> {
        return api.markChatAsRead(chatId, lastReadId)
    }

    override fun createChat(accountId: String): Single<Chat> {
        return api.createChat(accountId)
    }

    override fun getChat(chatId: String): Single<Chat> {
        return api.getChat(chatId)
    }

    override fun updateAccountNote(accountId: String, note: String): Single<Relationship> {
        return api.updateAccountNote(accountId, note)
    }
}
