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

package com.keylesspalace.tusky.viewdata;

import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.keylesspalace.tusky.entity.Attachment;
import com.keylesspalace.tusky.entity.Card;
import com.keylesspalace.tusky.entity.Emoji;
import com.keylesspalace.tusky.entity.EmojiReaction;
import com.keylesspalace.tusky.entity.Poll;
import com.keylesspalace.tusky.entity.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Class to represent data required to display either a notification or a placeholder. It is either
 * a {@link StatusViewData.Concrete} or a {@link StatusViewData.Placeholder}.
 */
public abstract class StatusViewData {

    private StatusViewData() {
    }

    public abstract long getViewDataId();

    public abstract boolean deepEquals(StatusViewData other);

    public static final class Concrete extends StatusViewData {
        private static final char SOFT_HYPHEN = '\u00ad';
        private static final char ASCII_HYPHEN = '-';

        private final String id;
        private final Spanned content;
        final boolean reblogged;
        final boolean favourited;
        final boolean bookmarked;
        @Nullable
        private final String spoilerText;
        private final Status.Visibility visibility;
        private final List<Attachment> attachments;
        @Nullable
        private final String rebloggedByUsername;
        @Nullable
        private final String rebloggedAvatar;
        private final boolean isSensitive;
        final boolean isExpanded;
        private final boolean isShowingContent;
        private final String userFullName;
        private final String nickname;
        private final String avatar;
        private final Date createdAt;
        private final Date editedAt;
        private final int reblogsCount;
        private final int favouritesCount;
        @Nullable
        private final String inReplyToId;
        @Nullable
        private final String inReplyToAccountAcct;
        // I would rather have something else but it would be too much of a rewrite
        @Nullable
        private final Status.Mention[] mentions;
        private final String senderId;
        private final boolean rebloggingEnabled;
        private final Status.Application application;
        private final List<Emoji> statusEmojis;
        private final List<Emoji> accountEmojis;
        private final List<Emoji> rebloggedByAccountEmojis;
        @Nullable
        private final Card card;
        private final boolean isCollapsible;
        /**
         * Whether the status meets the requirement to be collapse
         */
        final boolean isCollapsed;
        /**
         * Whether the status is shown partially or fully
         */
        @Nullable
        private final PollViewData poll;
        private final boolean isBot;
        private final boolean isMuted; /* user toggle */
        private final boolean isThreadMuted; /* thread_muted state got from backend */
        private final boolean isUserMuted; /* muted state got from backend */
        private final String conversationId;
        @Nullable
        private final List<EmojiReaction> emojiReactions;
        private final boolean parentVisible;
        private final Spanned quote;
        private final List<Emoji> quoteEmojis;
        private final String quoteFullName;
        private final String quoteUsername;
        private final List<Emoji> quotedAccountEmojis;
        private final String quotedStatusId;
        private final String quotedStatusUrl;

