/* Copyright 2018 Conny Duck
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

package com.keylesspalace.tusky

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.keylesspalace.tusky.components.chat.ChatActivity
import com.keylesspalace.tusky.entity.Chat
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.LinkHelper
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

/** this is the base class for all activities that open links
 *  links are checked against the api if they are mastodon links so they can be openend in Tusky
 *  Subclasses must have a bottom sheet with Id item_status_bottom_sheet in their layout hierachy
 */

abstract class BottomSheetActivity : BaseActivity() {

    lateinit var bottomSheet: BottomSheetBehavior<LinearLayout>
    var searchUrl: String? = null

    @Inject
    lateinit var mastodonApi: MastodonApi

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val bottomSheetLayout: LinearLayout = findViewById(R.id.item_status_bottom_sheet)
        bottomSheet = BottomSheetBehavior.from(bottomSheetLayout)
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    cancelActiveSearch()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

    }

    open fun viewUrl(url: String, lookupFallbackBehavior: PostLookupFallbackBehavior = PostLookupFallbackBehavior.OPEN_IN_BROWSER) {
        if (!looksLikeMastodonUrl(url)) {
            openLink(url)
            return
        }

        mastodonApi.searchObservable(
                query = url,
                resolve = true
        ).observeOn(AndroidSchedulers.mainThread())
                .autoDispose(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_DESTROY))
                .subscribe({ (accounts, statuses) ->
                    if (getCancelSearchRequested(url)) {
                        return@subscribe
                    }

                    onEndSearch(url)

                    if (accounts.isNotEmpty()) {

                        // HACKHACK: Pleroma, remove when search will work normally
                        if (accounts[0].pleroma != null) {
                            val account = accounts.firstOrNull { it.pleroma?.apId == url || it.url == url }

                            if (account != null) {
                                viewAccount(account.id)
                                return@subscribe
                            }
                        } else {
                            viewAccount(accounts[0].id)
                            return@subscribe
                        }
                    }

                    if (statuses.isNotEmpty()) {
                        viewThread(statuses[0].id, statuses[0].url)
                        return@subscribe
                    }

                    performUrlFallbackAction(url, lookupFallbackBehavior)
                }, {
                    if (!getCancelSearchRequested(url)) {
                        onEndSearch(url)
                        performUrlFallbackAction(url, lookupFallbackBehavior)
                    }
                })

        onBeginSearch(url)
    }

    open fun viewThread(statusId: String, url: String?) {
        if (!isSearching()) {
            val intent = ViewThreadActivity.startIntent(this, statusId, url)
            startActivityWithSlideInAnimation(intent)
        }
    }

    open fun viewAccount(id: String) {
        val intent = AccountActivity.getIntent(this, id)
        startActivityWithSlideInAnimation(intent)
    }

    open fun openChat(chat: Chat) {
        startActivityWithSlideInAnimation(ChatActivity.getIntent(this, chat))
    }

    protected open fun performUrlFallbackAction(url: String, fallbackBehavior: PostLookupFallbackBehavior) {
        when (fallbackBehavior) {
            PostLookupFallbackBehavior.OPEN_IN_BROWSER -> openLink(url)
            PostLookupFallbackBehavior.DISPLAY_ERROR -> Toast.makeText(this, getString(R.string.post_lookup_error_format, url), Toast.LENGTH_SHORT).show()
        }
    }

    @VisibleForTesting
    fun onBeginSearch(url: String) {
        searchUrl = url
        showQuerySheet()
    }

    @VisibleForTesting
    fun getCancelSearchRequested(url: String): Boolean {
        return url != searchUrl
    }

    @VisibleForTesting
    fun isSearching(): Boolean {
        return searchUrl != null
    }

    @VisibleForTesting
    fun onEndSearch(url: String?) {
        if (url == searchUrl) {
            // Don't clear query if there's no match,
            // since we might just now be getting the response for a canceled search
            searchUrl = null
            hideQuerySheet()
        }
    }

    @VisibleForTesting
    fun cancelActiveSearch() {
        if (isSearching()) {
            onEndSearch(searchUrl)
        }
    }

    @VisibleForTesting
    open fun openLink(url: String) {
        LinkHelper.openLink(url, this)
    }

    private fun showQuerySheet() {
        bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun hideQuerySheet() {
        bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
    }
}

// https://mastodon.foo.bar/@User
// https://mastodon.foo.bar/@User/43456787654678
// https://pleroma.foo.bar/users/User
// https://pleroma.foo.bar/users/9qTHT2ANWUdXzENqC0
// https://pleroma.foo.bar/notice/9sBHWIlwwGZi5QGlHc
// https://pleroma.foo.bar/objects/d4643c42-3ae0-4b73-b8b0-c725f5819207
// https://friendica.foo.bar/profile/user
// https://friendica.foo.bar/display/d4643c42-3ae0-4b73-b8b0-c725f5819207
// https://misskey.foo.bar/notes/83w6r388br (always lowercase)
fun looksLikeMastodonUrl(urlString: String): Boolean {
    val uri: URI
    try {
        uri = URI(urlString)
    } catch (e: URISyntaxException) {
        return false
    }

    if (uri.query != null ||
            uri.fragment != null ||
            uri.path == null) {
        return false
    }

    val path = uri.path
    return path.matches("^/@[^/]+$".toRegex()) ||
            path.matches("^/@[^/]+/\\d+$".toRegex()) ||
            path.matches("^/users/\\w+$".toRegex()) ||
            path.matches("^/notice/[a-zA-Z0-9]+$".toRegex()) ||
            path.matches("^/objects/[-a-f0-9]+$".toRegex()) ||
            path.matches("^/notes/[a-z0-9]+$".toRegex()) ||
            path.matches("^/display/[-a-f0-9]+$".toRegex()) ||
            path.matches("^/profile/\\w+$".toRegex())
}

enum class PostLookupFallbackBehavior {
    OPEN_IN_BROWSER,
    DISPLAY_ERROR,
}
