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

package com.keylesspalace.tusky.components.preference

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.keylesspalace.tusky.AccountListActivity
import com.keylesspalace.tusky.AccountListActivity.Type.BLOCKS
import com.keylesspalace.tusky.AccountListActivity.Type.MUTES
import com.keylesspalace.tusky.BuildConfig
import com.keylesspalace.tusky.FiltersActivity
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.R.anim
import com.keylesspalace.tusky.R.array
import com.keylesspalace.tusky.R.attr
import com.keylesspalace.tusky.R.dimen
import com.keylesspalace.tusky.R.drawable
import com.keylesspalace.tusky.R.string
import com.keylesspalace.tusky.TabPreferenceActivity
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.components.instancemute.InstanceListActivity
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.Filter
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.entity.Status.Visibility
import com.keylesspalace.tusky.entity.Status.Visibility.PUBLIC
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.settings.listPreference
import com.keylesspalace.tusky.settings.makePreferenceScreen
import com.keylesspalace.tusky.settings.preference
import com.keylesspalace.tusky.settings.preferenceCategory
import com.keylesspalace.tusky.settings.switchPreference
import com.keylesspalace.tusky.util.ThemeUtils
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial.Icon.gmd_block
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial.Icon.gmd_notifications
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeRes
import org.koin.android.ext.android.inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AccountPreferencesFragment : PreferenceFragmentCompat() {

    private val accountManager: AccountManager by inject()
    private val mastodonApi: MastodonApi by inject()
    private val eventHub: EventHub by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        makePreferenceScreen {
            preference {
                setTitle(string.pref_title_edit_notification_settings)
                icon = IconicsDrawable(context, gmd_notifications).apply {
                    sizeRes = dimen.preference_icon_size
                    colorInt = ThemeUtils.getColor(context, attr.iconColor)
                }
                setOnPreferenceClickListener {
                    openNotificationPrefs()
                    true
                }
            }

            preference {
                setTitle(string.title_tab_preferences)
                setIcon(drawable.ic_tabs)
                setOnPreferenceClickListener {
                    val intent = Intent(context, TabPreferenceActivity::class.java)
                    activity?.startActivity(intent)
                    activity?.overridePendingTransition(
                        anim.slide_from_right,
                        anim.slide_to_left
                    )
                    true
                }
            }

            preference {
                setTitle(string.action_view_mutes)
                setIcon(drawable.ic_mute_24dp)
                setOnPreferenceClickListener {
                    val intent = Intent(context, AccountListActivity::class.java)
                    intent.putExtra("type", MUTES)
                    activity?.startActivity(intent)
                    activity?.overridePendingTransition(
                        anim.slide_from_right,
                        anim.slide_to_left
                    )
                    true
                }
            }

            preference {
                setTitle(string.action_view_blocks)
                icon = IconicsDrawable(context, gmd_block).apply {
                    sizeRes = dimen.preference_icon_size
                    colorInt = ThemeUtils.getColor(context, attr.iconColor)
                }
                setOnPreferenceClickListener {
                    val intent = Intent(context, AccountListActivity::class.java)
                    intent.putExtra("type", BLOCKS)
                    activity?.startActivity(intent)
                    activity?.overridePendingTransition(
                        anim.slide_from_right,
                        anim.slide_to_left
                    )
                    true
                }
            }

            preference {
                setTitle(string.title_domain_mutes)
                setIcon(drawable.ic_mute_24dp)
                setOnPreferenceClickListener {
                    val intent = Intent(context, InstanceListActivity::class.java)
                    activity?.startActivity(intent)
                    activity?.overridePendingTransition(
                        anim.slide_from_right,
                        anim.slide_to_left
                    )
                    true
                }
            }

            preferenceCategory(string.pref_publishing) {
                listPreference {
                    setTitle(string.pref_default_post_privacy)
                    setEntries(array.post_privacy_names)
                    setEntryValues(array.post_privacy_values)
                    key = PrefKeys.DEFAULT_POST_PRIVACY
                    setSummaryProvider { entry }
                    val visibility = accountManager.activeAccount?.defaultPostPrivacy
                        ?: PUBLIC
                    value = visibility.serverString()
                    setIcon(getIconForVisibility(visibility))
                    setOnPreferenceChangeListener { _, newValue ->
                        setIcon(getIconForVisibility(Visibility.byString(newValue as String)))
                        syncWithServer(visibility = newValue)
                        eventHub.dispatch(PreferenceChangedEvent(key))
                        true
                    }
                }

                listPreference {
                    setTitle(string.pref_title_default_formatting)
                    setEntries(array.formatting_syntax_values)
                    setEntryValues(array.formatting_syntax_values)
                    key = PrefKeys.DEFAULT_FORMATTING_SYNTAX
                    setSummaryProvider { entry }
                    val syntax = accountManager.activeAccount?.defaultFormattingSyntax
                        ?: ""
                    value = when (syntax) {
                        "text/markdown" -> "Markdown"
                        "text/bbcode" -> "BBCode"
                        "text/html" -> "HTML"
                        else -> "Plaintext"
                    }
                    setIcon(getIconForSyntax(syntax))
                    setOnPreferenceChangeListener { _, newValue ->
                        val syntax = when (newValue) {
                            "Markdown" -> "text/markdown"
                            "BBCode" -> "text/bbcode"
                            "HTML" -> "text/html"
                            else -> ""
                        }
                        setIcon(getIconForSyntax(syntax))
                        updateAccount { it.defaultFormattingSyntax = syntax }
                        eventHub.dispatch(PreferenceChangedEvent(key))
                        true
                    }
                }

                switchPreference {
                    setTitle(string.pref_default_media_sensitivity)
                    setIcon(drawable.ic_eye_24dp)
                    key = PrefKeys.DEFAULT_MEDIA_SENSITIVITY
                    isSingleLineTitle = false
                    val sensitivity = accountManager.activeAccount?.defaultMediaSensitivity
                        ?: false
                    setDefaultValue(sensitivity)
                    setIcon(getIconForSensitivity(sensitivity))
                    setOnPreferenceChangeListener { _, newValue ->
                        setIcon(getIconForSensitivity(newValue as Boolean))
                        syncWithServer(sensitive = newValue)
                        eventHub.dispatch(PreferenceChangedEvent(key))
                        true
                    }
                }
            }

            preferenceCategory(string.pref_title_timelines) {
                switchPreference {
                    key = PrefKeys.MEDIA_PREVIEW_ENABLED
                    setTitle(string.pref_title_show_media_preview)
                    isSingleLineTitle = false
                    isChecked = accountManager.activeAccount?.mediaPreviewEnabled ?: true
                    setOnPreferenceChangeListener { _, newValue ->
                        updateAccount { it.mediaPreviewEnabled = newValue as Boolean }
                        eventHub.dispatch(PreferenceChangedEvent(key))
                        true
                    }
                }

                switchPreference {
                    key = PrefKeys.ALWAYS_SHOW_SENSITIVE_MEDIA
                    setTitle(string.pref_title_alway_show_sensitive_media)
                    isSingleLineTitle = false
                    isChecked = accountManager.activeAccount?.alwaysShowSensitiveMedia ?: false
                    setOnPreferenceChangeListener { _, newValue ->
                        updateAccount { it.alwaysShowSensitiveMedia = newValue as Boolean }
                        eventHub.dispatch(PreferenceChangedEvent(key))
                        true
                    }
                }

                switchPreference {
                    key = PrefKeys.ALWAYS_OPEN_SPOILER
                    setTitle(string.pref_title_alway_open_spoiler)
                    isSingleLineTitle = false
                    isChecked = accountManager.activeAccount?.alwaysOpenSpoiler ?: false
                    setOnPreferenceChangeListener { _, newValue ->
                        updateAccount { it.alwaysOpenSpoiler = newValue as Boolean }
                        eventHub.dispatch(PreferenceChangedEvent(key))
                        true
                    }
                }
            }

            preferenceCategory(string.pref_title_other) {
                switchPreference {
                    key = accountManager.pushNotificationPrefKey()
                    setTitle(string.pref_title_live_notifications)
                    setSummary(string.pref_summary_live_notifications)
                    isSingleLineTitle = false
                    isChecked = accountManager.hasNotificationsEnabled()
                    setOnPreferenceChangeListener { _, newValue ->
                        updateAccount { it.notificationsEnabled = newValue as Boolean }
                        eventHub.dispatch(PreferenceChangedEvent(key))
                        true
                    }
                }
            }

            preferenceCategory(string.pref_title_timeline_filters) {
                preference {
                    setTitle(string.pref_title_public_filter_keywords)
                    setOnPreferenceClickListener {
                        launchFilterActivity(
                            Filter.PUBLIC,
                            string.pref_title_public_filter_keywords
                        )
                        true
                    }
                }

                preference {
                    setTitle(string.title_notifications)
                    setOnPreferenceClickListener {
                        launchFilterActivity(Filter.NOTIFICATIONS, string.title_notifications)
                        true
                    }
                }

                preference {
                    setTitle(string.title_home)
                    setOnPreferenceClickListener {
                        launchFilterActivity(Filter.HOME, string.title_home)
                        true
                    }
                }

                preference {
                    setTitle(string.pref_title_thread_filter_keywords)
                    setOnPreferenceClickListener {
                        launchFilterActivity(
                            Filter.THREAD,
                            string.pref_title_thread_filter_keywords
                        )
                        true
                    }
                }

                preference {
                    setTitle(string.title_accounts)
                    setOnPreferenceClickListener {
                        launchFilterActivity(Filter.ACCOUNT, string.title_accounts)
                        true
                    }
                }
            }
        }
    }

    private fun openNotificationPrefs() {
        if (NotificationHelper.NOTIFICATION_USE_CHANNELS) {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("android.provider.extra.APP_PACKAGE", BuildConfig.APPLICATION_ID)
            startActivity(intent)
        } else {
            activity?.let {
                val intent =
                    PreferencesActivity.newIntent(it, PreferencesActivity.NOTIFICATION_PREFERENCES)
                it.startActivity(intent)
                it.overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
            }
        }
    }

    private inline fun updateAccount(changer: (AccountEntity) -> Unit) {
        accountManager.activeAccount?.let { account ->
            changer(account)
            accountManager.saveAccount(account)
        }
    }

    private fun syncWithServer(visibility: String? = null, sensitive: Boolean? = null) {
        mastodonApi.accountUpdateSource(visibility, sensitive)
            .enqueue(object : Callback<Account> {
                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    val account = response.body()
                    if (response.isSuccessful && account != null) {
                        accountManager.activeAccount?.let {
                            it.defaultPostPrivacy = account.source?.privacy
                                ?: Status.Visibility.PUBLIC
                            it.defaultMediaSensitivity = account.source?.sensitive ?: false
                            accountManager.saveAccount(it)
                        }
                    } else {
                        Log.e("AccountPreferences", "failed updating settings on server")
                        showErrorSnackbar(visibility, sensitive)
                    }
                }

                override fun onFailure(call: Call<Account>, t: Throwable) {
                    Log.e("AccountPreferences", "failed updating settings on server", t)
                    showErrorSnackbar(visibility, sensitive)
                }
            })
    }

    private fun showErrorSnackbar(visibility: String?, sensitive: Boolean?) {
        view?.let { view ->
            Snackbar.make(view, R.string.pref_failed_to_sync, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_retry) { syncWithServer(visibility, sensitive) }
                .show()
        }
    }

    @DrawableRes
    private fun getIconForVisibility(visibility: Status.Visibility): Int {
        return when (visibility) {
            Status.Visibility.PRIVATE -> R.drawable.ic_lock_outline_24dp

            Status.Visibility.UNLISTED -> R.drawable.ic_lock_open_24dp

            Status.Visibility.DIRECT -> R.drawable.ic_email_24dp

            Status.Visibility.LOCAL -> R.drawable.ic_local_24dp

            else -> R.drawable.ic_public_24dp
        }
    }

    @DrawableRes
    private fun getIconForSensitivity(sensitive: Boolean): Int {
        return if (sensitive) {
            R.drawable.ic_hide_media_24dp
        } else {
            R.drawable.ic_eye_24dp
        }
    }

    private fun getIconForSyntax(syntax: String): Int {
        return when (syntax) {
            "text/html" -> R.drawable.ic_html_24dp
            "text/bbcode" -> R.drawable.ic_bbcode_24dp
            "text/markdown" -> R.drawable.ic_markdown
            else -> android.R.color.transparent
        }
    }

    private fun launchFilterActivity(filterContext: String, titleResource: Int) {
        val intent = Intent(context, FiltersActivity::class.java)
        intent.putExtra(FiltersActivity.FILTERS_CONTEXT, filterContext)
        intent.putExtra(FiltersActivity.FILTERS_TITLE, getString(titleResource))
        activity?.startActivity(intent)
        activity?.overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
    }

    companion object {
        fun newInstance() = AccountPreferencesFragment()
    }
}
