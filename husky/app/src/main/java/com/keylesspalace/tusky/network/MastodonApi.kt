/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2017  Alibek "a1batross" Omarov
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

package com.keylesspalace.tusky.network

import com.keylesspalace.tusky.components.instance.data.models.data.Instance
import com.keylesspalace.tusky.components.unifiedpush.PushNotificationResponse
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
import com.keylesspalace.tusky.entity.Poll
import com.keylesspalace.tusky.entity.Relationship
import com.keylesspalace.tusky.entity.ScheduledStatus
import com.keylesspalace.tusky.entity.SearchResult
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.entity.StatusContext
import com.keylesspalace.tusky.entity.StickerPack
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/*
 * For documentation of the Mastodon REST API see https://docs.joinmastodon.org/api/
 */
@JvmSuppressWildcards
interface MastodonApi {

    companion object {
        const val ENDPOINT_AUTHORIZE = "/oauth/authorize"
        const val DOMAIN_HEADER = "domain"
        const val PLACEHOLDER_DOMAIN = "dummy.placeholder"
    }

    @GET("/api/v1/custom_emojis")
    fun getCustomEmojis(): Single<List<Emoji>>

    @GET("api/v1/instance")
    fun getInstance(): Single<Instance>

    @GET("api/v1/instance")
    suspend fun getInstanceData(): Response<Instance>

    @GET("api/v1/filters")
    fun getFilters(): Call<List<Filter>>

