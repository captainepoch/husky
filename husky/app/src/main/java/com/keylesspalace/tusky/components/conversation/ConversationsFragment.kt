/* Copyright 2019 Conny Duck
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.components.conversation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.keylesspalace.tusky.AccountActivity
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.ViewTagActivity
import com.keylesspalace.tusky.databinding.FragmentTimelineBinding
import com.keylesspalace.tusky.fragment.SFragment
import com.keylesspalace.tusky.interfaces.ReselectableFragment
import com.keylesspalace.tusky.interfaces.StatusActionListener
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.CardViewMode
import com.keylesspalace.tusky.util.NetworkState
import com.keylesspalace.tusky.util.StatusDisplayOptions
import com.keylesspalace.tusky.util.hide
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConversationsFragment : SFragment(), StatusActionListener, ReselectableFragment {

    // TODO(ViewBinding): Remove lateinit in favor of the extension
    // private val binding by viewBinding(FragmentTimelineBinding::bind)
    private lateinit var binding: FragmentTimelineBinding
    private val viewModel: ConversationsViewModel by viewModel()
    private lateinit var adapter: ConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(view.context)

        val statusDisplayOptions = StatusDisplayOptions(
            animateAvatars = preferences.getBoolean("animateGifAvatars", false),
            mediaPreviewEnabled = accountManager.value.activeAccount?.mediaPreviewEnabled ?: true,
            useAbsoluteTime = preferences.getBoolean("absoluteTimeView", false),
            showBotOverlay = preferences.getBoolean("showBotOverlay", true),
            useBlurhash = preferences.getBoolean("useBlurhash", true),
            cardViewMode = CardViewMode.NONE,
            confirmReblogs = preferences.getBoolean("confirmReblogs", true),
            renderStatusAsMention = preferences.getBoolean(PrefKeys.RENDER_STATUS_AS_MENTION, true),
            hideStats = preferences.getBoolean(PrefKeys.WELLBEING_HIDE_STATS_POSTS, false),
            canQuotePosts = false // TODO(Get from Instance config)
        )

        adapter = ConversationAdapter(statusDisplayOptions, this, ::onTopLoaded, viewModel::retry)

        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                view.context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(view.context)
        binding.recyclerView.adapter = adapter
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        binding.progressBar.hide()
        binding.statusView.hide()

        initSwipeToRefresh()

        viewModel.conversations.observe(
            viewLifecycleOwner,
            Observer<PagedList<ConversationEntity>> {
                adapter.submitList(it)
            }
        )
        viewModel.networkState.observe(
            viewLifecycleOwner,
            Observer {
                adapter.setNetworkState(it)
            }
        )

        viewModel.load()
    }

    private fun initSwipeToRefresh() {
        viewModel.refreshState.observe(viewLifecycleOwner) {
            binding.swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(R.color.tusky_blue)
    }

    private fun onTopLoaded() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun onReblog(reblog: Boolean, position: Int) {
        // Reblog private post is not possible
    }

    override fun onFavourite(favourite: Boolean, position: Int) {
        viewModel.favourite(favourite, position)
    }

    override fun onBookmark(favourite: Boolean, position: Int) {
        viewModel.bookmark(favourite, position)
    }

    override fun onMore(view: View, position: Int) {
        viewModel.conversations.value?.getOrNull(position)?.lastStatus?.let {
            more(it.toStatus(), view, position)
        }
    }

    override fun onViewMedia(position: Int, attachmentIndex: Int, view: View?) {
        viewModel.conversations.value?.getOrNull(position)?.lastStatus?.let {
            viewMedia(attachmentIndex, it.toStatus(), view)
        }
    }

    override fun onViewThread(position: Int) {
        viewModel.conversations.value?.getOrNull(position)?.lastStatus?.let {
            viewThread(it.toStatus())
        }
    }

    override fun onViewReplyTo(position: Int) {
        // there are no Reply to labels in conversations
    }

    override fun onOpenReblog(position: Int) {
        // there are no reblogs in search results
    }

    override fun onExpandedChange(expanded: Boolean, position: Int) {
        viewModel.expandHiddenStatus(expanded, position)
    }

    override fun onContentHiddenChange(isShowing: Boolean, position: Int) {
        viewModel.showContent(isShowing, position)
    }

    override fun onLoadMore(position: Int) {
        // not using the old way of pagination
    }

    override fun onContentCollapsedChange(isCollapsed: Boolean, position: Int) {
        viewModel.collapseLongStatus(isCollapsed, position)
    }

    override fun onViewAccount(id: String) {
        val intent = AccountActivity.getIntent(requireContext(), id)
        startActivity(intent)
    }

    override fun onViewTag(tag: String) {
        val intent = Intent(context, ViewTagActivity::class.java)
        intent.putExtra("hashtag", tag)
        startActivity(intent)
    }

    override fun removeItem(position: Int) {
        viewModel.remove(position)
    }

    override fun onReply(position: Int) {
        viewModel.conversations.value?.getOrNull(position)?.lastStatus?.let {
            reply(it.toStatus())
        }
    }

    private fun jumpToTop() {
        if (isAdded) {
            binding.recyclerView.layoutManager?.scrollToPosition(0)
            binding.recyclerView.stopScroll()
        }
    }

    override fun onReselect() {
        jumpToTop()
    }

    override fun onVoteInPoll(position: Int, choices: MutableList<Int>) {
        viewModel.voteInPoll(position, choices)
    }

    companion object {
        fun newInstance() = ConversationsFragment()
    }
}
