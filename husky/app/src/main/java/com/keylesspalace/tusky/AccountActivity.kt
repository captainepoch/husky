/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2018  Conny Duck
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

package com.keylesspalace.tusky

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.emoji2.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.MarginPageTransformer
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.keylesspalace.tusky.adapter.AccountFieldAdapter
import com.keylesspalace.tusky.components.chat.ChatActivity
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.profile.ui.view.EditProfileActivity
import com.keylesspalace.tusky.components.report.ReportActivity
import com.keylesspalace.tusky.core.extensions.DefaultTextWatcher
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityAccountBinding
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.Relationship
import com.keylesspalace.tusky.interfaces.ActionButtonActivity
import com.keylesspalace.tusky.interfaces.LinkListener
import com.keylesspalace.tusky.interfaces.ReselectableFragment
import com.keylesspalace.tusky.pager.AccountPagerAdapter
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.Error
import com.keylesspalace.tusky.util.LinkHelper
import com.keylesspalace.tusky.util.Success
import com.keylesspalace.tusky.util.ThemeUtils
import com.keylesspalace.tusky.util.emojify
import com.keylesspalace.tusky.util.hide
import com.keylesspalace.tusky.util.loadAvatar
import com.keylesspalace.tusky.util.show
import com.keylesspalace.tusky.util.visible
import com.keylesspalace.tusky.view.showMuteAccountDialog
import com.keylesspalace.tusky.viewmodel.AccountViewModel
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.NumberFormat
import kotlin.math.abs

