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

package com.keylesspalace.tusky.settings

enum class AppTheme(val value: String) {
    NIGHT("night"),
    DAY("day"),
    BLACK("black"),
    AUTO("auto"),
    AUTO_SYSTEM("auto_system");

    companion object {
        fun stringValues() = values().map { it.value }.toTypedArray()
    }
}

object PrefKeys {
    // Note: not all of these keys are actually used as SharedPreferences keys but we must give
    // each preference a key for it to work.

    const val APP_THEME = "appTheme"
    const val EMOJI = "selected_emoji_font"
    const val FAB_HIDE = "fabHide"
    const val LANGUAGE = "language"
    const val STATUS_TEXT_SIZE = "statusTextSize"
    const val MAIN_NAV_POSITION = "mainNavPosition"
    const val HIDE_TOP_TOOLBAR = "hideTopToolbar"
    const val ABSOLUTE_TIME_VIEW = "absoluteTimeView"
    const val SHOW_BOT_OVERLAY = "showBotOverlay"
    const val ANIMATE_GIF_AVATARS = "animateGifAvatars"
    const val USE_BLURHASH = "useBlurhash"
    const val SHOW_NOTIFICATIONS_FILTER = "showNotificationsFilter"
    const val SHOW_CARDS_IN_TIMELINES = "showCardsInTimelines"
    const val CONFIRM_REBLOGS = "confirmReblogs"
    const val ENABLE_SWIPE_FOR_TABS = "enableSwipeForTabs"
    const val BIG_EMOJIS = "bigEmojis"
    const val STICKERS = "stickers"
    const val ANONYMIZE_FILENAMES = "anonymizeFilenames"
    const val HIDE_LIVE_NOTIFICATION_DESCRIPTION = "hideLiveNotifDesc"
    const val HIDE_MUTED_USERS = "hideMutedUsers"
    const val ANIMATE_CUSTOM_EMOJIS = "animateCustomEmojis"
    const val RENDER_STATUS_AS_MENTION = "renderStatusAsMention"
    const val COMPOSING_ZWSP_CHAR = "composingZwspChar"

    const val CUSTOM_TABS = "customTabs"
    const val WELLBEING_LIMITED_NOTIFICATIONS = "wellbeingModeLimitedNotifications"
    const val WELLBEING_HIDE_STATS_POSTS = "wellbeingHideStatsPosts"
    const val WELLBEING_HIDE_STATS_PROFILE = "wellbeingHideStatsProfile"

    const val HTTP_PROXY_ENABLED = "httpProxyEnabled"
    const val HTTP_PROXY_SERVER = "httpProxyServer"
    const val HTTP_PROXY_PORT = "httpProxyPort"

    const val DEFAULT_POST_PRIVACY = "defaultPostPrivacy"
    const val DEFAULT_MEDIA_SENSITIVITY = "defaultMediaSensitivity"
    const val DEFAULT_FORMATTING_SYNTAX = "defaultFormattingSyntax"
    const val MEDIA_PREVIEW_ENABLED = "mediaPreviewEnabled"
    const val ALWAYS_SHOW_SENSITIVE_MEDIA = "alwaysShowSensitiveMedia"
    const val ALWAYS_OPEN_SPOILER = "alwaysOpenSpoiler"

    const val LIVE_NOTIFICATIONS = "liveNotifications"

    const val PUSH_NOTIFICATIONS_INFO_DIALOG = "pushNotificationsInfoDialog"
    const val UNIFIEDPUSH_ENROLL_DIALOG = "unifiedPushEnrollDialog"

    const val NOTIFICATIONS_ENABLED = "notificationsEnabled"
    const val NOTIFICATION_ALERT_LIGHT = "notificationAlertLight"
    const val NOTIFICATION_ALERT_VIBRATE = "notificationAlertVibrate"
    const val NOTIFICATION_ALERT_SOUND = "notificationAlertSound"
    const val NOTIFICATION_FILTER_POLLS = "notificationFilterPolls"
    const val NOTIFICATION_FILTER_CHAT_MESSAGES = "notificationFilterChatMessages"
    const val NOTIFICATION_FILTER_FAVS = "notificationFilterFavourites"
    const val NOTIFICATION_FILTER_REBLOGS = "notificationFilterReblogs"
    const val NOTIFICATION_FILTER_FOLLOW_REQUESTS = "notificationFilterFollowRequests"
    const val NOTIFICATION_FILTER_EMOJI_REACTIONS = "notificationFilterEmojis"
    const val NOTIFICATION_FILTER_SUBSCRIPTIONS = "notificationFilterSubscriptions"
    const val NOTIFICATION_FILTER_MOVE = "notificationFilterMove"
    const val NOTIFICATIONS_FILTER_FOLLOWS = "notificationFilterFollows"

    const val TAB_FILTER_HOME_REPLIES = "tabFilterHomeReplies"
    const val TAB_FILTER_HOME_BOOSTS = "tabFilterHomeBoosts"

    const val CRASH_HANDLER_ENABLE = "enableCrashHanlder"
}
