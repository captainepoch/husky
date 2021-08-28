package com.keylesspalace.tusky.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.arch.core.util.Function
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import at.connyduck.sparkbutton.helpers.Utils
import com.keylesspalace.tusky.BottomSheetActivity
import com.keylesspalace.tusky.PostLookupFallbackBehavior
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.adapter.ChatsAdapter
import com.keylesspalace.tusky.adapter.StatusBaseViewHolder
import com.keylesspalace.tusky.adapter.TimelineAdapter
import com.keylesspalace.tusky.appstore.*
import com.keylesspalace.tusky.components.chat.ChatActivity
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.di.Injectable
import com.keylesspalace.tusky.entity.Chat
import com.keylesspalace.tusky.entity.ChatMessage
import com.keylesspalace.tusky.entity.NewChatMessage
import com.keylesspalace.tusky.interfaces.ActionButtonActivity
import com.keylesspalace.tusky.interfaces.ChatActionListener
import com.keylesspalace.tusky.interfaces.RefreshableFragment
import com.keylesspalace.tusky.interfaces.ReselectableFragment
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.network.TimelineCases
import com.keylesspalace.tusky.repository.*
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.*
import com.keylesspalace.tusky.util.Either.Left
import com.keylesspalace.tusky.view.EndlessOnScrollListener
import com.keylesspalace.tusky.viewdata.ChatViewData
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_timeline.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChatsFragment : BaseFragment(), Injectable, RefreshableFragment, ReselectableFragment, ChatActionListener, OnRefreshListener {
    private val TAG = "ChatsF" // logging tag
    private val LOAD_AT_ONCE = 30
    private val BROKEN_PAGINATION_IN_BACKEND = true // break pagination until it's not fixed in plemora


    @Inject
    lateinit var eventHub: EventHub
    @Inject
    lateinit var api: MastodonApi
    @Inject
    lateinit var accountManager: AccountManager
    @Inject
    lateinit var chatRepo: ChatRepository
    @Inject
    lateinit var timelineCases: TimelineCases

    lateinit var adapter: ChatsAdapter

    lateinit var layoutManager: LinearLayoutManager

    private lateinit var scrollListener: EndlessOnScrollListener

    private lateinit var bottomSheetActivity: BottomSheetActivity
    private var hideFab = false
    private var bottomLoading = false

    private var eventRegistered = false
    private var isSwipeToRefreshEnabled = true
    private var isNeedRefresh = false
    private var didLoadEverythingBottom = false
    private var initialUpdateFailed = false

    private enum class FetchEnd {
        TOP, BOTTOM, MIDDLE
    }

    private val chats = PairedList<ChatStatus, ChatViewData?>(Function<ChatStatus, ChatViewData?> {input ->
        input.asRightOrNull()?.let(ViewDataUtils::chatToViewData) ?:
            ChatViewData.Placeholder(input.asLeft().id, false)
    })

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            if (isAdded) {
                Log.d(TAG, "onInserted");
                adapter.notifyItemRangeInserted(position, count)
                // scroll up when new items at the top are loaded while being in the first position
                // https://github.com/tuskyapp/Tusky/pull/1905#issuecomment-677819724
                if (position == 0 && context != null && adapter.itemCount != count) {
                    if (isSwipeToRefreshEnabled)
                        recyclerView.scrollBy(0, Utils.dpToPx(context!!, -30));
                    else
                        recyclerView.scrollToPosition(0);
                }
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            Log.d(TAG, "onRemoved");
            adapter.notifyItemRangeRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            Log.d(TAG, "onMoved");
            adapter.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            Log.d(TAG, "onChanged");
            adapter.notifyItemRangeChanged(position, count, payload)
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<ChatViewData>() {
        override fun areItemsTheSame(oldItem: ChatViewData, newItem: ChatViewData): Boolean {
            return oldItem.getViewDataId() == newItem.getViewDataId()
        }

        override fun areContentsTheSame(oldItem: ChatViewData, newItem: ChatViewData): Boolean {
            return false // Items are different always. It allows to refresh timestamp on every view holder update
        }

        override fun getChangePayload(oldItem: ChatViewData, newItem: ChatViewData): Any? {
            return if (oldItem.deepEquals(newItem)) {
                //If items are equal - update timestamp only
                listOf(StatusBaseViewHolder.Key.KEY_CREATED)
            } else  // If items are different - update a whole view holder
                null
        }
    }

    private val differ = AsyncListDiffer(listUpdateCallback,
            AsyncDifferConfig.Builder(diffCallback).build())

    private val dataSource = object : TimelineAdapter.AdapterDataSource<ChatViewData> {
        override fun getItemCount(): Int {
            return differ.currentList.size
        }

        override fun getItemAt(pos: Int): ChatViewData {
            return differ.currentList[pos]
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)

        val statusDisplayOptions = StatusDisplayOptions(
                animateAvatars = preferences.getBoolean(PrefKeys.ANIMATE_GIF_AVATARS, false),
                mediaPreviewEnabled = accountManager.activeAccount!!.mediaPreviewEnabled,
                useAbsoluteTime = preferences.getBoolean(PrefKeys.ABSOLUTE_TIME_VIEW, false),
                showBotOverlay = preferences.getBoolean(PrefKeys.SHOW_BOT_OVERLAY, true),
                useBlurhash = false,
                cardViewMode = CardViewMode.NONE,
                confirmReblogs = false,
                renderStatusAsMention = false,
                hideStats = false
        )

        adapter = ChatsAdapter(dataSource, statusDisplayOptions, this, accountManager.activeAccount!!.accountId)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bottomSheetActivity = if (context is BottomSheetActivity) {
            context
        } else {
            throw IllegalStateException("Fragment must be attached to a BottomSheetActivity!")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timeline, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout.isEnabled = isSwipeToRefreshEnabled
        swipeRefreshLayout.setOnRefreshListener(this)
        swipeRefreshLayout.setColorSchemeResources(R.color.tusky_blue)

        // TODO: a11y
        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(view.context)
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        if (chats.isEmpty()) {
            progressBar.visibility = View.VISIBLE
            bottomLoading = true
            sendInitialRequest()
        } else {
            progressBar.visibility = View.GONE
            if (isNeedRefresh) onRefresh()
        }
    }
    private fun sendInitialRequest() {
        // debug
        // sendFetchChatsRequest(null, null, null, FetchEnd.BOTTOM, -1)
        tryCache()
    }

    private fun tryCache() {
        // Request timeline from disk to make it quick, then replace it with timeline from
        // the server to update it
        chatRepo.getChats(null, null, null, LOAD_AT_ONCE, TimelineRequestMode.DISK)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(this, Lifecycle.Event.ON_DESTROY)
                .subscribe { newChats ->
                    if (newChats.size > 1) {
                        val mutableChats = newChats.toMutableList()
                        mutableChats.removeAll { it.isLeft() }

                        chats.clear()
                        chats.addAll(mutableChats)

                        updateAdapter()
                        progressBar.visibility = View.GONE
                    }
                    updateCurrent()
                    loadAbove()
                }
    }

    private fun updateCurrent() {
        if (!BROKEN_PAGINATION_IN_BACKEND && chats.isEmpty()) {
            return
        }

        val topId  = chats.firstOrNull { it.isRight() }?.asRight()?.id
        chatRepo.getChats(topId, null, null, LOAD_AT_ONCE, TimelineRequestMode.NETWORK)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(this, Lifecycle.Event.ON_DESTROY)
                .subscribe({ newChats ->
                    initialUpdateFailed = false
                    // When cached timeline is too old, we would replace it with nothing
                    if (newChats.isNotEmpty()) {
                        // clear old cached statuses
                        if(BROKEN_PAGINATION_IN_BACKEND) {
                            chats.clear()
                        } else {
                            chats.removeAll {
                                if(it.isLeft()) {
                                    val p = it.asLeft()
                                    p.id.length < topId!!.length || p.id < topId
                                } else {
                                    val c = it.asRight()
                                    c.id.length < topId!!.length || c.id < topId
                                }
                            }
                        }
                        chats.addAll(newChats)
                        updateAdapter()
                    }
                    bottomLoading = false
                    // Indicate that we are not loading anymore
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                }, {
                    initialUpdateFailed = true
                    // Indicate that we are not loading anymore
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                })
    }

    private fun showNothing() {
        statusView.visibility = View.VISIBLE
        statusView.setup(R.drawable.elephant_friend_empty, R.string.message_empty, null)
    }

    private fun removeAllByAccountId(accountId: String) {
        chats.removeAll {
            val chat = it.asRightOrNull()
            chat != null && chat.account.id == accountId
        }
        updateAdapter()
    }

    private fun removeAllByInstance(instance: String) {
        chats.removeAll {
            val chat = it.asRightOrNull()
            chat != null && LinkHelper.getDomain(chat.account.url) == instance
        }
        updateAdapter()
    }

    private fun deleteChatById(id: String) {
        val iterator = chats.iterator()
        while(iterator.hasNext()) {
            val chat = iterator.next().asRightOrNull()
            if(chat != null && chat.id == id) {
                iterator.remove()
                updateAdapter()
                break
            }
        }

        if(chats.isEmpty()) {
            showNothing()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        /* This is delayed until onActivityCreated solely because MainActivity.composeButton isn't
         * guaranteed to be set until then. */
        /* Use a modified scroll listener that both loads more statuses as it goes, and hides
         * the follow button on down-scroll. */
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        hideFab = preferences.getBoolean("fabHide", false)
        scrollListener = object : EndlessOnScrollListener(layoutManager) {
            override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(view, dx, dy)
                val activity = activity as ActionButtonActivity?
                val composeButton = activity!!.actionButton
                if (composeButton != null) {
                    if (hideFab) {
                        if (dy > 0 && composeButton.isShown) {
                            composeButton.hide() // hides the button if we're scrolling down
                        } else if (dy < 0 && !composeButton.isShown) {
                            composeButton.show() // shows it if we are scrolling up
                        }
                    } else if (!composeButton.isShown) {
                        composeButton.show()
                    }
                }
            }

            override fun onLoadMore(totalItemsCount: Int, view: RecyclerView) {
                if(!BROKEN_PAGINATION_IN_BACKEND)
                    this@ChatsFragment.onLoadMore()
            }
        }
        recyclerView.addOnScrollListener(scrollListener)
        if (!eventRegistered) {
            eventHub.events
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(this, Lifecycle.Event.ON_DESTROY)
                    .subscribe { event: Event? ->
                        when(event) {
                            is BlockEvent -> removeAllByAccountId(event.accountId)
                            is MuteEvent -> removeAllByAccountId(event.accountId)
                            is DomainMuteEvent -> removeAllByInstance(event.instance)
                            is StatusDeletedEvent -> deleteChatById(event.statusId)
                            is PreferenceChangedEvent -> onPreferenceChanged(event.preferenceKey)
                            is ChatMessageReceivedEvent -> onRefresh() // TODO: proper update
                        }
                    }
            eventRegistered = true
        }
    }

    /*
    private fun onChatMessageReceived(msg: ChatMessage) {
        val pos = findChatPosition(msg.chatId)
        if(pos == -1) {

            return
        }

        val oldChat = chats[pos].asRight()
        val newChat = Chat(oldChat.account, oldChat.id, oldChat.unread + 1, msg, msg.createdAt)
        val newViewData = ViewDataUtils.chatToViewData(newChat)

        chats.removeAt(pos)
        chats.add(pos, newChat.lift())
        chats.sortByDescending {
            if(it.isLeft()) Date(Long.MIN_VALUE)
            else it.asRight().updatedAt
        }

        updateAdapter()
    }
    */

    private fun onPreferenceChanged(key: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        when (key) {
            "fabHide" -> {
                hideFab = sharedPreferences.getBoolean("fabHide", false)
            }
        }
    }

    override fun onRefresh() {
        if (isSwipeToRefreshEnabled)
            swipeRefreshLayout.isEnabled = true

        statusView.visibility = View.GONE
        isNeedRefresh = false

        if (this.initialUpdateFailed) {
            updateCurrent()
        }
        loadAbove()
    }

    private fun loadAbove() {
        if(BROKEN_PAGINATION_IN_BACKEND) {
            updateCurrent()
            return
        }

        var firstOrNull: String? = null
        var secondOrNull: String? = null
        for (i in chats.indices) {
            val chat = chats[i]
            if (chat.isRight()) {
                firstOrNull = chat.asRight().id
                if (i + 1 < chats.size && chats[i + 1].isRight()) {
                    secondOrNull = chats[i + 1].asRight().id
                }
                break
            }
        }
        if (firstOrNull != null) {
            sendFetchChatsRequest(null, firstOrNull, secondOrNull, FetchEnd.TOP, -1)
        } else {
            sendFetchChatsRequest(null, null, null, FetchEnd.BOTTOM, -1)
        }
    }

    private fun onLoadMore() {
        if (BROKEN_PAGINATION_IN_BACKEND)
            updateCurrent()
            return

        if (didLoadEverythingBottom || bottomLoading) {
            return
        }
        if (chats.isEmpty()) {
            sendInitialRequest()
            return
        }
        bottomLoading = true
        val last = chats.last()
        val placeholder: Placeholder
        if (last.isRight()) {
            val placeholderId = last.asRight().id.dec()
            placeholder = Placeholder(placeholderId)
            chats.add(Left(placeholder))
        } else {
            placeholder = last.asLeft()
        }
        chats.setPairedItem(chats.size - 1,
                ChatViewData.Placeholder(placeholder.id, true))
        updateAdapter()
        val bottomId = chats.findLast { it.isRight() }?.let { it.asRight().id }
        sendFetchChatsRequest(bottomId, null, null, FetchEnd.BOTTOM, -1)
    }


    private fun sendFetchChatsRequest(maxId: String?, sinceId: String?,
                                      sinceIdMinusOne: String?,
                                      fetchEnd: FetchEnd, pos: Int) {
        if (isAdded
                && (fetchEnd == FetchEnd.TOP || fetchEnd == FetchEnd.BOTTOM && maxId == null && progressBar.visibility != View.VISIBLE)
                && !isSwipeToRefreshEnabled)
            topProgressBar.show()
        // allow getting old statuses/fallbacks for network only for for bottom loading
        val mode = if (fetchEnd == FetchEnd.BOTTOM) {
            TimelineRequestMode.ANY
        } else {
            TimelineRequestMode.NETWORK
        }
        chatRepo.getChats(maxId, sinceId, sinceIdMinusOne, LOAD_AT_ONCE, mode)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(this, Lifecycle.Event.ON_DESTROY)
                .subscribe( { result -> onFetchTimelineSuccess(result.toMutableList(), fetchEnd, pos) },
                        { onFetchTimelineFailure(Exception(it), fetchEnd, pos) })
    }

    private fun updateChats(newChats: MutableList<ChatStatus>, fullFetch: Boolean) {
        if (newChats.isEmpty()) {
            updateAdapter()
            return
        }
        if (chats.isEmpty()) {
            chats.addAll(newChats)
        } else {
            val lastOfNew = newChats[newChats.size - 1]
            val index = chats.indexOf(lastOfNew)
            if (index >= 0) {
                chats.subList(0, index).clear()
            }
            val newIndex = newChats.indexOf(chats[0])
            if (newIndex == -1) {
                if (index == -1 && fullFetch) {
                    newChats.last { it.isRight() }.let {
                        val placeholderId = it.asRight().id.inc()
                        newChats.add(Left(Placeholder(placeholderId)))
                    }
                }
                chats.addAll(0, newChats)
            } else {
                chats.addAll(0, newChats.subList(0, newIndex))
            }
        }
        // Remove all consecutive placeholders
        removeConsecutivePlaceholders()
        updateAdapter()
    }

    private fun removeConsecutivePlaceholders() {
        for (i in 0 until chats.size - 1) {
            if (chats[i].isLeft() && chats[i + 1].isLeft()) {
                chats.removeAt(i)
            }
        }
    }

    private fun replacePlaceholderWithChats(newChats: MutableList<ChatStatus>,
                                               fullFetch: Boolean, pos: Int) {
        val placeholder = chats[pos]
        if (placeholder.isLeft()) {
            chats.removeAt(pos)
        }
        if (newChats.isEmpty()) {
            updateAdapter()
            return
        }
        if (fullFetch) {
            newChats.add(placeholder)
        }
        chats.addAll(pos, newChats)
        removeConsecutivePlaceholders()
        updateAdapter()
    }

    private fun addItems(newChats: List<ChatStatus>) {
        if (newChats.isEmpty()) {
            return
        }
        val last = chats.findLast { it.isRight() }

        // I was about to replace findStatus with indexOf but it is incorrect to compare value
        // types by ID anyway and we should change equals() for Status, I think, so this makes sense
        if (last != null && !newChats.contains(last)) {
            chats.addAll(newChats)
            removeConsecutivePlaceholders()
            updateAdapter()
        }
    }

    private fun onFetchTimelineSuccess(chats: MutableList<ChatStatus>,
                                       fetchEnd: FetchEnd, pos: Int) {

        // We filled the hole (or reached the end) if the server returned less statuses than we
        // we asked for.
        val fullFetch = chats.size >= LOAD_AT_ONCE

        when (fetchEnd) {
            FetchEnd.TOP -> {
                updateChats(chats, fullFetch)
            }
            FetchEnd.MIDDLE -> {
                replacePlaceholderWithChats(chats, fullFetch, pos)
            }
            FetchEnd.BOTTOM -> {
                if (this.chats.isNotEmpty() && !this.chats.last().isRight()) {
                    this.chats.removeAt(this.chats.size - 1)
                    updateAdapter()
                }

                if (chats.isNotEmpty() && !chats.last().isRight()) {
                    // Removing placeholder if it's the last one from the cache
                    chats.removeAt(chats.size - 1)
                }

                val oldSize = this.chats.size
                if (this.chats.size > 1) {
                    addItems(chats)
                } else {
                    updateChats(chats, fullFetch)
                }

                if (this.chats.size == oldSize) {
                    // This may be a brittle check but seems like it works
                    // Can we check it using headers somehow? Do all server support them?
                    didLoadEverythingBottom = true
                }
            }
        }
        if (isAdded) {
            topProgressBar.hide()
            updateBottomLoadingState(fetchEnd)
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
            swipeRefreshLayout.isEnabled = true
            if (this.chats.size == 0) {
                showNothing()
            } else {
                this.statusView.visibility = View.GONE
            }
        }
    }

    private fun onFetchTimelineFailure(exception: Exception, fetchEnd: FetchEnd, position: Int) {
        if (isAdded) {
            swipeRefreshLayout.isRefreshing = false
            topProgressBar.hide()
            if (fetchEnd == FetchEnd.MIDDLE && !chats[position].isRight()) {
                var placeholder = chats[position].asLeftOrNull()
                val newViewData: ChatViewData
                if (placeholder == null) {
                    val chat = chats[position - 1].asRight()
                    val newId = chat.id.dec()
                    placeholder = Placeholder(newId)
                }
                newViewData = ChatViewData.Placeholder(placeholder.id, false)
                chats.setPairedItem(position, newViewData)
                updateAdapter()
            } else if (chats.isEmpty()) {
                swipeRefreshLayout.isEnabled = false
                statusView.visibility = View.VISIBLE
                if (exception is IOException) {
                    statusView.setup(R.drawable.elephant_offline, R.string.error_network) {
                        progressBar.visibility = View.VISIBLE
                        onRefresh()
                    }
                } else {
                    statusView.setup(R.drawable.elephant_error, R.string.error_generic) {
                        progressBar.visibility = View.VISIBLE
                        onRefresh()
                    }
                }
            }
            Log.e(TAG, "Fetch Failure: " + exception.message)
            updateBottomLoadingState(fetchEnd)
            progressBar.visibility = View.GONE
        }
    }

    private fun updateBottomLoadingState(fetchEnd: FetchEnd) {
        if (fetchEnd == FetchEnd.BOTTOM) {
            bottomLoading = false
        }
    }

    override fun onLoadMore(position: Int) {
        //check bounds before accessing list,
        if (chats.size >= position && position > 0) {
            val fromChat = chats[position - 1].asRightOrNull()
            val toChat = chats[position + 1].asRightOrNull()
            if (fromChat == null || toChat == null) {
                Log.e(TAG, "Failed to load more at $position, wrong placeholder position")
                return
            }

            val maxMinusOne = if (chats.size > position + 1 && chats[position + 2].isRight()) chats[position + 1].asRight().id else null
            sendFetchChatsRequest(fromChat.id, toChat.id, maxMinusOne,
                    FetchEnd.MIDDLE, position)

            val (id) = chats[position].asLeft()
            val newViewData = ChatViewData.Placeholder(id, true)
            chats.setPairedItem(position, newViewData)
            updateAdapter()
        } else {
            Log.e(TAG, "error loading more")
        }
    }

    override fun onViewAccount(id: String?) {
        id?.let(bottomSheetActivity::viewAccount)
    }

    override fun onViewUrl(url: String?) {
        url?.let { bottomSheetActivity.viewUrl(it, PostLookupFallbackBehavior.OPEN_IN_BROWSER) }
    }

    // never called
    override fun onViewTag(tag: String?) {}

    private fun updateAdapter() {
        Log.d(TAG, "updateAdapter")
        differ.submitList(chats.pairedCopy)
    }

    private fun jumpToTop() {
        if (isAdded) {
            layoutManager.scrollToPosition(0)
            recyclerView.stopScroll()
            scrollListener.reset()
        }
    }

    override fun onReselect() {
        jumpToTop()
    }

    override fun onResume() {
        super.onResume()
        startUpdateTimestamp()
    }

    override fun refreshContent() {
        if (isAdded) onRefresh() else isNeedRefresh = true
    }

    /**
     * Start to update adapter every minute to refresh timestamp
     * If setting absoluteTimeView is false
     * Auto dispose observable on pause
     */
    private fun startUpdateTimestamp() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val useAbsoluteTime = preferences.getBoolean("absoluteTimeView", false)
        if (!useAbsoluteTime) {
            Observable.interval(1, TimeUnit.MINUTES)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(this, Lifecycle.Event.ON_PAUSE)
                    .subscribe { updateAdapter() }
        }
    }

    private fun findChatPosition(id: String) : Int {
        return chats.indexOfFirst { it.isRight() && it.asRight().id == id }
    }

    private fun markAsRead(chat: Chat) {
        val pos = findChatPosition(chat.id)
        val chatViewData = ViewDataUtils.chatToViewData(chat)

        chats.setPairedItem(pos, chatViewData)
        updateAdapter()
    }

    override fun onMore(id: String, v: View) {
        val popup = PopupMenu(requireContext(), v)
        popup.inflate(R.menu.chat_more)
        val pos = findChatPosition(id)
        val chat = chats[pos].asRight()
        // val menu = popup.menu
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.chat_mark_as_read -> {
                    api.markChatAsRead(chat.id, chat.lastMessage?.id ?: null)
                        .observeOn(AndroidSchedulers.mainThread())
                        .autoDispose(this, Lifecycle.Event.ON_DESTROY)
                        .subscribe({ chat -> markAsRead(chat)
                        }, { err -> Log.e(TAG, "Failed to mark chat as read", err) })

                    true
                }
                else -> {
                    false // ????
                }
            }
        }
        popup.show()
    }

    override fun openChat(position: Int) {
        if(position < 0 || position >= chats.size)
            return

        val chat = chats[position].asRightOrNull()
        chat?.let {
            bottomSheetActivity.openChat(it)
        }
    }
}