class AccountActivity :
    BottomSheetActivity(),
    ActionButtonActivity,
    LinkListener {

    private val binding by viewBinding(ActivityAccountBinding::inflate)
    private val viewModel: AccountViewModel by viewModel()

    private val accountFieldAdapter = AccountFieldAdapter(this)

    private var followState: FollowState = FollowState.NOT_FOLLOWING
    private var blocking: Boolean = false
    private var muting: Boolean = false
    private var blockingDomain: Boolean = false
    private var showingReblogs: Boolean = false
    private var subscribing: Boolean = false
    private var loadedAccount: Account? = null

    private var animateAvatar: Boolean = false

    // fields for scroll animation
    private var hideFab: Boolean = false
    private var oldOffset: Int = 0

    @ColorInt
    private var toolbarColor: Int = 0

    @ColorInt
    private var statusBarColorTransparent: Int = 0

    @ColorInt
    private var statusBarColorOpaque: Int = 0

    private var avatarSize: Float = 0f

    @Px
    private var titleVisibleHeight: Int = 0
    private lateinit var domain: String

    private enum class FollowState {
        NOT_FOLLOWING,
        FOLLOWING,
        REQUESTED
    }

    private lateinit var adapter: AccountPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadResources()
        makeNotificationBarTransparent()
        setContentView(binding.root)

        // Obtain information to fill out the profile.
        viewModel.setAccountInfo(intent.getStringExtra(KEY_ACCOUNT_ID)!!)

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        animateAvatar = sharedPrefs.getBoolean("animateGifAvatars", false)
        hideFab = sharedPrefs.getBoolean("fabHide", false)

        setupToolbar()
        setupTabs()
        setupAccountViews()
        setupRefreshLayout()
        subscribeObservables()

        if (viewModel.isSelf) {
            updateButtons()
            binding.saveNoteInfo.hide()
        } else {
            binding.saveNoteInfo.visibility = View.INVISIBLE
        }
    }

    /**
     * Load colors and dimensions from resources
     */
    private fun loadResources() {
        toolbarColor = ThemeUtils.getColor(this, R.attr.colorSurface)
        statusBarColorTransparent =
            ContextCompat.getColor(this, R.color.transparent_statusbar_background)
        statusBarColorOpaque = ThemeUtils.getColor(this, R.attr.colorPrimaryDark)
        avatarSize = resources.getDimension(R.dimen.account_activity_avatar_size)
        titleVisibleHeight =
            resources.getDimensionPixelSize(R.dimen.account_activity_scroll_title_visible_height)
    }

    /**
     * Setup account widgets visibility and actions
     */
    private fun setupAccountViews() {
        // Initialise the default UI states.
        binding.accountAdminTextView.hide()
        binding.accountModeratorTextView.hide()
        binding.accountFloatingActionButton.hide()
        binding.accountFollowButton.hide()
        binding.accountMuteButton.hide()
        binding.accountFollowsYouTextView.hide()

        // setup the RecyclerView for the account fields
        binding.accountFieldList.isNestedScrollingEnabled = false
        binding.accountFieldList.layoutManager = LinearLayoutManager(this)
        binding.accountFieldList.adapter = accountFieldAdapter

        val accountListClickListener = { v: View ->
            val type = when (v.id) {
                R.id.accountFollowers -> AccountListActivity.Type.FOLLOWERS
                R.id.accountFollowing -> AccountListActivity.Type.FOLLOWS
                else -> throw AssertionError()
            }
            val accountListIntent = AccountListActivity.newIntent(
                this,
                type,
                viewModel.accountId
            )
            startActivityWithSlideInAnimation(accountListIntent)
        }
        binding.accountFollowers.setOnClickListener(accountListClickListener)
        binding.accountFollowing.setOnClickListener(accountListClickListener)

        binding.accountStatuses.setOnClickListener {
            // Make nice ripple effect on tab
            binding.accountTabLayout.getTabAt(0)!!.select()
            val poorTabView = (binding.accountTabLayout.getChildAt(0) as ViewGroup)
                .getChildAt(0)
            poorTabView.isPressed = true
            binding.accountTabLayout.postDelayed({ poorTabView.isPressed = false }, 300)
        }

        // If wellbeing mode is enabled, follow stats and posts count should be hidden
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val wellbeingEnabled = preferences.getBoolean(PrefKeys.WELLBEING_HIDE_STATS_PROFILE, false)

        if (wellbeingEnabled) {
            binding.accountStatuses.hide()
            binding.accountFollowers.hide()
            binding.accountFollowing.hide()
        }
    }

    /**
     * Init timeline tabs
     */
    private fun setupTabs() {
        // Setup the tabs and timeline pager.
        adapter = AccountPagerAdapter(this, viewModel.accountId)

        binding.accountFragmentViewPager.adapter = adapter
        binding.accountFragmentViewPager.offscreenPageLimit = 2

        val pageTitles = arrayOf(
            getString(R.string.title_statuses),
            getString(R.string.title_statuses_with_replies),
            getString(R.string.title_statuses_pinned),
            getString(R.string.title_media)
        )

        TabLayoutMediator(
            binding.accountTabLayout,
            binding.accountFragmentViewPager
        ) { tab, position ->
            tab.text = pageTitles[position]
        }.attach()

        val pageMargin = resources.getDimensionPixelSize(R.dimen.tab_page_margin)
        binding.accountFragmentViewPager.setPageTransformer(MarginPageTransformer(pageMargin))

        binding.accountTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    (adapter.getFragment(position) as? ReselectableFragment)?.onReselect()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupToolbar() {
        // set toolbar top margin according to system window insets
        binding.accountCoordinatorLayout.setOnApplyWindowInsetsListener { _, insets ->
            val top = insets.systemWindowInsetTop

            val toolbarParams =
                binding.accountToolbar.layoutParams as CollapsingToolbarLayout.LayoutParams
            toolbarParams.topMargin = top

            insets.consumeSystemWindowInsets()
        }

        // Setup the toolbar.
        setSupportActionBar(binding.accountToolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val appBarElevation = resources.getDimension(R.dimen.actionbar_elevation)

        val toolbarBackground =
            MaterialShapeDrawable.createWithElevationOverlay(this, appBarElevation)
        toolbarBackground.fillColor = ColorStateList.valueOf(Color.TRANSPARENT)
        binding.accountToolbar.background = toolbarBackground

        binding.accountHeaderInfoContainer.background =
            MaterialShapeDrawable.createWithElevationOverlay(this, appBarElevation)

        val avatarBackground =
            MaterialShapeDrawable.createWithElevationOverlay(this, appBarElevation).apply {
                fillColor = ColorStateList.valueOf(toolbarColor)
                elevation = appBarElevation
                shapeAppearanceModel = ShapeAppearanceModel.builder()
                    .setAllCornerSizes(resources.getDimension(R.dimen.account_avatar_background_radius))
                    .build()
            }
        binding.accountAvatarImageView.background = avatarBackground

        // Add a listener to change the toolbar icon color when it enters/exits its collapsed state.
        binding.accountAppBarLayout.addOnOffsetChangedListener(object :
                AppBarLayout.OnOffsetChangedListener {

                override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                    if (verticalOffset == oldOffset) {
                        return
                    }
                    oldOffset = verticalOffset

                    if (titleVisibleHeight + verticalOffset < 0) {
                        supportActionBar?.setDisplayShowTitleEnabled(true)
                    } else {
                        supportActionBar?.setDisplayShowTitleEnabled(false)
                    }

                    if (hideFab && !viewModel.isSelf && !blocking) {
                        if (verticalOffset > oldOffset) {
                            binding.accountFloatingActionButton.show()
                        }
                        if (verticalOffset < oldOffset) {
                            hideFabMenu()
                            binding.accountFloatingActionButton.hide()
                        }
                    }

                    val scaledAvatarSize = (avatarSize + verticalOffset) / avatarSize

                    binding.accountAvatarImageView.scaleX = scaledAvatarSize
                    binding.accountAvatarImageView.scaleY = scaledAvatarSize

                    binding.accountAvatarImageView.visible(scaledAvatarSize > 0)

                    val transparencyPercent =
                        (abs(verticalOffset) / titleVisibleHeight.toFloat()).coerceAtMost(1f)

                    window.statusBarColor = argbEvaluator.evaluate(
                        transparencyPercent,
                        statusBarColorTransparent,
                        statusBarColorOpaque
                    ) as Int

                    val evaluatedToolbarColor = argbEvaluator.evaluate(
                        transparencyPercent,
                        Color.TRANSPARENT,
                        toolbarColor
                    ) as Int

                    toolbarBackground.fillColor = ColorStateList.valueOf(evaluatedToolbarColor)

                    binding.swipeToRefreshLayout.isEnabled = verticalOffset == 0
                }
            })
    }

    private fun makeNotificationBarTransparent() {
        val decorView = window.decorView
        decorView.systemUiVisibility =
            decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = statusBarColorTransparent
    }

    /**
     * Subscribe to data loaded at the view model
     */
    private fun subscribeObservables() {
        viewModel.accountData.observe(this) {
            when (it) {
                is Success -> onAccountChanged(it.data)
                is Error -> {
                    Snackbar.make(
                        binding.accountCoordinatorLayout,
                        R.string.error_generic,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.action_retry) { viewModel.refresh() }
                        .show()
                }
                else -> {}
            }
        }

        viewModel.relationshipData.observe(this) {
            val relation = it?.data
            if (relation != null) {
                onRelationshipChanged(relation)
            }

            if (it is Error) {
                Snackbar.make(
                    binding.accountCoordinatorLayout,
                    R.string.error_generic,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.action_retry) { viewModel.refresh() }
                    .show()
            }
        }

        viewModel.accountFieldData.observe(this) {
            accountFieldAdapter.fields = it
            accountFieldAdapter.notifyDataSetChanged()
        }

        viewModel.noteSaved.observe(this) {
            binding.saveNoteInfo.visible(it, View.INVISIBLE)
        }
    }

    /**
     * Setup swipe to refresh layout
     */
    private fun setupRefreshLayout() {
        binding.swipeToRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
            adapter.refreshContent()
        }
        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeToRefreshLayout.isRefreshing = isRefreshing == true
        }
        binding.swipeToRefreshLayout.setColorSchemeResources(R.color.tusky_blue)
    }

    private fun onAccountChanged(account: Account?) {
        loadedAccount = account ?: return

        val usernameFormatted = getString(R.string.status_username_format, account.username)
        binding.accountUsernameTextView.text = usernameFormatted
        binding.accountDisplayNameTextView.text =
            account.name.emojify(account.emojis, binding.accountDisplayNameTextView)

        val emojifiedNote = account.note.emojify(account.emojis, binding.accountNoteTextView)
        LinkHelper.setClickableText(binding.accountNoteTextView, emojifiedNote, null, this)

        // accountFieldAdapter.fields = account.fields ?: emptyList()
        accountFieldAdapter.emojis = account.emojis ?: emptyList()
        accountFieldAdapter.notifyDataSetChanged()

        binding.accountLockedImageView.visible(account.locked)
        binding.accountBadgeTextView.visible(account.bot)

        // API can return user is both admin and mod
        // but admin rights already implies moderator, so just ignore it
        val isAdmin = account.pleroma?.isAdmin ?: false
        binding.accountAdminTextView.visible(isAdmin)
        binding.accountModeratorTextView.visible(!isAdmin && account.pleroma?.isModerator ?: false)

        updateAccountAvatar()
        updateToolbar()
        updateMovedAccount()
        updateRemoteAccount()
        updateAccountStats()
        invalidateOptionsMenu()

        binding.accountMuteButton.setOnClickListener {
            viewModel.unmuteAccount()
            updateMuteButton()
        }
    }

    /**
     * Load account's avatar and header image
     */
    private fun updateAccountAvatar() {
        loadedAccount?.let { account ->

            loadAvatar(
                account.avatar,
                binding.accountAvatarImageView,
                resources.getDimensionPixelSize(R.dimen.avatar_radius_94dp),
                animateAvatar
            )

            if (animateAvatar) {
                Glide.with(this)
                    .load(account.header)
                    .centerCrop()
                    .into(binding.accountHeaderImageView)
            } else {
                Glide.with(this)
                    .asBitmap()
                    .load(account.header)
                    .centerCrop()
                    .into(binding.accountHeaderImageView)
            }

            binding.accountAvatarImageView.setOnClickListener { avatarView ->
                val intent =
                    ViewMediaActivity.newSingleImageIntent(avatarView.context, account.avatar)

                avatarView.transitionName = account.avatar
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    avatarView,
                    account.avatar
                )

                startActivity(intent, options.toBundle())
            }
        }
    }

    /**
     * Update toolbar views for loaded account
     */
    private fun updateToolbar() {
        loadedAccount?.let { account ->

            val emojifiedName = account.name.emojify(account.emojis, binding.accountToolbar, true)

            try {
                supportActionBar?.title = EmojiCompat.get().process(emojifiedName)
            } catch (e: IllegalStateException) {
                supportActionBar?.title = emojifiedName
            }
            supportActionBar?.subtitle =
                String.format(getString(R.string.status_username_format), account.username)
        }
    }

    /**
     * Update moved account info
     */
    private fun updateMovedAccount() {
        loadedAccount?.moved?.let { movedAccount ->
            binding.accountMovedDisplayName.text = movedAccount.name
            binding.accountMovedUsername.text =
                getString(R.string.status_username_format, movedAccount.username)

            val avatarRadius = resources.getDimensionPixelSize(R.dimen.avatar_radius_48dp)

            loadAvatar(movedAccount.avatar, binding.accountMovedAvatar, avatarRadius, animateAvatar)

            binding.accountMovedText.text =
                getString(R.string.account_moved_description, movedAccount.name)

            // this is necessary because API 19 can't handle vector compound drawables
            val movedIcon = ContextCompat.getDrawable(this, R.drawable.ic_briefcase)?.mutate()
            val textColor = ThemeUtils.getColor(this, android.R.attr.textColorTertiary)
            movedIcon?.colorFilter = PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN)

            binding.accountMovedText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                movedIcon,
                null,
                null,
                null
            )

            binding.accountMovedView.show()
        }
    }

    /**
     * Check is account remote and update info if so
     */
    private fun updateRemoteAccount() {
        loadedAccount?.let { account ->
            if (account.isRemote()) {
                binding.accountRemoveView.show()
                binding.accountRemoveView.setOnClickListener {
                    LinkHelper.openLink(account.url, this)
                }
            }
        }
    }

    private fun FloatingActionButton.menuAnimate(show: Boolean) {
        val height = this.height.toFloat()

        if (show) {
            visibility = View.VISIBLE
            alpha = 0.0f
            translationY = height

            animate().setDuration(200)
                .translationY(0.0f)
                .alpha(1.0f)
                .setListener(object :
                        AnimatorListenerAdapter() {}) // seems listener is saved, so reset it here
                .start()
        } else {
            animate().setDuration(200)
                .translationY(height)
                .alpha(0.0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                        super.onAnimationEnd(animation)
                    }
                })
                .start()
        }
    }

    private fun hideFabMenu() {
        openedFabMenu = false

        binding.accountFloatingActionButton.animate().setDuration(200)
            .rotation(0.0f).start()
        binding.accountFloatingActionButtonChat.menuAnimate(openedFabMenu)
        binding.accountFloatingActionButtonMention.menuAnimate(openedFabMenu)
    }

    var openedFabMenu = false
    private fun animateFabMenu() {
        if (openedFabMenu) {
            hideFabMenu()
        } else {
            openedFabMenu = true

            binding.accountFloatingActionButton.animate().setDuration(200)
                .rotation(135.0f).start()
            binding.accountFloatingActionButtonChat.menuAnimate(openedFabMenu)
            binding.accountFloatingActionButtonMention.menuAnimate(openedFabMenu)
        }
    }

    /**
     * Update account stat info
     */
    private fun updateAccountStats() {
        loadedAccount?.let { account ->
            val numberFormat = NumberFormat.getNumberInstance()
            binding.accountFollowersTextView.text = numberFormat.format(account.followersCount)
            binding.accountFollowingTextView.text = numberFormat.format(account.followingCount)
            binding.accountStatusesTextView.text = numberFormat.format(account.statusesCount)

            binding.accountFloatingActionButtonMention.setOnClickListener { mention() }

            if (account.pleroma?.acceptsChatMessages == true) {
                binding.accountFloatingActionButtonChat.setOnClickListener {
                    mastodonApi.createChat(account.id)
                        .observeOn(AndroidSchedulers.mainThread())
                        .autoDispose(from(this, Lifecycle.Event.ON_DESTROY))
                        .subscribe({
                            val intent = ChatActivity.getIntent(this@AccountActivity, it)
                            startActivityWithSlideInAnimation(intent)
                        }, {
                            Toast.makeText(
                                this@AccountActivity,
                                getString(R.string.error_generic),
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                }
            } else {
                binding.accountFloatingActionButtonChat.backgroundTintList =
                    ColorStateList.valueOf(Color.GRAY)
                binding.accountFloatingActionButtonChat.setOnClickListener {
                    Toast.makeText(
                        this@AccountActivity,
                        getString(R.string.error_chat_recipient_unavailable),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            binding.accountFloatingActionButton.setOnClickListener { animateFabMenu() }

            binding.accountFollowButton.setOnClickListener {
                if (viewModel.isSelf) {
                    val intent = Intent(this@AccountActivity, EditProfileActivity::class.java)
                    startActivity(intent)
                    return@setOnClickListener
                }

                if (blocking) {
                    viewModel.changeBlockState()
                    return@setOnClickListener
                }

                when (followState) {
                    FollowState.NOT_FOLLOWING -> {
                        viewModel.changeFollowState()
                    }
                    FollowState.REQUESTED -> {
                        showFollowRequestPendingDialog()
                    }
                    FollowState.FOLLOWING -> {
                        showUnfollowWarningDialog()
                    }
                }
                updateFollowButton()
            }
        }
    }

    private fun onRelationshipChanged(relation: Relationship) {
        followState = when {
            relation.following -> FollowState.FOLLOWING
            relation.requested -> FollowState.REQUESTED
            else -> FollowState.NOT_FOLLOWING
        }
        blocking = relation.blocking
        muting = relation.muting
        blockingDomain = relation.blockingDomain
        showingReblogs = relation.showingReblogs

        // If wellbeing mode is enabled, "follows you" text should not be visible
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val wellbeingEnabled = preferences.getBoolean(PrefKeys.WELLBEING_HIDE_STATS_PROFILE, false)

        binding.accountFollowsYouTextView.visible(relation.followedBy && !wellbeingEnabled)

        // because subscribing is Pleroma extension, enable it __only__ when we have non-null subscribing field
        // it's also now supported in Mastodon 3.3.0rc but called notifying and use different API call
        if (!viewModel.isSelf && followState == FollowState.FOLLOWING &&
            (relation.subscribing != null || relation.notifying != null)
        ) {
            binding.accountSubscribeButton.show()
            binding.accountSubscribeButton.setOnClickListener {
                viewModel.changeSubscribingState()
            }
            if (relation.notifying != null) {
                subscribing = relation.notifying
            } else if (relation.subscribing != null) {
                subscribing = relation.subscribing
            }
        }

        binding.accountNoteTextInputLayout.visible(relation.note != null)
        binding.accountNoteTextInputLayout.editText?.setText(relation.note)

        // add the listener late to avoid it firing on the first change
        binding.accountNoteTextInputLayout.editText?.removeTextChangedListener(noteWatcher)
        binding.accountNoteTextInputLayout.editText?.addTextChangedListener(noteWatcher)

        updateButtons()
    }

    private val noteWatcher = object : DefaultTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            viewModel.noteChanged(s.toString())
        }
    }

    private fun updateFollowButton() {
        if (viewModel.isSelf) {
            binding.accountFollowButton.setText(R.string.action_edit_own_profile)
            return
        }
        if (blocking) {
            binding.accountFollowButton.setText(R.string.action_unblock)
            return
        }
        when (followState) {
            FollowState.NOT_FOLLOWING -> {
                binding.accountFollowButton.setText(R.string.action_follow)
            }
            FollowState.REQUESTED -> {
                binding.accountFollowButton.setText(R.string.state_follow_requested)
            }
            FollowState.FOLLOWING -> {
                binding.accountFollowButton.setText(R.string.action_unfollow)
            }
        }
        updateSubscribeButton()
    }

    private fun updateMuteButton() {
        if (muting) {
            binding.accountMuteButton.setIconResource(R.drawable.ic_unmute_24dp)
        } else {
            binding.accountMuteButton.hide()
        }
    }

    private fun updateSubscribeButton() {
        if (followState != FollowState.FOLLOWING) {
            binding.accountSubscribeButton.hide()
        }

        if (subscribing) {
            binding.accountSubscribeButton.setIconResource(R.drawable.ic_notifications_active_24dp)
        } else {
            binding.accountSubscribeButton.setIconResource(R.drawable.ic_notifications_24dp)
        }
    }

    private fun updateButtons() {
        invalidateOptionsMenu()

        if (loadedAccount?.moved == null) {
            binding.accountFollowButton.show()
            updateFollowButton()

            if (blocking || viewModel.isSelf) {
                hideFabMenu()
                binding.accountFloatingActionButton.hide()
                binding.accountMuteButton.hide()
                binding.accountSubscribeButton.hide()
            } else {
                binding.accountFloatingActionButton.show()
                if (muting) {
                    binding.accountMuteButton.show()
                } else {
                    binding.accountMuteButton.hide()
                }
                updateMuteButton()
            }
        } else {
            hideFabMenu()
            binding.accountFloatingActionButton.hide()
            binding.accountFollowButton.hide()
            binding.accountMuteButton.hide()
            binding.accountSubscribeButton.hide()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.account_toolbar, menu)

        if (!viewModel.isSelf) {
            val follow = menu.findItem(R.id.action_follow)
            follow.title = if (followState == FollowState.NOT_FOLLOWING) {
                getString(R.string.action_follow)
            } else {
                getString(R.string.action_unfollow)
            }

            follow.isVisible = followState != FollowState.REQUESTED

            val block = menu.findItem(R.id.action_block)
            block.title = if (blocking) {
                getString(R.string.action_unblock)
            } else {
                getString(R.string.action_block)
            }

            val mute = menu.findItem(R.id.action_mute)
            mute.title = if (muting) {
                getString(R.string.action_unmute)
            } else {
                getString(R.string.action_mute)
            }

            if (loadedAccount != null) {
                val muteDomain = menu.findItem(R.id.action_mute_domain)
                domain = LinkHelper.getDomain(loadedAccount?.url)
                if (domain.isEmpty()) {
                    // If we can't get the domain, there's no way we can mute it anyway...
                    menu.removeItem(R.id.action_mute_domain)
                } else {
                    if (blockingDomain) {
                        muteDomain.title = getString(R.string.action_unmute_domain, domain)
                    } else {
                        muteDomain.title = getString(R.string.action_mute_domain, domain)
                    }
                }
            }

            if (followState == FollowState.FOLLOWING) {
                val showReblogs = menu.findItem(R.id.action_show_reblogs)
                showReblogs.title = if (showingReblogs) {
                    getString(R.string.action_hide_reblogs)
                } else {
                    getString(R.string.action_show_reblogs)
                }
            } else {
                menu.removeItem(R.id.action_show_reblogs)
            }
        } else {
            // It shouldn't be possible to block, follow, mute or report yourself.
            menu.removeItem(R.id.action_follow)
            menu.removeItem(R.id.action_block)
            menu.removeItem(R.id.action_mute)
            menu.removeItem(R.id.action_mute_domain)
            menu.removeItem(R.id.action_show_reblogs)
            menu.removeItem(R.id.action_report)
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun showFollowRequestPendingDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.dialog_message_cancel_follow_request)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.changeFollowState() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showUnfollowWarningDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.dialog_unfollow_warning)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.changeFollowState() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun toggleBlockDomain(instance: String) {
        if (blockingDomain) {
            viewModel.unblockDomain(instance)
        } else {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.mute_domain_warning, instance))
                .setPositiveButton(getString(R.string.mute_domain_warning_dialog_ok)) { _, _ ->
                    viewModel.blockDomain(
                        instance
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun toggleBlock() {
        if (viewModel.relationshipData.value?.data?.blocking != true) {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.dialog_block_warning, loadedAccount?.username))
                .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.changeBlockState() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            viewModel.changeBlockState()
        }
    }

    private fun toggleMute() {
        if (viewModel.relationshipData.value?.data?.muting != true) {
            loadedAccount?.let {
                showMuteAccountDialog(
                    this,
                    it.username
                ) { notifications, duration ->
                    viewModel.muteAccount(notifications, duration)
                }
            }
        } else {
            viewModel.unmuteAccount()
        }
    }

    private fun mention() {
        loadedAccount?.let {
            val intent = ComposeActivity.startIntent(
                this,
                ComposeActivity.ComposeOptions(mentionedUsernames = setOf(it.username))
            )
            startActivity(intent)
        }
    }

    override fun onViewTag(tag: String) {
        val intent = Intent(this, ViewTagActivity::class.java)
        intent.putExtra("hashtag", tag)
        startActivityWithSlideInAnimation(intent)
    }

    override fun onViewAccount(id: String) {
        val intent = Intent(this, AccountActivity::class.java)
        intent.putExtra("id", id)
        startActivityWithSlideInAnimation(intent)
    }

    override fun onViewUrl(url: String) {
        viewUrl(url)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_mention -> {
                mention()
                return true
            }
            R.id.action_open_in_web -> {
                // If the account isn't loaded yet, eat the input.
                if (loadedAccount != null) {
                    LinkHelper.openLink(loadedAccount?.url, this)
                }
                return true
            }
            R.id.action_follow -> {
                viewModel.changeFollowState()
                return true
            }
            R.id.action_block -> {
                toggleBlock()
                return true
            }
            R.id.action_mute -> {
                toggleMute()
                return true
            }
            R.id.action_mute_domain -> {
                toggleBlockDomain(domain)
                return true
            }
            R.id.action_show_reblogs -> {
                viewModel.changeShowReblogsState()
                return true
            }
            R.id.action_report -> {
                if (loadedAccount != null) {
                    startActivity(
                        ReportActivity.getIntent(
                            this,
                            viewModel.accountId,
                            loadedAccount!!.username
                        )
                    )
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getActionButton(): FloatingActionButton? {
        return if (!viewModel.isSelf && !blocking) {
            binding.accountFloatingActionButton
        } else {
            null
        }
    }

    override fun onActionButtonHidden() {
        hideFabMenu()
    }

    companion object {

        private const val KEY_ACCOUNT_ID = "id"
        private val argbEvaluator = ArgbEvaluator()

        @JvmStatic
        fun getIntent(context: Context, accountId: String): Intent {
            val intent = Intent(context, AccountActivity::class.java)
            intent.putExtra(KEY_ACCOUNT_ID, accountId)
            return intent
        }
    }
}
