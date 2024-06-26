/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2020  Tusky Contributors
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

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.InitCallback
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.MarginPageTransformer
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.FixedSizeDrawable
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import com.keylesspalace.tusky.appstore.AnnouncementReadEvent
import com.keylesspalace.tusky.appstore.CacheUpdater
import com.keylesspalace.tusky.appstore.Event
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.MainTabsChangedEvent
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.appstore.ProfileEditedEvent
import com.keylesspalace.tusky.components.announcements.AnnouncementsActivity
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.compose.ComposeActivity.Companion.canHandleMimeType
import com.keylesspalace.tusky.components.conversation.ConversationsRepository
import com.keylesspalace.tusky.components.drafts.DraftsActivity
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.components.preference.PreferencesActivity
import com.keylesspalace.tusky.components.profile.ui.view.EditProfileActivity
import com.keylesspalace.tusky.components.scheduled.ScheduledTootActivity
import com.keylesspalace.tusky.components.search.SearchActivity
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.core.utils.ApplicationUtils
import com.keylesspalace.tusky.databinding.ActivityMainBinding
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.fragment.SFragment
import com.keylesspalace.tusky.interfaces.AccountSelectionListener
import com.keylesspalace.tusky.interfaces.ActionButtonActivity
import com.keylesspalace.tusky.interfaces.ReselectableFragment
import com.keylesspalace.tusky.pager.MainPagerAdapter
import com.keylesspalace.tusky.service.StreamingService
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.ThemeUtils
import com.keylesspalace.tusky.util.ViewPager2Fix
import com.keylesspalace.tusky.util.deleteStaleCachedMedia
import com.keylesspalace.tusky.util.emojify
import com.keylesspalace.tusky.util.hide
import com.keylesspalace.tusky.util.removeShortcut
import com.keylesspalace.tusky.util.updateShortcut
import com.keylesspalace.tusky.util.visible
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.model.AbstractDrawerItem
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.model.interfaces.descriptionRes
import com.mikepenz.materialdrawer.model.interfaces.descriptionText
import com.mikepenz.materialdrawer.model.interfaces.iconDrawable
import com.mikepenz.materialdrawer.model.interfaces.iconRes
import com.mikepenz.materialdrawer.model.interfaces.iconUrl
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.mikepenz.materialdrawer.util.addItemAtPosition
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.addItemsAtPosition
import com.mikepenz.materialdrawer.util.getDrawerItem
import com.mikepenz.materialdrawer.util.removeItems
import com.mikepenz.materialdrawer.util.updateBadge
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainActivity : BottomSheetActivity(), ActionButtonActivity {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val eventHub: EventHub by inject()
    private val cacheUpdater: CacheUpdater by inject()
    private val conversationRepository: ConversationsRepository by inject()
    private val appDb: AppDatabase by inject()

    private lateinit var header: AccountHeaderView

    private var notificationTabPosition = 0
    private var onTabSelectedListener: OnTabSelectedListener? = null
    private var unreadAnnouncementsCount = 0
    private val notificationPermissionsRequestCode = 100

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    private lateinit var glide: RequestManager

    private val emojiInitCallback = object : InitCallback() {
        override fun onInitialized() {
            if (!isDestroyed) {
                updateProfiles()
            }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            when {
                binding.mainDrawerLayout.isOpen -> {
                    binding.mainDrawerLayout.close()
                }

                binding.viewPager.currentItem != 0 -> {
                    binding.viewPager.currentItem = 0
                }

                else -> {
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Will be redirected to LoginActivity by BaseActivity
        val activeAccount = accountManager.value.activeAccount ?: return

        var showNotificationTab = false
        if (intent != null) {
            /** There are two possibilities the accountId can be passed to MainActivity:
             * - from our code as long 'account_id'.
             * - from share shortcuts as String 'android.intent.extra.shortcut.ID'.
             */
            var accountId = intent.getLongExtra(NotificationHelper.ACCOUNT_ID, -1)
            if (accountId == -1L) {
                val accountIdString = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)
                if (accountIdString != null) {
                    accountId = accountIdString.toLong()
                }
            }

            val accountRequested = accountId != -1L
            if (accountRequested && accountId != activeAccount.id) {
                accountManager.value.setActiveAccount(accountId)
            }

            if (canHandleMimeType(intent.type)) {
                // Sharing to Tusky from an external app
                if (accountRequested) {
                    // The correct account is already active
                    forwardShare(intent)
                } else {
                    // No account was provided, show the chooser
                    showAccountChooserDialog(
                        getString(R.string.action_share_as),
                        true,
                        object : AccountSelectionListener {
                            override fun onAccountSelected(account: AccountEntity) {
                                val requestedId = account.id
                                if (requestedId == activeAccount.id) {
                                    // The correct account is already active
                                    forwardShare(intent)
                                } else {
                                    // A different account was requested, restart the activity
                                    intent.putExtra(NotificationHelper.ACCOUNT_ID, requestedId)
                                    changeAccount(requestedId, intent)
                                }
                            }
                        }
                    )
                }
            } else if (accountRequested) {
                // User clicked a notification, show notification tab and switch user if necessary
                showNotificationTab = true
            }
        }

        // Do not draw a status bar, the DrawerLayout and the MaterialDrawerLayout have their own
        window.statusBarColor = Color.TRANSPARENT
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        ViewPager2Fix.reduceVelocity(binding.viewPager, 2.0f)

        glide = Glide.with(this)

        binding.composeButton.setOnClickListener {
            val composeIntent = Intent(applicationContext, ComposeActivity::class.java)
            startActivity(composeIntent)
        }

        val hideTopToolbar = preferences.getBoolean(PrefKeys.HIDE_TOP_TOOLBAR, false)
        binding.mainToolbar.visible(!hideTopToolbar)

        loadDrawerAvatar(activeAccount.profilePictureUrl, true)

        binding.mainToolbar.menu.add(R.string.action_search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            icon = IconicsDrawable(this@MainActivity, GoogleMaterial.Icon.gmd_search).apply {
                sizeDp = 20
                colorInt = ThemeUtils.getColor(this@MainActivity, android.R.attr.textColorPrimary)
            }
            setOnMenuItemClickListener {
                startActivity(SearchActivity.getIntent(this@MainActivity))
                true
            }
        }

        setupDrawer(savedInstanceState, addSearchButton = hideTopToolbar)

        /*
         * Fetch user info while we're doing other things. This has to be done after setting up the
         * drawer, though, because its callback touches the header in the drawer.
         */
        fetchUserInfo()

        fetchAnnouncements()

        setupTabs(showNotificationTab)

        eventHub.events
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe { event: Event? ->
                when (event) {
                    is ProfileEditedEvent -> onFetchUserInfoSuccess(event.newProfileData)
                    is MainTabsChangedEvent -> setupTabs(false)
                    is PreferenceChangedEvent -> {
                        when (event.preferenceKey) {
                            PrefKeys.LIVE_NOTIFICATIONS -> {
                                initPullNotifications()
                            }
                            PrefKeys.HIDE_LIVE_NOTIFICATION_DESCRIPTION -> {
                                initPullNotifications(rebootPush = true)
                            }
                        }
                    }
                    is AnnouncementReadEvent -> {
                        unreadAnnouncementsCount--
                        updateAnnouncementsBadge()
                    }
                }
            }

        Schedulers.io().scheduleDirect {
            // Flush old media that was cached for sharing
            deleteStaleCachedMedia(applicationContext.getExternalFilesDir("Husky"))
        }

        // Load the default settings for the app but does not overwrite changed values
        PreferenceManager.setDefaultValues(
            this,
            R.xml.app_preferences,
            true
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            notificationPermissionsRequestCode -> {
                Timber.d("Notifications permissions are requested")
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Timber.d("Permissions are granted")
                    initPullNotifications()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                Timber.d("Super method is called")
            }
        }
    }

    private fun initPullNotifications(rebootPush: Boolean = false) {
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            Timber.d("Asking permissions on Tiramisu and newer")
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                Timber.w("Permissions denied, requesting permissions for notifications")
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    notificationPermissionsRequestCode
                )

                return
            }
        }

        if (rebootPush) {
            disablePushNotifications()
        }

        if (NotificationHelper.areNotificationsEnabled(this, accountManager.value)) {
            if (accountManager.value.areNotificationsStreamingEnabled()) {
                StreamingService.startStreaming(this)
                NotificationHelper.disablePullNotifications(this)
            } else {
                StreamingService.stopStreaming(this)
                NotificationHelper.enablePullNotifications(this)
            }
        } else {
            disablePushNotifications()
        }
        draftWarning()
    }

    private fun disablePushNotifications() {
        StreamingService.stopStreaming(this)
        NotificationHelper.disablePullNotifications(this)
    }

    override fun onResume() {
        super.onResume()
        NotificationHelper.clearNotificationsForActiveAccount(this, accountManager.value)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                if (binding.mainDrawerLayout.isOpen) {
                    binding.mainDrawerLayout.close()
                } else {
                    binding.mainDrawerLayout.open()
                }
                return true
            }
            KeyEvent.KEYCODE_SEARCH -> {
                startActivityWithSlideInAnimation(SearchActivity.getIntent(this))
                return true
            }
        }

        if (event.isCtrlPressed || event.isShiftPressed) {
            // FIXME: blackberry keyONE raises SHIFT key event even CTRL IS PRESSED
            when (keyCode) {
                KeyEvent.KEYCODE_N -> {
                    // Open compose activity by pressing SHIFT + N (or CTRL + N)
                    val composeIntent = Intent(applicationContext, ComposeActivity::class.java)
                    startActivity(composeIntent)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    public override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (intent != null) {
            val statusUrl = intent.getStringExtra(STATUS_URL)
            if (statusUrl != null) {
                viewUrl(statusUrl, PostLookupFallbackBehavior.DISPLAY_ERROR)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EmojiCompat.get().unregisterInitCallback(emojiInitCallback)
    }

    private fun forwardShare(intent: Intent) {
        val composeIntent = Intent(this, ComposeActivity::class.java)
        composeIntent.action = intent.action
        composeIntent.type = intent.type
        composeIntent.putExtras(intent)
        composeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(composeIntent)
        finish()
    }

    private fun setupDrawer(savedInstanceState: Bundle?, addSearchButton: Boolean) {
        binding.mainToolbar.setNavigationOnClickListener {
            binding.mainDrawerLayout.open()
        }

        header = AccountHeaderView(this).apply {
            headerBackgroundScaleType = ImageView.ScaleType.CENTER_CROP
            currentHiddenInList = true
            onAccountHeaderListener = { _: View?, profile: IProfile, current: Boolean ->
                handleProfileClick(
                    profile,
                    current
                )
            }
            addProfile(
                ProfileSettingDrawerItem().apply {
                    identifier = DRAWER_ITEM_ADD_ACCOUNT
                    nameRes = R.string.add_account_name
                    descriptionRes = R.string.add_account_description
                    iconicsIcon = GoogleMaterial.Icon.gmd_add
                },
                0
            )
            attachToSliderView(binding.mainDrawer)
            dividerBelowHeader = false
            closeDrawerOnProfileListClick = true
        }

        header.accountHeaderBackground.setColorFilter(
            ContextCompat.getColor(
                this,
                R.color.headerBackgroundFilter
            )
        )
        header.accountHeaderBackground.setBackgroundColor(
            ThemeUtils.getColor(
                this,
                R.attr.colorBackgroundAccent
            )
        )
        val animateAvatars = preferences.getBoolean("animateGifAvatars", false)

        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
                if (animateAvatars) {
                    glide.load(uri)
                        .placeholder(placeholder)
                        .into(imageView)
                } else {
                    glide.asBitmap()
                        .load(uri)
                        .placeholder(placeholder)
                        .into(imageView)
                }
            }

            override fun cancel(imageView: ImageView) {
                glide.clear(imageView)
            }

            override fun placeholder(ctx: Context, tag: String?): Drawable {
                if (tag == DrawerImageLoader.Tags.PROFILE.name || tag == DrawerImageLoader.Tags.PROFILE_DRAWER_ITEM.name) {
                    AppCompatResources.getDrawable(ctx, R.drawable.avatar_default)
                        ?.let { drawable ->
                            return drawable
                        }
                }

                return super.placeholder(ctx, tag)
            }
        })

        binding.mainDrawer.apply {
            tintStatusBar = true
            addItems(
                primaryDrawerItem {
                    nameRes = R.string.action_edit_profile
                    iconicsIcon = GoogleMaterial.Icon.gmd_person
                    onClick = {
                        val intent = Intent(context, EditProfileActivity::class.java)
                        startActivityWithSlideInAnimation(intent)
                    }
                },
                primaryDrawerItem {
                    nameRes = R.string.action_view_favourites
                    isSelectable = false
                    iconicsIcon = GoogleMaterial.Icon.gmd_star
                    onClick = {
                        val intent = StatusListActivity.newFavouritesIntent(context)
                        startActivityWithSlideInAnimation(intent)
                    }
                },
                primaryDrawerItem {
                    nameRes = R.string.action_view_bookmarks
                    iconicsIcon = GoogleMaterial.Icon.gmd_bookmark
                    onClick = {
                        val intent = StatusListActivity.newBookmarksIntent(context)
                        startActivityWithSlideInAnimation(intent)
                    }
                },
                primaryDrawerItem {
                    nameRes = R.string.action_lists
                    iconicsIcon = GoogleMaterial.Icon.gmd_list
                    onClick = {
                        startActivityWithSlideInAnimation(ListsActivity.newIntent(context))
                    }
                },
                primaryDrawerItem {
                    nameRes = R.string.action_access_saved_toot
                    iconRes = R.drawable.ic_notebook
                    onClick = {
                        val intent = DraftsActivity.newIntent(context)
                        startActivityWithSlideInAnimation(intent)
                    }
                },
                primaryDrawerItem {
                    nameRes = R.string.action_access_scheduled_toot
                    iconRes = R.drawable.ic_access_time
                    onClick = {
                        startActivityWithSlideInAnimation(ScheduledTootActivity.newIntent(context))
                    }
                },
                primaryDrawerItem {
                    identifier = DRAWER_ITEM_ANNOUNCEMENTS
                    nameRes = R.string.title_announcements
                    iconRes = R.drawable.ic_bullhorn_24dp
                    onClick = {
                        startActivityWithSlideInAnimation(AnnouncementsActivity.newIntent(context))
                    }
                    badgeStyle = BadgeStyle().apply {
                        textColor = ColorHolder.fromColor(
                            ThemeUtils.getColor(
                                this@MainActivity,
                                R.attr.colorOnPrimary
                            )
                        )
                        color = ColorHolder.fromColor(
                            ThemeUtils.getColor(
                                this@MainActivity,
                                R.attr.colorPrimary
                            )
                        )
                    }
                },
                DividerDrawerItem(),
                secondaryDrawerItem {
                    nameRes = R.string.action_view_account_preferences
                    iconRes = R.drawable.ic_account_settings
                    onClick = {
                        val intent = PreferencesActivity.newIntent(
                            context,
                            PreferencesActivity.ACCOUNT_PREFERENCES
                        )
                        startActivityWithSlideInAnimation(intent)
                    }
                },
                secondaryDrawerItem {
                    nameRes = R.string.action_view_preferences
                    iconicsIcon = GoogleMaterial.Icon.gmd_settings
                    onClick = {
                        val intent = PreferencesActivity.newIntent(
                            context,
                            PreferencesActivity.GENERAL_PREFERENCES
                        )
                        startActivityWithSlideInAnimation(intent)
                    }
                },
                secondaryDrawerItem {
                    nameRes = R.string.about_title_activity
                    iconicsIcon = GoogleMaterial.Icon.gmd_info
                    onClick = {
                        val intent = Intent(context, AboutActivity::class.java)
                        startActivityWithSlideInAnimation(intent)
                    }
                },
                secondaryDrawerItem {
                    nameRes = R.string.action_logout
                    iconRes = R.drawable.ic_logout
                    onClick = ::logout
                }
            )

            if (addSearchButton) {
                binding.mainDrawer.addItemsAtPosition(
                    4,
                    primaryDrawerItem {
                        nameRes = R.string.action_search
                        iconicsIcon = GoogleMaterial.Icon.gmd_search
                        onClick = {
                            startActivityWithSlideInAnimation(SearchActivity.getIntent(context))
                        }
                    }
                )
            }

            setSavedInstance(savedInstanceState)
        }

        if (ApplicationUtils.isDebug()) {
            Timber.d("Flavor: ${BuildConfig.FLAVOR}")

            binding.mainDrawer.addItems(
                secondaryDrawerItem {
                    nameText = getString(R.string.debug_status)
                    isEnabled = false
                }
            )
        }
        EmojiCompat.get().registerInitCallback(emojiInitCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(binding.mainDrawer.saveInstanceState(outState))
    }

    private fun setupTabs(selectNotificationTab: Boolean) {
        val activeTabLayout = if (preferences.getString("mainNavPosition", "top") == "bottom") {
            val actionBarSize = ThemeUtils.getDimension(this, R.attr.actionBarSize)
            val fabMargin = resources.getDimensionPixelSize(R.dimen.fabMargin)
            (binding.composeButton.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin =
                actionBarSize + fabMargin
            binding.tabLayout.hide()
            binding.bottomTabLayout
        } else {
            binding.bottomNav.hide()
            (binding.viewPager.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin = 0
            (binding.composeButton.layoutParams as CoordinatorLayout.LayoutParams).anchorId =
                R.id.viewPager
            binding.tabLayout
        }

        val tabs = accountManager.value.activeAccount!!.tabPreferences

        val adapter = MainPagerAdapter(tabs, this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(
            activeTabLayout,
            binding.viewPager
        ) { _: TabLayout.Tab?, _: Int -> }.attach()
        activeTabLayout.removeAllTabs()

        for (i in tabs.indices) {
            val tab = activeTabLayout.newTab().setIcon(tabs[i].icon)

            if (tabs[i].id == LIST) {
                tab.contentDescription = tabs[i].arguments[1]
            } else {
                tab.setContentDescription(tabs[i].text)
            }
            activeTabLayout.addTab(tab)

            if (tabs[i].id == NOTIFICATIONS) {
                notificationTabPosition = i
                if (selectNotificationTab) {
                    tab.select()
                }
            }
        }

        val pageMargin = resources.getDimensionPixelSize(R.dimen.tab_page_margin)
        binding.viewPager.setPageTransformer(MarginPageTransformer(pageMargin))

        val enableSwipeForTabs = preferences.getBoolean("enableSwipeForTabs", true)
        binding.viewPager.isUserInputEnabled = enableSwipeForTabs

        onTabSelectedListener?.let {
            activeTabLayout.removeOnTabSelectedListener(it)
        }

        onTabSelectedListener = object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == notificationTabPosition) {
                    NotificationHelper.clearNotificationsForActiveAccount(
                        this@MainActivity,
                        accountManager.value
                    )
                }

                binding.mainToolbar.title = tabs[tab.position].title(this@MainActivity)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                val fragment = adapter.getFragment(tab.position)
                if (fragment is ReselectableFragment) {
                    (fragment as ReselectableFragment).onReselect()
                }
            }
        }.also {
            activeTabLayout.addOnTabSelectedListener(it)
        }

        val activeTabPosition = if (selectNotificationTab) notificationTabPosition else 0
        binding.mainToolbar.title = tabs[activeTabPosition].title(this@MainActivity)
        binding.mainToolbar.setOnClickListener {
            (adapter.getFragment(activeTabLayout.selectedTabPosition) as? ReselectableFragment)?.onReselect()
        }
    }

    private fun handleProfileClick(profile: IProfile, current: Boolean): Boolean {
        val activeAccount = accountManager.value.activeAccount

        // Open profile when active image was clicked
        if (current && activeAccount != null) {
            val intent = AccountActivity.getIntent(this, activeAccount.accountId)
            startActivityWithSlideInAnimation(intent)
            return false
        }
        // Open LoginActivity to add new account
        if (profile.identifier == DRAWER_ITEM_ADD_ACCOUNT) {
            startActivityWithSlideInAnimation(LoginActivity.getIntent(this, true))
            return false
        }
        // Change Account
        changeAccount(profile.identifier, null)
        return false
    }

    private fun changeAccount(newSelectedId: Long, forward: Intent?) {
        cacheUpdater.stop()
        SFragment.flushFilters()
        accountManager.value.setActiveAccount(newSelectedId)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        if (forward != null) {
            intent.type = forward.type
            intent.action = forward.action
            intent.putExtras(forward)
        }
        startActivity(intent)
        finishWithoutSlideOutAnimation()
        overridePendingTransition(R.anim.explode, R.anim.explode)
    }

    private fun logout() {
        accountManager.value.activeAccount?.let { activeAccount ->
            AlertDialog.Builder(this)
                .setTitle(R.string.action_logout)
                .setMessage(getString(R.string.action_logout_confirm, activeAccount.fullName))
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    NotificationHelper.deleteNotificationChannelsForAccount(activeAccount, this)
                    cacheUpdater.clearForUser(activeAccount.id)
                    conversationRepository.deleteCacheForAccount(activeAccount.id)
                    removeShortcut(this, activeAccount)
                    val newAccount = accountManager.value.logActiveAccountOut()
                    initPullNotifications()
                    val intent = if (newAccount == null) {
                        LoginActivity.getIntent(this, false)
                    } else {
                        Intent(this, MainActivity::class.java)
                    }
                    startActivity(intent)
                    finishWithoutSlideOutAnimation()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun fetchUserInfo() {
        mastodonApi.accountVerifyCredentials()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe(
                { userInfo ->
                    onFetchUserInfoSuccess(userInfo)
                },
                { throwable ->
                    Timber.e("Failed to fetch user info: ${throwable.message}")
                }
            )
    }

    private fun onFetchUserInfoSuccess(me: Account) {
        glide.asBitmap()
            .load(me.header)
            .into(header.accountHeaderBackground)

        loadDrawerAvatar(me.avatar, false)

        accountManager.value.updateActiveAccount(me)
        NotificationHelper.createNotificationChannelsForAccount(
            accountManager.value.activeAccount!!,
            this
        )

        initPullNotifications()

        // Show follow requests in the menu, if this is a locked account.
        if (me.locked && binding.mainDrawer.getDrawerItem(DRAWER_ITEM_FOLLOW_REQUESTS) == null) {
            val followRequestsItem = primaryDrawerItem {
                identifier = DRAWER_ITEM_FOLLOW_REQUESTS
                nameRes = R.string.action_view_follow_requests
                iconicsIcon = GoogleMaterial.Icon.gmd_person_add
                onClick = {
                    val intent = Intent(this@MainActivity, AccountListActivity::class.java)
                    intent.putExtra("type", AccountListActivity.Type.FOLLOW_REQUESTS)
                    startActivityWithSlideInAnimation(intent)
                }
            }
            binding.mainDrawer.addItemAtPosition(4, followRequestsItem)
        } else if (!me.locked) {
            binding.mainDrawer.removeItems(DRAWER_ITEM_FOLLOW_REQUESTS)
        }
        updateProfiles()
        updateShortcut(this, accountManager.value.activeAccount!!)
    }

    @SuppressLint("CheckResult")
    private fun loadDrawerAvatar(avatarUrl: String?, showPlaceholder: Boolean) {
        val navIconSize = resources.getDimensionPixelSize(R.dimen.avatar_toolbar_nav_icon_size)

        Glide.with(this).asDrawable()
            .load(avatarUrl)
            .placeholder(R.drawable.avatar_default)
            .transform(RoundedCorners(resources.getDimensionPixelSize(R.dimen.avatar_radius_36dp)))
            .into(object : CustomTarget<Drawable>(navIconSize, navIconSize) {

                override fun onLoadStarted(placeholder: Drawable?) {
                    if (placeholder != null) {
                        binding.mainToolbar.navigationIcon =
                            FixedSizeDrawable(placeholder, navIconSize, navIconSize)
                    }
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    binding.mainToolbar.navigationIcon = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    binding.mainToolbar.navigationIcon = placeholder
                }
            })
    }

    private fun fetchAnnouncements() {
        mastodonApi.listAnnouncements(false)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe(
                { announcements ->
                    unreadAnnouncementsCount = announcements.count { !it.read }
                    updateAnnouncementsBadge()
                },
                { throwable ->
                    Timber.w("Failed to fetch announcements: ${throwable.message}")
                }
            )
    }

    private fun updateAnnouncementsBadge() {
        binding.mainDrawer.updateBadge(
            DRAWER_ITEM_ANNOUNCEMENTS,
            StringHolder(if (unreadAnnouncementsCount <= 0) null else unreadAnnouncementsCount.toString())
        )
    }

    private fun updateProfiles() {
        val profiles: MutableList<IProfile> =
            accountManager.value.getAllAccountsOrderedByActive().map { acc ->
                val emojifiedName =
                    EmojiCompat.get().process(acc.displayName.emojify(acc.emojis, header, true))

                ProfileDrawerItem().apply {
                    isSelected = acc.isActive
                    nameText = emojifiedName ?: acc.displayName
                    if(acc.profilePictureUrl.isNullOrBlank()) {
                        iconDrawable = ContextCompat.getDrawable(
                            this@MainActivity,
                            R.drawable.avatar_default
                        )
                    } else {
                        iconUrl = acc.profilePictureUrl!!
                    }
                    isNameShown = true
                    identifier = acc.id
                    descriptionText = acc.fullName
                }
            }.toMutableList()

        // reuse the already existing "add account" item
        for (profile in header.profiles.orEmpty()) {
            if (profile.identifier == DRAWER_ITEM_ADD_ACCOUNT) {
                profiles.add(profile)
                break
            }
        }
        header.clear()
        header.profiles = profiles
        header.setActiveProfile(accountManager.value.activeAccount!!.id)
    }

    private fun draftWarning() {
        val sharedPrefsKey = "show_draft_warning"
        appDb.tootDao().savedTootCount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(this, Lifecycle.Event.ON_DESTROY)
            .subscribe { draftCount ->
                val showDraftWarning = preferences.getBoolean(sharedPrefsKey, true)
                if (draftCount > 0 && showDraftWarning) {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.new_drafts_warning)
                        .setNegativeButton("Don't show again") { _, _ ->
                            preferences.edit(commit = true) {
                                putBoolean(sharedPrefsKey, false)
                            }
                        }
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
    }

    override fun getActionButton(): FloatingActionButton = binding.composeButton

    companion object {
        private const val DRAWER_ITEM_ADD_ACCOUNT: Long = -13
        private const val DRAWER_ITEM_FOLLOW_REQUESTS: Long = 10
        private const val DRAWER_ITEM_ANNOUNCEMENTS: Long = 14
        const val STATUS_URL = "statusUrl"
    }
}

private inline fun primaryDrawerItem(block: PrimaryDrawerItem.() -> Unit): PrimaryDrawerItem {
    return PrimaryDrawerItem()
        .apply {
            isSelectable = false
            isIconTinted = true
        }
        .apply(block)
}

private inline fun secondaryDrawerItem(block: SecondaryDrawerItem.() -> Unit): SecondaryDrawerItem {
    return SecondaryDrawerItem()
        .apply {
            isSelectable = false
            isIconTinted = true
        }
        .apply(block)
}

private var AbstractDrawerItem<*, *>.onClick: () -> Unit
    get() = throw UnsupportedOperationException()
    set(value) {
        onDrawerItemClickListener = { _, _, _ ->
            value()
            false
        }
    }