    @GET("api/v1/timelines/home?with_muted=true")
    fun homeTimeline(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/timelines/home?with_muted=true")
    fun homeTimelineSingle(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Single<List<Status>>

    @GET("api/v1/timelines/public?with_muted=true")
    fun publicTimeline(
        @Query("local") local: Boolean?,
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/timelines/bubble?with_muted=true")
    fun bubbleTimeline(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/timelines/tag/{hashtag}?with_muted=true")
    fun hashtagTimeline(
        @Path("hashtag") hashtag: String,
        @Query("any[]") any: List<String>?,
        @Query("local") local: Boolean?,
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/timelines/list/{listId}?with_muted=true")
    fun listTimeline(
        @Path("listId") listId: String,
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/notifications")
    fun notifications(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?,
        @Query("exclude_types[]") excludes: Set<Notification.Type>?,
        @Query("with_muted") withMuted: Boolean?
    ): Call<List<Notification>>

    @GET("api/v1/markers")
    fun markersWithAuth(
        @Header("Authorization") auth: String,
        @Header(DOMAIN_HEADER) domain: String,
        @Query("timeline[]") timelines: List<String>
    ): Single<Map<String, Marker>>

    @GET("api/v1/notifications?with_muted=true")
    fun notificationsWithAuth(
        @Header("Authorization") auth: String,
        @Header(DOMAIN_HEADER) domain: String,
        @Query("since_id") sinceId: String?,
        @Query("include_types[]") includeTypes: List<String>?
    ): Single<List<Notification>>

    @POST("api/v1/notifications/clear")
    fun clearNotifications(): Call<ResponseBody>

    @GET("api/v1/notifications/{id}")
    fun notification(
        @Path("id") notificationId: String
    ): Call<Notification>

    @Multipart
    @POST("api/v1/media")
    fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part description: MultipartBody.Part? = null
    ): Single<Attachment>

    @FormUrlEncoded
    @PUT("api/v1/media/{mediaId}")
    fun updateMedia(
        @Path("mediaId") mediaId: String,
        @Field("description") description: String
    ): Single<Attachment>

    @POST("api/v1/statuses")
    fun createStatus(
        @Header("Authorization") auth: String,
        @Header(DOMAIN_HEADER) domain: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body status: NewStatus
    ): Call<Status>

    @GET("api/v1/statuses/{id}")
    fun status(
        @Path("id") statusId: String
    ): Call<Status>

    @GET("api/v1/statuses/{id}")
    fun statusSingle(
        @Path("id") statusId: String
    ): Single<Status>

    @GET("api/v1/statuses/{id}/context")
    fun statusContext(
        @Path("id") statusId: String
    ): Call<StatusContext>

    @GET("api/v1/statuses/{id}/reblogged_by")
    fun statusRebloggedBy(
        @Path("id") statusId: String,
        @Query("max_id") maxId: String?
    ): Single<Response<List<Account>>>

    @GET("api/v1/statuses/{id}/favourited_by")
    fun statusFavouritedBy(
        @Path("id") statusId: String,
        @Query("max_id") maxId: String?
    ): Single<Response<List<Account>>>

    @DELETE("api/v1/statuses/{id}")
    fun deleteStatus(
        @Path("id") statusId: String
    ): Single<DeletedStatus>

    @POST("api/v1/statuses/{id}/reblog")
    fun reblogStatus(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/unreblog")
    fun unreblogStatus(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/favourite")
    fun favouriteStatus(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/unfavourite")
    fun unfavouriteStatus(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/bookmark")
    fun bookmarkStatus(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/unbookmark")
    fun unbookmarkStatus(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/pin")
    fun pinStatus(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/unpin")
    fun unpinStatus(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/mute")
    fun muteConversation(
        @Path("id") statusId: String
    ): Single<Status>

    @POST("api/v1/statuses/{id}/unmute")
    fun unmuteConversation(
        @Path("id") statusId: String
    ): Single<Status>

    @GET("api/v1/scheduled_statuses")
    fun scheduledStatuses(
        @Query("limit") limit: Int? = null,
        @Query("max_id") maxId: String? = null
    ): Single<List<ScheduledStatus>>

    @DELETE("api/v1/scheduled_statuses/{id}")
    fun deleteScheduledStatus(
        @Path("id") scheduledStatusId: String
    ): Single<ResponseBody>

    @GET("api/v1/accounts/verify_credentials")
    fun accountVerifyCredentials(): Single<Account>

    @FormUrlEncoded
    @PATCH("api/v1/accounts/update_credentials")
    fun accountUpdateSource(
        @Field("source[privacy]") privacy: String?,
        @Field("source[sensitive]") sensitive: Boolean?
    ): Call<Account>

    @Multipart
    @PATCH("api/v1/accounts/update_credentials")
    suspend fun accountUpdateCredentialsData(
        @Part(value = "display_name") displayName: RequestBody?,
        @Part(value = "note") note: RequestBody?,
        @Part(value = "locked") locked: RequestBody?,
        @Part avatar: MultipartBody.Part?,
        @Part header: MultipartBody.Part?,
        @PartMap() fields_attributes: HashMap<String, RequestBody>?
    ): Response<Account>

    @Multipart
    @PATCH("api/v1/accounts/update_credentials")
    fun accountUpdateCredentials(
        @Part(value = "display_name") displayName: RequestBody?,
        @Part(value = "note") note: RequestBody?,
        @Part(value = "locked") locked: RequestBody?,
        @Part avatar: MultipartBody.Part?,
        @Part header: MultipartBody.Part?,
        @PartMap() fields_attributes: HashMap<String, RequestBody>?
    ): Call<Account>

    @GET("api/v1/accounts/search")
    fun searchAccounts(
        @Query("q") query: String,
        @Query("resolve") resolve: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("following") following: Boolean? = null
    ): Single<List<Account>>

    @GET("api/v1/accounts/{id}")
    fun account(
        @Path("id") accountId: String
    ): Single<Account>

    /**
     * Method to fetch statuses for the specified account.
     * @param accountId ID for account for which statuses will be requested
     * @param maxId Only statuses with ID less than maxID will be returned
     * @param sinceId Only statuses with ID bigger than sinceID will be returned
     * @param limit Limit returned statuses (current API limits: default - 20, max - 40)
     * @param excludeReplies only return statuses that are no replies
     * @param onlyMedia only return statuses that have media attached
     */
    @GET("api/v1/accounts/{id}/statuses?with_muted=true")
    fun accountStatuses(
        @Path("id") accountId: String,
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?,
        @Query("exclude_replies") excludeReplies: Boolean?,
        @Query("only_media") onlyMedia: Boolean?,
        @Query("pinned") pinned: Boolean?
    ): Call<List<Status>>

    @GET("api/v1/accounts/{id}/followers")
    fun accountFollowers(
        @Path("id") accountId: String,
        @Query("max_id") maxId: String?
    ): Single<Response<List<Account>>>

    @GET("api/v1/accounts/{id}/following")
    fun accountFollowing(
        @Path("id") accountId: String,
        @Query("max_id") maxId: String?
    ): Single<Response<List<Account>>>

    @FormUrlEncoded
    @POST("api/v1/accounts/{id}/follow")
    fun followAccount(
        @Path("id") accountId: String,
        @Field("reblogs") showReblogs: Boolean? = null,
        @Field("notify") notify: Boolean? = null
    ): Single<Relationship>

    @POST("api/v1/accounts/{id}/unfollow")
    fun unfollowAccount(
        @Path("id") accountId: String
    ): Single<Relationship>

    @POST("api/v1/accounts/{id}/block")
    fun blockAccount(
        @Path("id") accountId: String
    ): Single<Relationship>

    @POST("api/v1/accounts/{id}/unblock")
    fun unblockAccount(
        @Path("id") accountId: String
    ): Single<Relationship>

    @FormUrlEncoded
    @POST("api/v1/accounts/{id}/mute")
    fun muteAccount(
        @Path("id") accountId: String,
        @Field("notifications") notifications: Boolean? = null,
        @Field("duration") duration: Int? = null
    ): Single<Relationship>

    @POST("api/v1/accounts/{id}/unmute")
    fun unmuteAccount(
        @Path("id") accountId: String
    ): Single<Relationship>

    @GET("api/v1/accounts/relationships")
    fun relationships(
        @Query("id[]") accountIds: List<String>
    ): Single<List<Relationship>>

    @GET("api/v1/accounts/{id}/identity_proofs")
    fun identityProofs(
        @Path("id") accountId: String
    ): Single<List<IdentityProof>>

    @POST("api/v1/pleroma/accounts/{id}/subscribe")
    fun subscribeAccount(
        @Path("id") accountId: String
    ): Single<Relationship>

    @POST("api/v1/pleroma/accounts/{id}/unsubscribe")
    fun unsubscribeAccount(
        @Path("id") accountId: String
    ): Single<Relationship>

    @GET("api/v1/blocks")
    fun blocks(
        @Query("max_id") maxId: String?
    ): Single<Response<List<Account>>>

    @GET("api/v1/mutes")
    fun mutes(
        @Query("max_id") maxId: String?
    ): Single<Response<List<Account>>>

    @GET("api/v1/domain_blocks")
    fun domainBlocks(
        @Query("max_id") maxId: String? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("limit") limit: Int? = null
    ): Single<Response<List<String>>>

    @FormUrlEncoded
    @POST("api/v1/domain_blocks")
    fun blockDomain(
        @Field("domain") domain: String
    ): Call<Any>

    @FormUrlEncoded
    // @DELETE doesn't support fields
    @HTTP(method = "DELETE", path = "api/v1/domain_blocks", hasBody = true)
    fun unblockDomain(@Field("domain") domain: String): Call<Any>

    @GET("api/v1/favourites?with_muted=true")
    fun favourites(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/bookmarks?with_muted=true")
    fun bookmarks(
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?
    ): Call<List<Status>>

    @GET("api/v1/follow_requests")
    fun followRequests(
        @Query("max_id") maxId: String?
    ): Single<Response<List<Account>>>

    @POST("api/v1/follow_requests/{id}/authorize")
    fun authorizeFollowRequest(
        @Path("id") accountId: String
    ): Call<Relationship>

    @POST("api/v1/follow_requests/{id}/reject")
    fun rejectFollowRequest(
        @Path("id") accountId: String
    ): Call<Relationship>

    @POST("api/v1/follow_requests/{id}/authorize")
    fun authorizeFollowRequestObservable(
        @Path("id") accountId: String
    ): Single<Relationship>

    @POST("api/v1/follow_requests/{id}/reject")
    fun rejectFollowRequestObservable(
        @Path("id") accountId: String
    ): Single<Relationship>

    @FormUrlEncoded
    @POST("api/v1/apps")
    fun authenticateApp(
        @Header(DOMAIN_HEADER) domain: String,
        @Field("client_name") clientName: String,
        @Field("redirect_uris") redirectUris: String,
        @Field("scopes") scopes: String,
        @Field("website") website: String
    ): Call<AppCredentials>

    @FormUrlEncoded
    @POST("oauth/token")
    fun fetchOAuthToken(
        @Header(DOMAIN_HEADER) domain: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String
    ): Call<AccessToken>

    @GET("/api/v1/lists")
    fun getLists(): Single<List<MastoList>>

    @GET("/api/v1/lists")
    suspend fun coGetLists(): Response<List<MastoList>>

    @GET("/api/v1/accounts/{id}/lists")
    suspend fun getListsIncludesAccount(
        @Path("id") accountId: String
    ): Response<List<MastoList>>

    @FormUrlEncoded
    @POST("api/v1/lists")
    fun createList(
        @Field("title") title: String
    ): Single<MastoList>

    @FormUrlEncoded
    @PUT("api/v1/lists/{listId}")
    fun updateList(
        @Path("listId") listId: String,
        @Field("title") title: String
    ): Single<MastoList>

    @DELETE("api/v1/lists/{listId}")
    fun deleteList(
        @Path("listId") listId: String
    ): Completable

    @GET("api/v1/lists/{listId}/accounts")
    fun getAccountsInList(
        @Path("listId") listId: String,
        @Query("limit") limit: Int
    ): Single<List<Account>>

    @FormUrlEncoded
    // @DELETE doesn't support fields
    @HTTP(method = "DELETE", path = "api/v1/lists/{listId}/accounts", hasBody = true)
    suspend fun coDeleteAccountFromList(
        @Path("listId") listId: String,
        @Field("account_ids[]") accountIds: List<String>
    ): Response<Unit>

    @FormUrlEncoded
    // @DELETE doesn't support fields
    @HTTP(method = "DELETE", path = "api/v1/lists/{listId}/accounts", hasBody = true)
    fun deleteAccountFromList(
        @Path("listId") listId: String,
        @Field("account_ids[]") accountIds: List<String>
    ): Completable

    @FormUrlEncoded
    @POST("api/v1/lists/{listId}/accounts")
    suspend fun coAddAccountToList(
        @Path("listId") listId: String,
        @Field("account_ids[]") accountIds: List<String>
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v1/lists/{listId}/accounts")
    fun addAccountToList(
        @Path("listId") listId: String,
        @Field("account_ids[]") accountIds: List<String>
    ): Completable

    @GET("/api/v1/conversations")
    fun getConversations(
        @Query("max_id") maxId: String? = null,
        @Query("limit") limit: Int
    ): Call<List<Conversation>>

    @FormUrlEncoded
    @POST("api/v1/filters")
    fun createFilter(
        @Field("phrase") phrase: String,
        @Field("context[]") context: List<String>,
        @Field("irreversible") irreversible: Boolean?,
        @Field("whole_word") wholeWord: Boolean?,
        @Field("expires_in") expiresInSeconds: Int?
    ): Call<Filter>

    @FormUrlEncoded
    @PUT("api/v1/filters/{id}")
    fun updateFilter(
        @Path("id") id: String,
        @Field("phrase") phrase: String,
        @Field("context[]") context: List<String>,
        @Field("irreversible") irreversible: Boolean?,
        @Field("whole_word") wholeWord: Boolean?,
        @Field("expires_in") expiresInSeconds: Int?
    ): Call<Filter>

    @DELETE("api/v1/filters/{id}")
    fun deleteFilter(
        @Path("id") id: String
    ): Call<ResponseBody>

    @FormUrlEncoded
    @POST("api/v1/polls/{id}/votes")
    fun voteInPoll(
        @Path("id") id: String,
        @Field("choices[]") choices: List<Int>
    ): Single<Poll>

    @GET("api/v1/announcements")
    fun listAnnouncements(
        @Query("with_dismissed") withDismissed: Boolean = true
    ): Single<List<Announcement>>

    @POST("api/v1/announcements/{id}/dismiss")
    fun dismissAnnouncement(
        @Path("id") announcementId: String
    ): Single<ResponseBody>

    @PUT("api/v1/announcements/{id}/reactions/{name}")
    fun addAnnouncementReaction(
        @Path("id") announcementId: String,
        @Path("name") name: String
    ): Single<ResponseBody>

    @DELETE("api/v1/announcements/{id}/reactions/{name}")
    fun removeAnnouncementReaction(
        @Path("id") announcementId: String,
        @Path("name") name: String
    ): Single<ResponseBody>

    @FormUrlEncoded
    @POST("api/v1/reports")
    fun reportObservable(
        @Field("account_id") accountId: String,
        @Field("status_ids[]") statusIds: List<String>,
        @Field("comment") comment: String,
        @Field("forward") isNotifyRemote: Boolean?
    ): Single<ResponseBody>

    @GET("api/v1/accounts/{id}/statuses?with_muted=true")
    fun accountStatusesObservable(
        @Path("id") accountId: String,
        @Query("max_id") maxId: String?,
        @Query("since_id") sinceId: String?,
        @Query("limit") limit: Int?,
        @Query("exclude_reblogs") excludeReblogs: Boolean?
    ): Single<List<Status>>

    @GET("api/v1/statuses/{id}")
    fun statusObservable(
        @Path("id") statusId: String
    ): Single<Status>

    @GET("api/v2/search")
    fun searchObservable(
        @Query("q") query: String?,
        @Query("type") type: String? = null,
        @Query("resolve") resolve: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("following") following: Boolean? = null
    ): Single<SearchResult>

    @GET(".well-known/nodeinfo")
    fun getNodeinfoLinks(): Single<NodeInfoLinks>

    @GET
    fun getNodeinfo(@Url url: String): Single<NodeInfo>

    @PUT("api/v1/pleroma/statuses/{id}/reactions/{emoji}")
    fun reactWithEmoji(
        @Path("id") statusId: String,
        @Path("emoji") emoji: String
    ): Single<Status>

    @DELETE("api/v1/pleroma/statuses/{id}/reactions/{emoji}")
    fun unreactWithEmoji(
        @Path("id") statusId: String,
        @Path("emoji") emoji: String
    ): Single<Status>

    @GET("api/v1/pleroma/statuses/{id}/reactions/{emoji}")
    fun statusReactedBy(
        @Path("id") statusId: String,
        @Path("emoji") emoji: String
    ): Single<Response<List<EmojiReaction>>>

    // NOT AN API CALLS NOT AN API CALLS NOT AN API CALLS NOT AN API CALLS
    // just for testing and because puniko asked me
    @GET("static/stickers.json")
    fun getStickers(): Single<Map<String, String>>

    @GET
    fun getStickerPack(
        @Url path: String
    ): Single<Response<StickerPack>>
    // NOT AN API CALLS NOT AN API CALLS NOT AN API CALLS NOT AN API CALLS

    @POST("api/v1/pleroma/chats/{id}/messages/{message_id}/read")
    fun markChatMessageAsRead(
        @Path("id") chatId: String,
        @Path("message_id") messageId: String
    ): Single<ChatMessage>

    @DELETE("api/v1/pleroma/chats/{id}/messages/{message_id}")
    fun deleteChatMessage(
        @Path("id") chatId: String,
        @Path("message_id") messageId: String
    ): Single<ChatMessage>

    @GET("api/v2/pleroma/chats")
    fun getChats(
        @Query("max_id") maxId: String?,
        @Query("min_id") minId: String?,
        @Query("since_id") sinceId: String?,
        @Query("offset") offset: Int?,
        @Query("limit") limit: Int?
    ): Single<List<Chat>>

    @GET("api/v1/pleroma/chats/{id}/messages")
    fun getChatMessages(
        @Path("id") chatId: String,
        @Query("max_id") maxId: String?,
        @Query("min_id") minId: String?,
        @Query("since_id") sinceId: String?,
        @Query("offset") offset: Int?,
        @Query("limit") limit: Int?
    ): Single<List<ChatMessage>>

    @POST("api/v1/pleroma/chats/{id}/messages")
    fun createChatMessage(
        @Header("Authorization") auth: String,
        @Header(DOMAIN_HEADER) domain: String,
        @Path("id") chatId: String,
        @Body chatMessage: NewChatMessage
    ): Call<ChatMessage>

    @FormUrlEncoded
    @POST("api/v1/pleroma/chats/{id}/read")
    fun markChatAsRead(
        @Path("id") chatId: String,
        @Field("last_read_id") lastReadId: String? = null
    ): Single<Chat>

    @POST("api/v1/pleroma/chats/by-account-id/{id}")
    fun createChat(
        @Path("id") accountId: String
    ): Single<Chat>

    @GET("api/v1/pleroma/chats/{id}")
    fun getChat(
        @Path("id") chatId: String
    ): Single<Chat>

    @FormUrlEncoded
    @POST("api/v1/accounts/{id}/note")
    fun updateAccountNote(
        @Path("id") accountId: String,
        @Field("comment") note: String
    ): Single<Relationship>

    @FormUrlEncoded
    @POST("api/v1/push/subscription")
    suspend fun subscribePushNotifications(
        @Header("Authorization") authToken: String?,
        @Header(DOMAIN_HEADER) instanceDomain: String?,
        @Field("subscription[endpoint]") unifiedPushEndpoint: String?,
        @Field("subscription[keys][p256dh]") p256dhPubKey: String?,
        @Field("subscription[keys][auth]") authKey: String?,
        @FieldMap pushData: Map<String, Boolean>?
    ): Response<PushNotificationResponse>
}