        public Concrete(
                String id,
                Spanned content,
                boolean reblogged,
                boolean favourited,
                boolean bookmarked,
                @Nullable String spoilerText,
                Status.Visibility visibility,
                List<Attachment> attachments,
                @Nullable String rebloggedByUsername,
                @Nullable String rebloggedAvatar,
                boolean sensitive,
                boolean isExpanded,
                boolean isShowingContent,
                String userFullName,
                String nickname,
                String avatar,
                Date createdAt,
                Date editedAt,
                int reblogsCount,
                int favouritesCount,
                @Nullable String inReplyToId,
                @Nullable String inReplyToAccountAcct,
                @Nullable Status.Mention[] mentions,
                String senderId,
                boolean rebloggingEnabled,
                Status.Application application,
                List<Emoji> statusEmojis,
                List<Emoji> accountEmojis,
                List<Emoji> rebloggedByAccountEmojis,
                @Nullable Card card,
                boolean isCollapsible,
                boolean isCollapsed,
                @Nullable PollViewData poll,
                boolean isBot,
                boolean isMuted,
                boolean isThreadMuted,
                boolean isUserMuted,
                String conversationId,
                @Nullable List<EmojiReaction> emojiReactions,
                boolean parentVisible,
                @Nullable Spanned quote,
                List<Emoji> quoteEmojis,
                String quoteFullName,
                String quoteUsername,
                List<Emoji> quotedAccountEmojis,
                String quotedStatusId,
                String quotedStatusUrl
        ) {
            this.id = id;
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                // https://github.com/tuskyapp/Tusky/issues/563
                this.content = replaceCrashingCharacters(content);
                this.spoilerText = spoilerText == null ? null : replaceCrashingCharacters(
                        spoilerText).toString();
                CharSequence nicknameReplaced = replaceCrashingCharacters(nickname);
                if (nicknameReplaced != null) {
                    this.nickname = nicknameReplaced.toString();
                } else {
                    this.nickname = null;
                }
            } else {
                this.content = content;
                this.spoilerText = spoilerText;
                this.nickname = nickname;
            }
            this.reblogged = reblogged;
            this.favourited = favourited;
            this.bookmarked = bookmarked;
            this.visibility = visibility;
            this.attachments = attachments;
            this.rebloggedByUsername = rebloggedByUsername;
            this.rebloggedAvatar = rebloggedAvatar;
            this.isSensitive = sensitive;
            this.isExpanded = isExpanded;
            this.isShowingContent = isShowingContent;
            this.userFullName = userFullName;
            this.avatar = avatar;
            this.createdAt = createdAt;
            this.editedAt = editedAt;
            this.reblogsCount = reblogsCount;
            this.favouritesCount = favouritesCount;
            this.inReplyToId = inReplyToId;
            this.inReplyToAccountAcct = inReplyToAccountAcct;
            this.mentions = mentions;
            this.senderId = senderId;
            this.rebloggingEnabled = rebloggingEnabled;
            this.application = application;
            this.statusEmojis = statusEmojis;
            this.accountEmojis = accountEmojis;
            this.rebloggedByAccountEmojis = rebloggedByAccountEmojis;
            this.card = card;
            this.isCollapsible = isCollapsible;
            this.isCollapsed = isCollapsed;
            this.poll = poll;
            this.isBot = isBot;
            this.isMuted = isMuted;
            this.isThreadMuted = isThreadMuted;
            this.isUserMuted = isUserMuted;
            this.conversationId = conversationId;
            this.emojiReactions = emojiReactions;
            this.parentVisible = parentVisible;
            this.quote = quote;
            this.quoteEmojis = quoteEmojis;
            this.quoteFullName = quoteFullName;
            this.quoteUsername = quoteUsername;
            this.quotedAccountEmojis = quotedAccountEmojis;
            this.quotedStatusId = quotedStatusId;
            this.quotedStatusUrl = quotedStatusUrl;
        }

        public String getId() {
            return id;
        }

        public Spanned getContent() {
            return content;
        }

        public boolean isReblogged() {
            return reblogged;
        }

        public boolean isFavourited() {
            return favourited;
        }

        public boolean isBookmarked() {
            return bookmarked;
        }

        @Nullable
        public String getSpoilerText() {
            return spoilerText;
        }

        public Status.Visibility getVisibility() {
            return visibility;
        }

        public List<Attachment> getAttachments() {
            return attachments;
        }

        @Nullable
        public String getRebloggedByUsername() {
            return rebloggedByUsername;
        }

        public boolean isSensitive() {
            return isSensitive;
        }

        public boolean isExpanded() {
            return isExpanded;
        }

        public boolean isShowingContent() {
            return isShowingContent;
        }

        public boolean isBot() {
            return isBot;
        }

        @Nullable
        public String getRebloggedAvatar() {
            return rebloggedAvatar;
        }

        public String getUserFullName() {
            return Objects.requireNonNullElse(userFullName, "");
        }

        public String getNickname() {
            return nickname;
        }

