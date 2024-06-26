/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
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

package com.keylesspalace.tusky.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.keylesspalace.tusky.AccountActivity
import com.keylesspalace.tusky.AccountListActivity.Type
import com.keylesspalace.tusky.BaseActivity
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.adapter.AccountAdapter
import com.keylesspalace.tusky.adapter.BlocksAdapter
import com.keylesspalace.tusky.adapter.FollowAdapter
import com.keylesspalace.tusky.adapter.FollowRequestsAdapter
import com.keylesspalace.tusky.adapter.MutesAdapter
import com.keylesspalace.tusky.databinding.FragmentAccountListBinding
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.EmojiReaction
import com.keylesspalace.tusky.entity.Relationship
import com.keylesspalace.tusky.interfaces.AccountActionListener
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.HttpHeaderLink
import com.keylesspalace.tusky.util.hide
import com.keylesspalace.tusky.util.show
import com.keylesspalace.tusky.view.EndlessOnScrollListener
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from
import com.uber.autodispose.autoDispose
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.android.ext.android.inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class AccountListFragment : BaseFragment(), AccountActionListener {

    // TODO(ViewBinding): Remove lateinit in favor of the extension
    // private val binding by viewBinding(FragmentAccountListBinding::bind)
    private lateinit var binding: FragmentAccountListBinding

    private val api: MastodonApi by inject()
    private lateinit var type: Type
    private var id: String? = null
    private var emojiReaction: String? = null

    private lateinit var scrollListener: EndlessOnScrollListener
    private lateinit var adapter: AccountAdapter
    private var fetching = false
    private var bottomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getSerializable(ARG_TYPE) as Type
        id = arguments?.getString(ARG_ID)
        emojiReaction = arguments?.getString(ARG_EMOJI)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(view.context)
        binding.recyclerView.layoutManager = layoutManager
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                view.context,
                DividerItemDecoration.VERTICAL
            )
        )

        adapter = when (type) {
            Type.BLOCKS -> BlocksAdapter(this)
            Type.MUTES -> MutesAdapter(this)
            Type.FOLLOW_REQUESTS -> FollowRequestsAdapter(this)
            else -> FollowAdapter(this)
        }
        binding.recyclerView.adapter = adapter

        scrollListener = object : EndlessOnScrollListener(layoutManager) {
            override fun onLoadMore(totalItemsCount: Int, view: RecyclerView) {
                if (bottomId == null) {
                    return
                }
                fetchAccounts(bottomId)
            }
        }

        binding.recyclerView.addOnScrollListener(scrollListener)

        fetchAccounts()
    }

    override fun onViewAccount(id: String) {
        (activity as BaseActivity?)?.let {
            val intent = AccountActivity.getIntent(it, id)
            it.startActivityWithSlideInAnimation(intent)
        }
    }

    override fun onMute(mute: Boolean, id: String, position: Int, notifications: Boolean) {
        if (!mute) {
            api.unmuteAccount(id)
        } else {
            api.muteAccount(id, notifications)
        }
            .autoDispose(from(this))
            .subscribe({
                onMuteSuccess(mute, id, position, notifications)
            }, {
                onMuteFailure(mute, id, notifications)
            })
    }

    private fun onMuteSuccess(muted: Boolean, id: String, position: Int, notifications: Boolean) {
        val mutesAdapter = adapter as MutesAdapter
        if (muted) {
            mutesAdapter.updateMutingNotifications(id, notifications, position)
            return
        }
        val unmutedUser = mutesAdapter.removeItem(position)

        if (unmutedUser != null) {
            Snackbar.make(binding.recyclerView, R.string.confirmation_unmuted, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo) {
                    mutesAdapter.addItem(unmutedUser, position)
                    onMute(true, id, position, notifications)
                }.show()
        }
    }

    private fun onMuteFailure(mute: Boolean, accountId: String, notifications: Boolean) {
        val verb = if (mute) {
            if (notifications) {
                "mute (notifications = true)"
            } else {
                "mute (notifications = false)"
            }
        } else {
            "unmute"
        }
        Log.e(TAG, "Failed to $verb account id $accountId")
    }

    override fun onBlock(block: Boolean, id: String, position: Int) {
        if (!block) {
            api.unblockAccount(id)
        } else {
            api.blockAccount(id)
        }
            .autoDispose(from(this))
            .subscribe({
                onBlockSuccess(block, id, position)
            }, {
                onBlockFailure(block, id)
            })
    }

    private fun onBlockSuccess(blocked: Boolean, id: String, position: Int) {
        if (blocked) {
            return
        }
        val blocksAdapter = adapter as BlocksAdapter
        val unblockedUser = blocksAdapter.removeItem(position)

        if (unblockedUser != null) {
            Snackbar.make(
                binding.recyclerView,
                R.string.confirmation_unblocked,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.action_undo) {
                    blocksAdapter.addItem(unblockedUser, position)
                    onBlock(true, id, position)
                }.show()
        }
    }

    private fun onBlockFailure(block: Boolean, accountId: String) {
        val verb = if (block) {
            "block"
        } else {
            "unblock"
        }
        Log.e(TAG, "Failed to $verb account accountId $accountId")
    }

    override fun onRespondToFollowRequest(
        accept: Boolean,
        accountId: String,
        position: Int
    ) {
        val callback = object : Callback<Relationship> {
            override fun onResponse(call: Call<Relationship>, response: Response<Relationship>) {
                if (response.isSuccessful) {
                    onRespondToFollowRequestSuccess(position)
                } else {
                    onRespondToFollowRequestFailure(accept, accountId)
                }
            }

            override fun onFailure(call: Call<Relationship>, t: Throwable) {
                onRespondToFollowRequestFailure(accept, accountId)
            }
        }

        val call = if (accept) {
            api.authorizeFollowRequest(accountId)
        } else {
            api.rejectFollowRequest(accountId)
        }
        callList.add(call)
        call.enqueue(callback)
    }

    private fun onRespondToFollowRequestSuccess(position: Int) {
        val followRequestsAdapter = adapter as FollowRequestsAdapter
        followRequestsAdapter.removeItem(position)
    }

    private fun onRespondToFollowRequestFailure(accept: Boolean, accountId: String) {
        val verb = if (accept) {
            "accept"
        } else {
            "reject"
        }
        Log.e(TAG, "Failed to $verb account id $accountId.")
    }

    private fun getFetchCallByListType(fromId: String?): Single<Response<List<Account>>> {
        return when (type) {
            Type.FOLLOWS -> {
                val accountId = requireId(type, id)
                api.accountFollowing(accountId, fromId)
            }
            Type.FOLLOWERS -> {
                val accountId = requireId(type, id)
                api.accountFollowers(accountId, fromId)
            }
            Type.BLOCKS -> api.blocks(fromId)
            Type.MUTES -> api.mutes(fromId)
            Type.FOLLOW_REQUESTS -> api.followRequests(fromId)
            Type.REBLOGGED -> {
                val statusId = requireId(type, id)
                api.statusRebloggedBy(statusId, fromId)
            }
            Type.FAVOURITED -> {
                val statusId = requireId(type, id)
                api.statusFavouritedBy(statusId, fromId)
            }
            Type.REACTED -> {
                // HACKHACK: make compiler happy
                val statusId = requireId(type, id)
                api.statusFavouritedBy(statusId, fromId)
            }
        }
    }

    private fun requireId(type: Type, id: String?, name: String = "id"): String {
        return requireNotNull(id) { name + " must not be null for type " + type.name }
    }

    private fun getEmojiReactionFetchCall(): Single<Response<List<EmojiReaction>>> {
        val statusId = requireId(type, id)
        val emoji = requireId(type, emojiReaction, "emoji").split("@")[0]
        return api.statusReactedBy(statusId, emoji)
    }

    private fun fetchAccounts(fromId: String? = null) {
        if (fetching) {
            return
        }
        fetching = true

        if (fromId != null) {
            binding.recyclerView.post { adapter.setBottomLoading(true) }
        }

        if (type == Type.REACTED) {
            getEmojiReactionFetchCall()
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(from(this, Lifecycle.Event.ON_DESTROY))
                .subscribe({ response ->
                    val emojiReaction = response.body()

                    if (response.isSuccessful &&
                        emojiReaction != null &&
                        emojiReaction.isNotEmpty() &&
                        emojiReaction[0].accounts != null
                    ) {
                        val linkHeader = response.headers()["Link"]
                        onFetchAccountsSuccess(emojiReaction[0].accounts!!, linkHeader)
                    } else {
                        onFetchAccountsFailure(Exception(response.message()))
                    }
                }, { throwable ->
                    onFetchAccountsFailure(throwable)
                })
        } else {
            getFetchCallByListType(fromId)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(from(this, Lifecycle.Event.ON_DESTROY))
                .subscribe({ response ->
                    val accountList = response.body()

                    if (response.isSuccessful && accountList != null) {
                        val linkHeader = response.headers()["Link"]
                        onFetchAccountsSuccess(accountList, linkHeader)
                    } else {
                        onFetchAccountsFailure(Exception(response.message()))
                    }
                }, { throwable ->
                    onFetchAccountsFailure(throwable)
                })
        }
    }

    private fun onFetchAccountsSuccess(accounts: List<Account>, linkHeader: String?) {
        adapter.setBottomLoading(false)

        val links = HttpHeaderLink.parse(linkHeader)
        val next = HttpHeaderLink.findByRelationType(links, "next")
        val fromId = next?.uri?.getQueryParameter("max_id")

        if (adapter.itemCount > 0) {
            adapter.addItems(accounts)
        } else {
            adapter.update(accounts)
        }

        if (adapter is MutesAdapter) {
            fetchRelationships(accounts.map { it.id })
        }

        bottomId = fromId

        fetching = false

        if (adapter.itemCount == 0) {
            binding.messageView.show()
            binding.messageView.setup(
                R.drawable.elephant_friend_empty,
                R.string.message_empty,
                null
            )
        } else {
            binding.messageView.hide()
        }
    }

    private fun fetchRelationships(ids: List<String>) {
        api.relationships(ids)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(from(this))
            .subscribe(::onFetchRelationshipsSuccess) {
                onFetchRelationshipsFailure(ids)
            }
    }

    private fun onFetchRelationshipsSuccess(relationships: List<Relationship>) {
        val mutesAdapter = adapter as MutesAdapter
        var mutingNotificationsMap = HashMap<String, Boolean>()
        relationships.map { mutingNotificationsMap.put(it.id, it.mutingNotifications) }
        mutesAdapter.updateMutingNotificationsMap(mutingNotificationsMap)
    }

    private fun onFetchRelationshipsFailure(ids: List<String>) {
        Log.e(TAG, "Fetch failure for relationships of accounts: $ids")
    }

    private fun onFetchAccountsFailure(throwable: Throwable) {
        fetching = false
        Log.e(TAG, "Fetch failure", throwable)

        if (adapter.itemCount == 0) {
            binding.messageView.show()
            if (throwable is IOException) {
                binding.messageView.setup(R.drawable.elephant_offline, R.string.error_network) {
                    binding.messageView.hide()
                    this.fetchAccounts(null)
                }
            } else {
                binding.messageView.setup(R.drawable.elephant_error, R.string.error_generic) {
                    binding.messageView.hide()
                    this.fetchAccounts(null)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AccountList" // logging tag
        private const val ARG_TYPE = "type"
        private const val ARG_ID = "id"
        private const val ARG_EMOJI = "emoji"

        fun newInstance(
            type: Type,
            id: String? = null,
            emoji: String? = null
        ): AccountListFragment {
            return AccountListFragment().apply {
                arguments = Bundle(3).apply {
                    putSerializable(ARG_TYPE, type)
                    putString(ARG_ID, id)
                    putString(ARG_EMOJI, emoji)
                }
            }
        }
    }
}
