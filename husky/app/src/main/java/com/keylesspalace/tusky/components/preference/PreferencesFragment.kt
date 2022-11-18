/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
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

package com.keylesspalace.tusky.components.preference

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.core.logging.CrashHandler
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.di.Injectable
import com.keylesspalace.tusky.entity.Notification
import com.keylesspalace.tusky.settings.AppTheme
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.settings.emojiPreference
import com.keylesspalace.tusky.settings.listPreference
import com.keylesspalace.tusky.settings.makePreferenceScreen
import com.keylesspalace.tusky.settings.preference
import com.keylesspalace.tusky.settings.preferenceCategory
import com.keylesspalace.tusky.settings.switchPreference
import com.keylesspalace.tusky.util.ThemeUtils
import com.keylesspalace.tusky.util.deserialize
import com.keylesspalace.tusky.util.getNonNullString
import com.keylesspalace.tusky.util.serialize
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import okhttp3.OkHttpClient
import javax.inject.Inject

class PreferencesFragment : PreferenceFragmentCompat(), Injectable {

    @Inject
    lateinit var okhttpclient: OkHttpClient

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var eventHub: EventHub

    @Inject
    lateinit var crashHandler: CrashHandler

    private val iconSize by lazy { resources.getDimensionPixelSize(R.dimen.preference_icon_size) }
    private var httpProxyPref: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        makePreferenceScreen {
            preferenceCategory(R.string.pref_title_appearance_settings) {
                listPreference {
                    setDefaultValue(AppTheme.NIGHT.value)
                    setEntries(R.array.app_theme_names)
                    entryValues = AppTheme.stringValues()
                    key = PrefKeys.APP_THEME
                    setSummaryProvider { entry }
                    setTitle(R.string.pref_title_app_theme)
                    icon = makeIcon(GoogleMaterial.Icon.gmd_palette)
                }

                emojiPreference(okhttpclient) {
                    setDefaultValue("system_default")
                    setIcon(R.drawable.ic_emoji_24dp)
                    key = PrefKeys.EMOJI
                    setSummary(R.string.system_default)
                    setTitle(R.string.emoji_style)
                    icon = makeIcon(GoogleMaterial.Icon.gmd_sentiment_satisfied)
                }

                listPreference {
                    setDefaultValue("default")
                    setEntries(R.array.language_entries)
                    setEntryValues(R.array.language_values)
                    key = PrefKeys.LANGUAGE
                    setSummaryProvider { entry }
                    setTitle(R.string.pref_title_language)
                    icon = makeIcon(GoogleMaterial.Icon.gmd_translate)
                }

                listPreference {
                    setDefaultValue("medium")
                    setEntries(R.array.status_text_size_names)
                    setEntryValues(R.array.status_text_size_values)
                    key = PrefKeys.STATUS_TEXT_SIZE
                    setSummaryProvider { entry }
                    setTitle(R.string.pref_status_text_size)
                    icon = makeIcon(GoogleMaterial.Icon.gmd_format_size)
                }

                listPreference {
                    setDefaultValue("top")
                    setEntries(R.array.pref_main_nav_position_options)
                    setEntryValues(R.array.pref_main_nav_position_values)
                    key = PrefKeys.MAIN_NAV_POSITION
                    setSummaryProvider { entry }
                    setTitle(R.string.pref_main_nav_position)
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.HIDE_TOP_TOOLBAR
                    setTitle(R.string.pref_title_hide_top_toolbar)
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.FAB_HIDE
                    setTitle(R.string.pref_title_hide_follow_button)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.ABSOLUTE_TIME_VIEW
                    setTitle(R.string.pref_title_absolute_time)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(true)
                    key = PrefKeys.SHOW_BOT_OVERLAY
                    setTitle(R.string.pref_title_bot_overlay)
                    isSingleLineTitle = false
                    setIcon(R.drawable.ic_bot_24dp)
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.ANIMATE_GIF_AVATARS
                    setTitle(R.string.pref_title_animate_gif_avatars)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(true)
                    key = PrefKeys.USE_BLURHASH
                    setTitle(R.string.pref_title_gradient_for_media)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.SHOW_CARDS_IN_TIMELINES
                    setTitle(R.string.pref_title_show_cards_in_timelines)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(true)
                    key = PrefKeys.SHOW_NOTIFICATIONS_FILTER
                    setTitle(R.string.pref_title_show_notifications_filter)
                    isSingleLineTitle = false
                    setOnPreferenceClickListener {
                        activity?.let { activity ->
                            val intent = PreferencesActivity.newIntent(
                                activity,
                                PreferencesActivity.TAB_FILTER_PREFERENCES
                            )
                            activity.startActivity(intent)
                            activity.overridePendingTransition(
                                R.anim.slide_from_right,
                                R.anim.slide_to_left
                            )
                        }
                        true
                    }
                }

                switchPreference {
                    setDefaultValue(true)
                    key = PrefKeys.CONFIRM_REBLOGS
                    setTitle(R.string.pref_title_confirm_reblogs)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.HIDE_MUTED_USERS
                    setTitle(R.string.pref_title_hide_muted_users)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(true)
                    key = PrefKeys.ENABLE_SWIPE_FOR_TABS
                    setTitle(R.string.pref_title_enable_swipe_for_tabs)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(true)
                    key = PrefKeys.BIG_EMOJIS
                    setTitle(R.string.pref_title_enable_big_emojis)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.STICKERS
                    setTitle(R.string.pref_title_enable_experimental_stickers)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.ANIMATE_CUSTOM_EMOJIS
                    setTitle(R.string.pref_title_animate_custom_emojis)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(true)
                    key = PrefKeys.RENDER_STATUS_AS_MENTION
                    setTitle(R.string.pref_title_render_subscriptions_as_statuses)
                    isSingleLineTitle = true
                }
            }

            preferenceCategory(R.string.pref_title_composing) {
                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.COMPOSING_ZWSP_CHAR
                    setTitle(R.string.pref_title_composing_title)
                    isSingleLineTitle = false
                }
            }