        public String getAvatar() {
            return avatar;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public Date getEditedAt() {
            return editedAt;
        }

        public int getReblogsCount() {
            return reblogsCount;
        }

        public int getFavouritesCount() {
            return favouritesCount;
        }

        @Nullable
        public String getInReplyToId() {
            return inReplyToId;
        }

        public String getInReplyToAccountAcct() {
            if (inReplyToAccountAcct != null) {
                return inReplyToAccountAcct;
            }
            return "";
        }

        public String getSenderId() {
            return senderId;
        }

        public Boolean getRebloggingEnabled() {
            return rebloggingEnabled;
        }

        @Nullable
        public Status.Mention[] getMentions() {
            return mentions;
        }

        public Status.Application getApplication() {
            return application;
        }

        public List<Emoji> getStatusEmojis() {
            return statusEmojis;
        }

        public List<Emoji> getAccountEmojis() {
            return accountEmojis;
        }

        public boolean getParentVisible() {
            return parentVisible;
        }

        public List<Emoji> getRebloggedByAccountEmojis() {
            return rebloggedByAccountEmojis;
        }

        @Nullable
        public Card getCard() {
            return card;
        }

        /**
         * Specifies whether the content of this post is allowed to be collapsed or if it should
         * show all content regardless.
         *
         * @return Whether the post is collapsible or never collapsed.
         */
        public boolean isCollapsible() {
            return isCollapsible;
        }

        /**
         * Specifies whether the content of this post is currently limited in visibility to the
         * first 500 characters or not.
         *
         * @return Whether the post is collapsed or fully expanded.
         */
        public boolean isCollapsed() {
            return isCollapsed;
        }

        @Nullable
        public PollViewData getPoll() {
            return poll;
        }

        @Override
        public long getViewDataId() {
            // Chance of collision is super low and impact of mistake is low as well
            return id.hashCode();
        }

        public boolean isThreadMuted() {
            return isThreadMuted;
        }

        public boolean isMuted() {
            return isMuted;
        }

        public boolean isUserMuted() {
            return isUserMuted;
        }

        @Nullable
        public List<EmojiReaction> getEmojiReactions() {
            return emojiReactions;
        }

        @Nullable
        public Spanned getQuote() {
            return quote;
        }

        @NonNull
        public List<Emoji> getQuoteEmojis() {
            return quoteEmojis;
        }

        public String getQuoteFullName() {
            return quoteFullName;
        }

        public String getQuoteUsername() {
            return quoteUsername;
        }

        @NonNull
        public List<Emoji> getQuotedAccountEmojis() {
            return quotedAccountEmojis;
        }

        public String getQuotedStatusId() {
            return quotedStatusId;
        }

        public String getQuotedStatusUrl() {
            return quotedStatusUrl;
        }

        public boolean deepEquals(StatusViewData o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Concrete concrete = (Concrete) o;
            return reblogged == concrete.reblogged &&
                    favourited == concrete.favourited &&
                    bookmarked == concrete.bookmarked &&
                    isSensitive == concrete.isSensitive &&
                    isExpanded == concrete.isExpanded &&
                    isShowingContent == concrete.isShowingContent &&
                    isBot == concrete.isBot &&
                    reblogsCount == concrete.reblogsCount &&
                    favouritesCount == concrete.favouritesCount &&
                    rebloggingEnabled == concrete.rebloggingEnabled &&
                    Objects.equals(id, concrete.id) &&
                    Objects.equals(content, concrete.content) &&
                    Objects.equals(quote, concrete.quote) &&
                    Objects.equals(spoilerText, concrete.spoilerText) &&
                    visibility == concrete.visibility &&
                    Objects.equals(attachments, concrete.attachments) &&
                    Objects.equals(rebloggedByUsername, concrete.rebloggedByUsername) &&
                    Objects.equals(rebloggedAvatar, concrete.rebloggedAvatar) &&
                    Objects.equals(userFullName, concrete.userFullName) &&
                    Objects.equals(nickname, concrete.nickname) &&
                    Objects.equals(avatar, concrete.avatar) &&
                    Objects.equals(createdAt, concrete.createdAt) &&
                    Objects.equals(editedAt, concrete.editedAt) &&
                    Objects.equals(inReplyToId, concrete.inReplyToId) &&
                    Objects.equals(inReplyToAccountAcct, concrete.inReplyToAccountAcct) &&
                    Arrays.equals(mentions, concrete.mentions) &&
                    Objects.equals(senderId, concrete.senderId) &&
                    Objects.equals(application, concrete.application) &&
                    Objects.equals(statusEmojis, concrete.statusEmojis) &&
                    Objects.equals(accountEmojis, concrete.accountEmojis) &&
                    Objects.equals(rebloggedByAccountEmojis, concrete.rebloggedByAccountEmojis) &&
                    Objects.equals(card, concrete.card) &&
                    Objects.equals(poll, concrete.poll) &&
                    isCollapsed == concrete.isCollapsed &&
                    isMuted == concrete.isMuted &&
                    isThreadMuted == concrete.isThreadMuted &&
                    isUserMuted == concrete.isUserMuted &&
                    conversationId == concrete.conversationId &&
                    Objects.equals(emojiReactions, concrete.emojiReactions) &&
                    parentVisible == concrete.parentVisible &&
                    Objects.equals(quoteEmojis, concrete.quoteEmojis) &&
                    Objects.equals(quotedStatusId, concrete.quotedStatusId) &&
                    Objects.equals(quotedStatusUrl, concrete.quotedStatusUrl);
        }

        static Spanned replaceCrashingCharacters(Spanned content) {
            return (Spanned) replaceCrashingCharacters((CharSequence) content);
        }

        @Nullable
        static CharSequence replaceCrashingCharacters(CharSequence content) {
            if (content == null) {
                return null;
            }

            boolean replacing = false;
            SpannableStringBuilder builder = null;
            int length = content.length();

            for (int index = 0; index < length; ++index) {
                char character = content.charAt(index);

                // If there are more than one or two, switch to a map
                if (character == SOFT_HYPHEN) {
                    if (!replacing) {
                        replacing = true;
                        builder = new SpannableStringBuilder(content, 0, index);
                    }
                    builder.append(ASCII_HYPHEN);
                } else if (replacing) {
                    builder.append(character);
                }
            }

            return replacing ? builder : content;
        }
    }

