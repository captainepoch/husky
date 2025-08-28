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

package com.keylesspalace.tusky.util;

import android.text.Spanned;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.keylesspalace.tusky.entity.Notification;
import com.keylesspalace.tusky.entity.Quote;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.entity.Chat;
import com.keylesspalace.tusky.entity.ChatMessage;
import com.keylesspalace.tusky.viewdata.NotificationViewData;
import com.keylesspalace.tusky.viewdata.StatusViewData;
import com.keylesspalace.tusky.viewdata.ChatViewData;
import com.keylesspalace.tusky.viewdata.ChatMessageViewData;
import com.keylesspalace.tusky.viewdata.StatusViewData.Builder;

public final class ViewDataUtils {
    @Nullable
    public static StatusViewData.Concrete statusToViewData(
            @Nullable Status status,
            boolean alwaysShowSensitiveMedia,
            boolean alwaysOpenSpoiler
    ) {
        if (status == null) {
            return null;
        }
        Status visibleStatus = (status.getReblog() == null ? status : status.getReblog());
        Spanned statusContent = visibleStatus.getContent();
        StatusViewData.Builder newStatus = new Builder();
        newStatus.setId(status.getId())
                 .setAttachments(visibleStatus.getAttachments())
                 .setAvatar(visibleStatus.getAccount().getAvatar())
                 .setContent(statusContent)
                 .setCreatedAt(visibleStatus.getCreatedAt())
                 .setEditedAt(visibleStatus.getEditedAt())
                 .setReblogsCount(visibleStatus.getReblogsCount())
                 .setFavouritesCount(visibleStatus.getFavouritesCount())
                 .setInReplyToId(visibleStatus.getInReplyToId())
                 .setInReplyToAccountAcct(visibleStatus.getInReplyToAccountAcct())
                 .setFavourited(visibleStatus.getFavourited())
                 .setBookmarked(visibleStatus.getBookmarked())
                 .setReblogged(visibleStatus.getReblogged())
                 .setIsExpanded(alwaysOpenSpoiler)
                 .setIsShowingSensitiveContent(false)
                 .setMentions(visibleStatus.getMentions())
                 .setNickname(visibleStatus.getAccount().getUsername())
                 .setRebloggedAvatar(
                         status.getReblog() == null ? null : status.getAccount().getAvatar())
                 .setSensitive(visibleStatus.getSensitive())
                 .setIsShowingSensitiveContent(
                         alwaysShowSensitiveMedia || !visibleStatus.getSensitive())
                 .setSpoilerText(visibleStatus.getSpoilerText())
                 .setRebloggedByUsername(
                         status.getReblog() == null ? null : status.getAccount().getDisplayName())
                 .setUserFullName(visibleStatus.getAccount().getName())
                 .setVisibility(visibleStatus.getVisibility())
                 .setSenderId(visibleStatus.getAccount().getId())
                 .setRebloggingEnabled(visibleStatus.rebloggingAllowed())
                 .setApplication(visibleStatus.getApplication())
                 .setStatusEmojis(visibleStatus.getEmojis())
                 .setAccountEmojis(visibleStatus.getAccount().getEmojis())
                 .setRebloggedByEmojis(
                         status.getReblog() == null ? null : status.getAccount().getEmojis());

        // Needed in some cases
        if (statusContent != null) {
            newStatus.setCollapsible(SmartLengthInputFilterKt.shouldTrimStatus(statusContent));
        }

        newStatus.setCollapsed(true)
                 .setPoll(visibleStatus.getPoll())
                 .setCard(visibleStatus.getCard())
                 .setIsBot(visibleStatus.getAccount().getBot())
                 .setMuted(visibleStatus.isMuted())
                 .setUserMuted(visibleStatus.isUserMuted())
                 .setThreadMuted(visibleStatus.isThreadMuted())
                 .setConversationId(visibleStatus.getConversationId())
                 .setEmojiReactions(visibleStatus.getEmojiReactions())
                 .setParentVisible(visibleStatus.getParentVisible())
                 .createStatusViewData();

        Quote quote = null;
        if (visibleStatus.getPleroma() != null && visibleStatus.getPleroma().getQuote() != null) {
            quote = visibleStatus.getPleroma().getQuote();
        } else if (visibleStatus.getQuote() != null) {
            quote = visibleStatus.getQuote();
        }

        if (quote != null) {
            if (quote.getContent() != null) {
                newStatus = newStatus.setQuote(quote.getContent());
            }
            if (quote.getQuoteEmojis() != null) {
                newStatus = newStatus.setQuoteEmojis(quote.getQuoteEmojis());
            }

            if (quote.getAccount() != null) {
                newStatus = newStatus.setQuoteFullName(quote.getAccount().getDisplayName())
                    .setQuoteUsername(quote.getAccount().getUsername());

                if (quote.getAccount().getEmojis() != null) {
                    newStatus = newStatus.setQuotedAccountEmojis(
                        quote.getAccount().getEmojis()
                    );
                }
            }

            newStatus = newStatus.setQuotedStatusId(
                quote.getQuotedStatusId()
            ).setQuotedStatusUrl(
                quote.getQuotedStatusUrl()
            );
        }

        return newStatus.createStatusViewData();
    }

    public static NotificationViewData.Concrete notificationToViewData(
            Notification notification,
            boolean alwaysShowSensitiveData,
            boolean alwaysOpenSpoiler
    ) {
        return new NotificationViewData.Concrete(
                notification.getType(),
                notification.getId(),
                notification.getAccount(),
                statusToViewData(
                        notification.getStatus(),
                        alwaysShowSensitiveData,
                        alwaysOpenSpoiler
                ),
                notification.getEmoji(),
                notification.getEmojiUrl(),
                notification.getTarget()
        );
    }

    public static ChatMessageViewData.Concrete chatMessageToViewData(@Nullable ChatMessage msg) {
        if (msg == null) {
            return null;
        }

        return new ChatMessageViewData.Concrete(
                msg.getId(),
                msg.getContent(),
                msg.getChatId(),
                msg.getAccountId(),
                msg.getCreatedAt(),
                msg.getAttachment(),
                msg.getEmojis(),
                msg.getCard()
        );
    }

    @NonNull
    public static ChatViewData.Concrete chatToViewData(Chat chat) {
        return new ChatViewData.Concrete(
                chat.getAccount(),
                chat.getId(),
                chat.getUnread(),
                chatMessageToViewData(
                        chat.getLastMessage()
                ),
                chat.getUpdatedAt()
        );
    }
}