            preferenceCategory(R.string.pref_title_privacy) {
                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.ANONYMIZE_FILENAMES
                    setTitle(R.string.pref_title_anonymize_upload_filenames)
                    isSingleLineTitle = false
                }

                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.HIDE_LIVE_NOTIFICATION_DESCRIPTION
                    setTitle(R.string.pref_title_hide_live_notification_description)
                    isSingleLineTitle = false
                    setOnPreferenceChangeListener { _, _ ->
                        eventHub.dispatch(PreferenceChangedEvent(key))

                        Toast.makeText(
                            context,
                            getString(R.string.pref_title_hide_live_notification_description_toast),
                            Toast.LENGTH_LONG
                        ).show()

                        true
                    }
                }
            }

            preferenceCategory(R.string.pref_title_browser_settings) {
                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.CUSTOM_TABS
                    setTitle(R.string.pref_title_custom_tabs)
                    isSingleLineTitle = false
                }
            }

            preferenceCategory(R.string.pref_title_timeline_filters) {
                preference {
                    setTitle(R.string.pref_title_status_tabs)
                    setOnPreferenceClickListener {
                        activity?.let { activity ->
                            val intent = PreferencesActivity.newIntent(
                                activity,
                                PreferencesActivity.TAB_FILTER_PREFERENCES
                            )
                            activity.startActivity(intent)
                            activity.overridePendingTransition(
                                R.anim.slide_from_right,
                                R.anim.slide_to_left
                            )
                        }
                        true
                    }
                }
            }

            preferenceCategory(R.string.pref_title_wellbeing_mode) {
                switchPreference {
                    title = getString(R.string.limit_notifications)
                    setDefaultValue(false)
                    key = PrefKeys.WELLBEING_LIMITED_NOTIFICATIONS
                    setOnPreferenceChangeListener { _, value ->
                        for (account in accountManager.accounts) {
                            val notificationFilter =
                                deserialize(account.notificationsFilter).toMutableSet()

                            if (value == true) {
                                notificationFilter.add(Notification.Type.FAVOURITE)
                                notificationFilter.add(Notification.Type.FOLLOW)
                                notificationFilter.add(Notification.Type.REBLOG)
                            } else {
                                notificationFilter.remove(Notification.Type.FAVOURITE)
                                notificationFilter.remove(Notification.Type.FOLLOW)
                                notificationFilter.remove(Notification.Type.REBLOG)
                            }

                            account.notificationsFilter = serialize(notificationFilter)
                            accountManager.saveAccount(account)
                        }
                        true
                    }
                }

                switchPreference {
                    title = getString(R.string.wellbeing_hide_stats_posts)
                    setDefaultValue(false)
                    key = PrefKeys.WELLBEING_HIDE_STATS_POSTS
                }

                switchPreference {
                    title = getString(R.string.wellbeing_hide_stats_profile)
                    setDefaultValue(false)
                    key = PrefKeys.WELLBEING_HIDE_STATS_PROFILE
                }
            }

            preferenceCategory(R.string.pref_title_proxy_settings) {
                httpProxyPref = preference {
                    setTitle(R.string.pref_title_http_proxy_settings)
                    setOnPreferenceClickListener {
                        activity?.let { activity ->
                            val intent = PreferencesActivity.newIntent(
                                activity,
                                PreferencesActivity.PROXY_PREFERENCES
                            )
                            activity.startActivity(intent)
                            activity.overridePendingTransition(
                                R.anim.slide_from_right,
                                R.anim.slide_to_left
                            )
                        }
                        true
                    }
                }
            }

            preferenceCategory(R.string.pref_crashhandler_category) {
                switchPreference {
                    setDefaultValue(false)
                    key = PrefKeys.CRASH_HANDLER_ENABLE
                    setTitle(R.string.pref_crashhandler_body)
                    isSingleLineTitle = false
                    setOnPreferenceChangeListener { _, value ->
                        with(value as Boolean) {
                            if (this) {
                                crashHandler.setAsDefaultHandler()
                            } else {
                                crashHandler.removeDefaultHandler()
                            }
                        }

                        true
                    }
                }
            }
        }
    }

    private fun makeIcon(icon: GoogleMaterial.Icon): IconicsDrawable {
        val context = requireContext()
        return IconicsDrawable(context, icon).apply {
            sizePx = iconSize
            colorInt = ThemeUtils.getColor(context, R.attr.iconColor)
        }
    }

    override fun onResume() {
        super.onResume()
        updateHttpProxySummary()
    }

    private fun updateHttpProxySummary() {
        val sharedPreferences = preferenceManager.sharedPreferences
        val httpProxyEnabled = sharedPreferences?.getBoolean(PrefKeys.HTTP_PROXY_ENABLED, false) ?: false
        val httpServer = sharedPreferences?.getNonNullString(PrefKeys.HTTP_PROXY_SERVER, "") ?: ""

        try {
            val httpPort = sharedPreferences?.getNonNullString(PrefKeys.HTTP_PROXY_PORT, "-1")
                ?.toInt() ?: -1

            if (httpProxyEnabled && httpServer.isNotBlank() && httpPort > 0 && httpPort < 65535) {
                httpProxyPref?.summary = "$httpServer:$httpPort"
                return
            }
        } catch (e: NumberFormatException) {
            // user has entered wrong port, fall back to empty summary
        }

        httpProxyPref?.summary = ""
    }

    companion object {
        fun newInstance(): PreferencesFragment {
            return PreferencesFragment()
        }
    }
}