    public static final class Placeholder extends StatusViewData {
        private final boolean isLoading;
        private final String id;

        public Placeholder(String id, boolean isLoading) {
            this.id = id;
            this.isLoading = isLoading;
        }

        public boolean isLoading() {
            return isLoading;
        }

        public String getId() {
            return id;
        }

        @Override
        public long getViewDataId() {
            return id.hashCode();
        }

        @Override
        public boolean deepEquals(StatusViewData other) {
            if (!(other instanceof Placeholder)) {
                return false;
            }
            Placeholder that = (Placeholder) other;
            return isLoading == that.isLoading && id.equals(that.id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Placeholder that = (Placeholder) o;

            return deepEquals(that);
        }

        @Override
        public int hashCode() {
            int result = (isLoading ? 1 : 0);
            result = 31 * result + id.hashCode();
            return result;
        }
    }

    public static class Builder {
        private String id;
        private Spanned content;
        private boolean reblogged;
        private boolean favourited;
        private boolean bookmarked;
        private String spoilerText;
        private Status.Visibility visibility;
        private List<Attachment> attachments;
        private String rebloggedByUsername;
        private String rebloggedAvatar;
        private boolean isSensitive;
        private boolean isExpanded;
        private boolean isShowingContent;
        private String userFullName;
        private String nickname;
        private String avatar;
        private Date createdAt;
        private Date editedAt;
        private int reblogsCount;
        private int favouritesCount;
        private String inReplyToId;
        private String inReplyToAccountAcct;
        private Status.Mention[] mentions;
        private String senderId;
        private boolean rebloggingEnabled;
        private Status.Application application;
        private List<Emoji> statusEmojis;
        private List<Emoji> accountEmojis;
        private List<Emoji> rebloggedByAccountEmojis;
        private Card card;
        private boolean isCollapsible;
        /**
         * Whether the status meets the requirement to be collapsed
         */
        private boolean isCollapsed;
        /**
         * Whether the status is shown partially or fully
         */
        private PollViewData poll;
        private boolean isBot;
        private boolean isMuted;
        private boolean isThreadMuted;
        private boolean isUserMuted;
        private String conversationId;
        private List<EmojiReaction> emojiReactions;
        private boolean parentVisible;
        private Spanned quote;
        private List<Emoji> quoteEmojis;
        private String quoteFullName;
        private String quoteUsername;
        private List<Emoji> quotedAccountEmojis;
        private String quotedStatusId;
        private String quotedStatusUrl;

        public Builder() {
        }

        public Builder(final StatusViewData.Concrete viewData) {
            id = viewData.id;
            content = viewData.content;
            reblogged = viewData.reblogged;
            favourited = viewData.favourited;
            bookmarked = viewData.bookmarked;
            spoilerText = viewData.spoilerText;
            visibility = viewData.visibility;
            attachments =
                    viewData.attachments == null ? null : new ArrayList<>(viewData.attachments);
            rebloggedByUsername = viewData.rebloggedByUsername;
            rebloggedAvatar = viewData.rebloggedAvatar;
            isSensitive = viewData.isSensitive;
            isExpanded = viewData.isExpanded;
            isShowingContent = viewData.isShowingContent;
            userFullName = viewData.userFullName;
            nickname = viewData.nickname;
            avatar = viewData.avatar;
            createdAt = new Date(viewData.createdAt.getTime());
            editedAt = viewData.editedAt == null ? null : new Date(viewData.editedAt.getTime());
            reblogsCount = viewData.reblogsCount;
            favouritesCount = viewData.favouritesCount;
            inReplyToId = viewData.inReplyToId;
            inReplyToAccountAcct = viewData.inReplyToAccountAcct;
            mentions = viewData.mentions == null ? null : viewData.mentions.clone();
            senderId = viewData.senderId;
            rebloggingEnabled = viewData.rebloggingEnabled;
            application = viewData.application;
            statusEmojis = viewData.getStatusEmojis();
            accountEmojis = viewData.getAccountEmojis();
            card = viewData.getCard();
            isCollapsible = viewData.isCollapsible();
            isCollapsed = viewData.isCollapsed();
            poll = viewData.poll;
            isBot = viewData.isBot();
            isMuted = viewData.isMuted;
            isThreadMuted = viewData.isThreadMuted;
            isUserMuted = viewData.isUserMuted;
            emojiReactions = viewData.emojiReactions;
            parentVisible = viewData.parentVisible;
            quote = viewData.quote;
            quoteEmojis = viewData.quoteEmojis;
            quoteFullName = viewData.quoteFullName;
            quoteUsername = viewData.quoteUsername;
            quotedAccountEmojis = viewData.quotedAccountEmojis;
            quotedStatusId = viewData.quotedStatusId;
            quotedStatusUrl = viewData.quotedStatusUrl;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setContent(Spanned content) {
            this.content = content;
            return this;
        }

        public Builder setReblogged(boolean reblogged) {
            this.reblogged = reblogged;
            return this;
        }

        public Builder setFavourited(boolean favourited) {
            this.favourited = favourited;
            return this;
        }

        public Builder setBookmarked(boolean bookmarked) {
            this.bookmarked = bookmarked;
            return this;
        }

        public Builder setSpoilerText(String spoilerText) {
            this.spoilerText = spoilerText;
            return this;
        }

        public Builder setVisibility(Status.Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder setAttachments(List<Attachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Builder setRebloggedByUsername(String rebloggedByUsername) {
            this.rebloggedByUsername = rebloggedByUsername;
            return this;
        }

        public Builder setRebloggedAvatar(String rebloggedAvatar) {
            this.rebloggedAvatar = rebloggedAvatar;
            return this;
        }

        public Builder setSensitive(boolean sensitive) {
            this.isSensitive = sensitive;
            return this;
        }

        public Builder setIsExpanded(boolean isExpanded) {
            this.isExpanded = isExpanded;
            return this;
        }

        public Builder setIsShowingSensitiveContent(boolean isShowingSensitiveContent) {
            this.isShowingContent = isShowingSensitiveContent;
            return this;
        }

        public Builder setIsBot(boolean isBot) {
            this.isBot = isBot;
            return this;
        }

        public Builder setUserFullName(String userFullName) {
            this.userFullName = userFullName;
            return this;
        }

        public Builder setNickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public Builder setAvatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public Builder setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setEditedAt(Date editedAt) {
            this.editedAt = editedAt;
            return this;
        }

        public Builder setReblogsCount(int reblogsCount) {
            this.reblogsCount = reblogsCount;
            return this;
        }

        public Builder setFavouritesCount(int favouritesCount) {
            this.favouritesCount = favouritesCount;
            return this;
        }

        public Builder setInReplyToId(String inReplyToId) {
            this.inReplyToId = inReplyToId;
            return this;
        }

        public Builder setInReplyToAccountAcct(String inReplyToAccountAcct) {
            this.inReplyToAccountAcct = inReplyToAccountAcct;
            return this;
        }

        public Builder setMentions(Status.Mention[] mentions) {
            this.mentions = mentions;
            return this;
        }

        public Builder setSenderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder setRebloggingEnabled(boolean rebloggingEnabled) {
            this.rebloggingEnabled = rebloggingEnabled;
            return this;
        }

        public Builder setApplication(Status.Application application) {
            this.application = application;
            return this;
        }

        public Builder setStatusEmojis(List<Emoji> emojis) {
            this.statusEmojis = emojis;
            return this;
        }

        public Builder setAccountEmojis(List<Emoji> emojis) {
            this.accountEmojis = emojis;
            return this;
        }

        public Builder setParentVisible(boolean parentVisible) {
            this.parentVisible = parentVisible;
            return this;
        }

        public Builder setRebloggedByEmojis(List<Emoji> emojis) {
            this.rebloggedByAccountEmojis = emojis;
            return this;
        }

        public Builder setCard(Card card) {
            this.card = card;
            return this;
        }

        /**
         * Configure the {@link com.keylesspalace.tusky.viewdata.StatusViewData} to support
         * collapsing its content limiting the visible length when collapsed at 500 characters,
         *
         * @param collapsible Whether the status should support being collapsed or not.
         *
         * @return This {@link com.keylesspalace.tusky.viewdata.StatusViewData.Builder} instance.
         */
        public Builder setCollapsible(boolean collapsible) {
            isCollapsible = collapsible;
            return this;
        }

        /**
         * Configure the {@link com.keylesspalace.tusky.viewdata.StatusViewData} to start in a
         * collapsed state, hiding partially the content of the post if it exceeds a certain amount
         * of characters.
         *
         * @param collapsed Whether to show the full content of the status or not.
         *
         * @return This {@link com.keylesspalace.tusky.viewdata.StatusViewData.Builder} instance.
         */
        public Builder setCollapsed(boolean collapsed) {
            isCollapsed = collapsed;
            return this;
        }

        public Builder setPoll(Poll poll) {
            this.poll = PollViewDataKt.toViewData(poll);
            return this;
        }

        public Builder setMuted(Boolean isMuted) {
            this.isMuted = isMuted;
            return this;
        }

        public Builder setUserMuted(Boolean isUserMuted) {
            this.isUserMuted = isUserMuted;
            return this;
        }

        public Builder setThreadMuted(Boolean isThreadMuted) {
            this.isThreadMuted = isThreadMuted;
            return this;
        }

        public Builder setConversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder setEmojiReactions(List<EmojiReaction> emojiReactions) {
            this.emojiReactions = emojiReactions;
            return this;
        }

        public Builder setQuote(@Nullable Spanned quote) {
            this.quote = quote;
            return this;
        }

        public Builder setQuoteEmojis(List<Emoji> quoteEmojis) {
            this.quoteEmojis = quoteEmojis;
            return this;
        }

        public Builder setQuoteFullName(String quoteFullName) {
            this.quoteFullName = quoteFullName;
            return this;
        }

        public Builder setQuoteUsername(String quoteUsername) {
            this.quoteUsername = quoteUsername;
            return this;
        }

        public Builder setQuotedAccountEmojis(List<Emoji> quotedAccountEmojis) {
            this.quotedAccountEmojis = quotedAccountEmojis;
            return this;
        }

        public Builder setQuotedStatusId(String quotedStatusId) {
            this.quotedStatusId = quotedStatusId;
            return this;
        }

        public Builder setQuotedStatusUrl(String quotedStatusUrl) {
            this.quotedStatusUrl = quotedStatusUrl;
            return this;
        }

        public StatusViewData.Concrete createStatusViewData() {
            if (this.statusEmojis == null) {
                statusEmojis = Collections.emptyList();
            }
            if (this.accountEmojis == null) {
                accountEmojis = Collections.emptyList();
            }
            if (this.createdAt == null) {
                createdAt = new Date();
            }

            return new StatusViewData.Concrete(id,
                    content,
                    reblogged,
                    favourited,
                    bookmarked,
                    spoilerText,
                    visibility,
                    attachments,
                    rebloggedByUsername,
                    rebloggedAvatar,
                    isSensitive,
                    isExpanded,
                    isShowingContent,
                    userFullName,
                    nickname,
                    avatar,
                    createdAt,
                    editedAt,
                    reblogsCount,
                    favouritesCount,
                    inReplyToId,
                    inReplyToAccountAcct,
                    mentions,
                    senderId,
                    rebloggingEnabled,
                    application,
                    statusEmojis,
                    accountEmojis,
                    rebloggedByAccountEmojis,
                    card,
                    isCollapsible,
                    isCollapsed,
                    poll,
                    isBot,
                    isMuted,
                    isThreadMuted,
                    isUserMuted,
                    conversationId,
                    emojiReactions,
                    parentVisible,
                    quote,
                    quoteEmojis,
                    quoteFullName,
                    quoteUsername,
                    quotedAccountEmojis,
                    quotedStatusId,
                    quotedStatusUrl
            );
        }
    }
}
