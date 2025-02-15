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

package com.keylesspalace.tusky.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import at.connyduck.sparkbutton.helpers.Utils;
import com.bumptech.glide.Glide;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.databinding.ItemFollowRequestNotificationBinding;
import com.keylesspalace.tusky.entity.Account;
import com.keylesspalace.tusky.entity.Emoji;
import com.keylesspalace.tusky.entity.Notification;
import com.keylesspalace.tusky.interfaces.AccountActionListener;
import com.keylesspalace.tusky.interfaces.LinkListener;
import com.keylesspalace.tusky.interfaces.StatusActionListener;
import com.keylesspalace.tusky.util.CardViewMode;
import com.keylesspalace.tusky.util.CustomEmojiHelper;
import com.keylesspalace.tusky.util.ImageLoadingHelper;
import com.keylesspalace.tusky.util.LinkHelper;
import com.keylesspalace.tusky.util.SmartLengthInputFilter;
import com.keylesspalace.tusky.util.StatusDisplayOptions;
import com.keylesspalace.tusky.util.StringUtils;
import com.keylesspalace.tusky.util.TimestampUtils;
import com.keylesspalace.tusky.viewdata.NotificationViewData;
import com.keylesspalace.tusky.viewdata.StatusViewData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter {

    public interface AdapterDataSource<T> {
        int getItemCount();

        T getItemAt(int pos);
    }

    private static final int VIEW_TYPE_STATUS = 0;
    private static final int VIEW_TYPE_STATUS_NOTIFICATION = 1;
    private static final int VIEW_TYPE_FOLLOW = 2;
    private static final int VIEW_TYPE_PLACEHOLDER = 3;
    private static final int VIEW_TYPE_MUTED_STATUS = 4;
    private static final int VIEW_TYPE_FOLLOW_REQUEST = 5;
    private static final int VIEW_TYPE_MOVE = 6;
    private static final int VIEW_TYPE_UNKNOWN = 7;

    private static final InputFilter[] COLLAPSE_INPUT_FILTER = new InputFilter[]{SmartLengthInputFilter.INSTANCE};
    private static final InputFilter[] NO_INPUT_FILTER = new InputFilter[0];

    private String accountId;
    private StatusDisplayOptions statusDisplayOptions;
    private StatusActionListener statusListener;
    private NotificationActionListener notificationActionListener;
    private AccountActionListener accountActionListener;
    private AdapterDataSource<NotificationViewData> dataSource;

    public NotificationsAdapter(String accountId,
                                AdapterDataSource<NotificationViewData> dataSource,
                                StatusDisplayOptions statusDisplayOptions,
                                StatusActionListener statusListener,
                                NotificationActionListener notificationActionListener,
                                AccountActionListener accountActionListener) {
        this.accountId = accountId;
        this.dataSource = dataSource;
        this.statusDisplayOptions = statusDisplayOptions;
        this.statusListener = statusListener;
        this.notificationActionListener = notificationActionListener;
        this.accountActionListener = accountActionListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_STATUS: {
                View view = inflater
                        .inflate(R.layout.item_status, parent, false);
                return new StatusViewHolder(view);
            }
            case VIEW_TYPE_MUTED_STATUS: {
                View view = inflater
                        .inflate(R.layout.item_status_muted, parent, false);
                return new MutedStatusViewHolder(view);
            }
            case VIEW_TYPE_STATUS_NOTIFICATION: {
                View view = inflater
                        .inflate(R.layout.item_status_notification, parent, false);
                return new StatusNotificationViewHolder(view, statusDisplayOptions);
            }
            case VIEW_TYPE_MOVE:
            case VIEW_TYPE_FOLLOW: {
                View view = inflater
                        .inflate(R.layout.item_follow, parent, false);
                return new FollowViewHolder(view, statusDisplayOptions);
            }
            case VIEW_TYPE_FOLLOW_REQUEST: {
                ItemFollowRequestNotificationBinding binding =
                    ItemFollowRequestNotificationBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false
                    );
                return new FollowRequestViewHolder(binding, true);
            }
            case VIEW_TYPE_PLACEHOLDER: {
                View view = inflater
                        .inflate(R.layout.item_status_placeholder, parent, false);
                return new PlaceholderViewHolder(view);
            }
            default:
            case VIEW_TYPE_UNKNOWN: {
                View view = new View(parent.getContext());
                view.setLayoutParams(
                        new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                Utils.dpToPx(parent.getContext(), 24)
                        )
                );
                return new RecyclerView.ViewHolder(view) {
                };
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        bindViewHolder(viewHolder, position, null);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position, @NonNull List payloads) {
        bindViewHolder(viewHolder, position, payloads);
    }

    private void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position, @Nullable List payloads) {
        Object payloadForHolder = payloads != null && !payloads.isEmpty() ? payloads.get(0) : null;
        if (position < this.dataSource.getItemCount()) {
            NotificationViewData notification = dataSource.getItemAt(position);
            if (notification instanceof NotificationViewData.Placeholder) {
                if (payloadForHolder == null) {
                    NotificationViewData.Placeholder placeholder = ((NotificationViewData.Placeholder) notification);
                    PlaceholderViewHolder holder = (PlaceholderViewHolder) viewHolder;
                    holder.setup(statusListener, placeholder.isLoading());
                }
                return;
            }
            NotificationViewData.Concrete concreteNotificaton =
                    (NotificationViewData.Concrete) notification;
            switch (viewHolder.getItemViewType()) {
                case VIEW_TYPE_STATUS: {
                    StatusViewHolder holder = (StatusViewHolder) viewHolder;
                    StatusViewData.Concrete status = concreteNotificaton.getStatusViewData();
                    holder.setupWithStatus(status, statusListener, statusDisplayOptions, payloadForHolder);
                    switch (concreteNotificaton.getType()) {
                        case POLL:
                            holder.setPollInfo(accountId.equals(concreteNotificaton.getAccount().getId()));
                            break;
                        case UPDATE:
                            holder.setEditInfo(status.getUserFullName());
                            break;
                        default:
                            holder.hideStatusInfo();
                            break;
                    }
                    break;
                }
                case VIEW_TYPE_MUTED_STATUS: {
                    MutedStatusViewHolder holder = (MutedStatusViewHolder) viewHolder;
                    StatusViewData.Concrete status = concreteNotificaton.getStatusViewData();
                    holder.setupWithStatus(status,
                            statusListener, statusDisplayOptions, payloadForHolder);
                    break;
                }
                case VIEW_TYPE_STATUS_NOTIFICATION: {
                    StatusNotificationViewHolder holder = (StatusNotificationViewHolder) viewHolder;
                    StatusViewData.Concrete statusViewData = concreteNotificaton.getStatusViewData();
                    if (payloadForHolder == null) {
                        if (statusViewData == null) {
                            holder.showNotificationContent(false);
                        } else {
                            holder.showNotificationContent(true);

                            holder.setDisplayName(statusViewData.getUserFullName(), statusViewData.getAccountEmojis());
                            holder.setUsername(statusViewData.getNickname());
                            holder.setCreatedAt(statusViewData.getCreatedAt());

                            if(concreteNotificaton.getType() == Notification.Type.STATUS) {
                                holder.setAvatar(statusViewData.getAvatar(), statusViewData.isBot());
                            } else {
                                holder.setAvatars(statusViewData.getAvatar(),
                                        concreteNotificaton.getAccount().getAvatar());
                            }
                        }

                        holder.setMessage(concreteNotificaton, statusListener);
                        holder.setupButtons(notificationActionListener,
                                concreteNotificaton.getAccount().getId(),
                                concreteNotificaton.getId());
                    } else {
                        if (payloadForHolder instanceof List)
                            for (Object item : (List) payloadForHolder) {
                                if (StatusBaseViewHolder.Key.KEY_CREATED.equals(item) && statusViewData != null) {
                                    holder.setCreatedAt(statusViewData.getCreatedAt());
                                }
                            }
                    }
                    break;
                }
                case VIEW_TYPE_FOLLOW: {
                    if (payloadForHolder == null) {
                        FollowViewHolder holder = (FollowViewHolder) viewHolder;
                        holder.setMessage(concreteNotificaton.getAccount(), null);
                        holder.setupButtons(notificationActionListener, concreteNotificaton.getAccount().getId());
                    }
                    break;
                }
                case VIEW_TYPE_MOVE: {
                    if (payloadForHolder == null) {
                        FollowViewHolder holder = (FollowViewHolder) viewHolder;
                        holder.setMessage(concreteNotificaton.getTarget(), concreteNotificaton.getAccount());
                        holder.setupButtons(notificationActionListener, concreteNotificaton.getAccount().getId());
                    }
                    break;
                }
                case VIEW_TYPE_FOLLOW_REQUEST: {
                    if (payloadForHolder == null) {
                        FollowRequestViewHolder holder = (FollowRequestViewHolder) viewHolder;
                        holder.setupWithAccount(concreteNotificaton.getAccount());
                        holder.setupActionListener(accountActionListener);
                    }
                }

                default:
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataSource.getItemCount();
    }

    public void setMediaPreviewEnabled(boolean mediaPreviewEnabled) {
        this.statusDisplayOptions = statusDisplayOptions.copy(
            statusDisplayOptions.animateAvatars(),
            mediaPreviewEnabled,
            statusDisplayOptions.useAbsoluteTime(),
            statusDisplayOptions.showBotOverlay(),
            statusDisplayOptions.useBlurhash(),
            CardViewMode.NONE,
            statusDisplayOptions.confirmReblogs(),
            statusDisplayOptions.renderStatusAsMention(),
            statusDisplayOptions.hideStats(),
            statusDisplayOptions.canQuotePosts()
        );
    }

    public boolean isMediaPreviewEnabled() {
        return this.statusDisplayOptions.mediaPreviewEnabled();
    }

    @Override
    public int getItemViewType(int position) {
        NotificationViewData notification = dataSource.getItemAt(position);
        if (notification instanceof NotificationViewData.Concrete) {
            NotificationViewData.Concrete concrete = ((NotificationViewData.Concrete) notification);
            switch (concrete.getType()) {
                case MENTION:
                case POLL:
                case UPDATE: {
                    if (concrete.getStatusViewData() != null && concrete.getStatusViewData().isMuted())
                        return VIEW_TYPE_MUTED_STATUS;
                    return VIEW_TYPE_STATUS;
                }
                case STATUS:
                    if (statusDisplayOptions.renderStatusAsMention()) {
                        if (concrete.getStatusViewData() != null && concrete.getStatusViewData().isMuted())
                            return VIEW_TYPE_MUTED_STATUS;
                        return VIEW_TYPE_STATUS;
                    }
                    /* fallthrough */
                case FAVOURITE:
                case REBLOG:
                case EMOJI_REACTION: {
                    return VIEW_TYPE_STATUS_NOTIFICATION;
                }
                case FOLLOW: {
                    return VIEW_TYPE_FOLLOW;
                }
                case FOLLOW_REQUEST: {
                    return VIEW_TYPE_FOLLOW_REQUEST;
                }
                case MOVE: {
                    return VIEW_TYPE_MOVE;
                }
                default: {
                    return VIEW_TYPE_UNKNOWN;
                }
            }
        } else if (notification instanceof NotificationViewData.Placeholder) {
            return VIEW_TYPE_PLACEHOLDER;
        } else {
            throw new AssertionError("Unknown notification type");
        }


    }

    public interface NotificationActionListener {
        void onViewAccount(String id);

        void onViewStatusForNotificationId(String notificationId);

        void onExpandedChange(boolean expanded, int position);

        /**
         * Called when the status {@link android.widget.ToggleButton} responsible for collapsing long
         * status content is interacted with.
         *
         * @param isCollapsed Whether the status content is shown in a collapsed state or fully.
         * @param position    The position of the status in the list.
         */
        void onNotificationContentCollapsedChange(boolean isCollapsed, int position);

        void onViewReplyTo(int position);
    }

    private static class FollowViewHolder extends RecyclerView.ViewHolder {
        private TextView message;
        private TextView usernameView;
        private TextView displayNameView;
        private ImageView avatar;
        private StatusDisplayOptions statusDisplayOptions;

        FollowViewHolder(View itemView, StatusDisplayOptions statusDisplayOptions) {
            super(itemView);
            message = itemView.findViewById(R.id.notification_text);
            usernameView = itemView.findViewById(R.id.notification_username);
            displayNameView = itemView.findViewById(R.id.notification_display_name);
            avatar = itemView.findViewById(R.id.notification_avatar);
            this.statusDisplayOptions = statusDisplayOptions;
        }

        void setMessage(Account account, @Nullable Account from) {
            Context context = message.getContext();

            String wrappedDisplayName = StringUtils.unicodeWrap(account.getName());
            Drawable drawable;
            CharSequence emojifiedMessage;

            if(from != null) {
                String format = context.getString(R.string.notification_move_format);
                String wrappedFromName = StringUtils.unicodeWrap(from.getName());
                String wholeMessage = String.format(format, wrappedFromName);
                emojifiedMessage = CustomEmojiHelper.emojify(wholeMessage, from.getEmojis(), message, true);

                drawable = ContextCompat.getDrawable(context, R.drawable.ic_reply_24dp);
            } else {
                String format = context.getString(R.string.notification_follow_format);
                String wholeMessage = String.format(format, wrappedDisplayName);
                emojifiedMessage = CustomEmojiHelper.emojify(wholeMessage, account.getEmojis(), message, true);

                drawable = ContextCompat.getDrawable(context, R.drawable.ic_person_add_24dp);
            }

            message.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null);

            message.setText(emojifiedMessage);

            String username = context.getString(R.string.status_username_format, account.getUsername());
            usernameView.setText(username);

            CharSequence emojifiedDisplayName = CustomEmojiHelper.emojify(wrappedDisplayName, account.getEmojis(), usernameView, true);

            displayNameView.setText(emojifiedDisplayName);

            int avatarRadius = avatar.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.avatar_radius_42dp);

            ImageLoadingHelper.loadAvatar(account.getAvatar(), avatar, avatarRadius,
                    statusDisplayOptions.animateAvatars());

        }

        void setupButtons(final NotificationActionListener listener, final String accountId) {
            itemView.setOnClickListener(v -> listener.onViewAccount(accountId));
        }
    }


    private static class StatusNotificationViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private final TextView message;
        private final View statusNameBar;
        private final TextView displayName;
        private final TextView username;
        private final TextView timestampInfo;
        private final TextView statusContent;
        private final ImageView statusAvatar;
        private final ImageView notificationAvatar;
        private final TextView replyInfo;
        private final TextView contentWarningDescriptionTextView;
        private final Button contentWarningButton;
        private final Button contentCollapseButton; // TODO: This code SHOULD be based on StatusBaseViewHolder
        private StatusDisplayOptions statusDisplayOptions;

        private String accountId;
        private String notificationId;
        private NotificationActionListener notificationActionListener;
        private StatusViewData.Concrete statusViewData;
        private SimpleDateFormat shortSdf;
        private SimpleDateFormat longSdf;

        private int avatarRadius48dp;
        private int avatarRadius36dp;
        private int avatarRadius24dp;

        StatusNotificationViewHolder(View itemView, StatusDisplayOptions statusDisplayOptions) {
            super(itemView);
            message = itemView.findViewById(R.id.notification_top_text);
            statusNameBar = itemView.findViewById(R.id.status_name_bar);
            displayName = itemView.findViewById(R.id.status_display_name);
            username = itemView.findViewById(R.id.status_username);
            timestampInfo = itemView.findViewById(R.id.status_timestamp_info);
            statusContent = itemView.findViewById(R.id.notification_content);
            statusAvatar = itemView.findViewById(R.id.notification_status_avatar);
            notificationAvatar = itemView.findViewById(R.id.notification_notification_avatar);
            replyInfo = itemView.findViewById(R.id.notification_reply_info);
            contentWarningDescriptionTextView = itemView.findViewById(R.id.notification_content_warning_description);
            contentWarningButton = itemView.findViewById(R.id.notification_content_warning_button);
            contentCollapseButton = itemView.findViewById(R.id.button_toggle_notification_content);
            this.statusDisplayOptions = statusDisplayOptions;

            int darkerFilter = Color.rgb(123, 123, 123);
            statusAvatar.setColorFilter(darkerFilter, PorterDuff.Mode.MULTIPLY);
            notificationAvatar.setColorFilter(darkerFilter, PorterDuff.Mode.MULTIPLY);

            itemView.setOnClickListener(this);
            message.setOnClickListener(this);
            statusContent.setOnClickListener(this);
            shortSdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            longSdf = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());

            this.avatarRadius48dp = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.avatar_radius_48dp);
            this.avatarRadius36dp = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.avatar_radius_36dp);
            this.avatarRadius24dp = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.avatar_radius_24dp);
        }

        private void showNotificationContent(boolean show) {
            statusNameBar.setVisibility(show ? View.VISIBLE : View.GONE);
            contentWarningDescriptionTextView.setVisibility(show ? View.VISIBLE : View.GONE);
            contentWarningButton.setVisibility(show ? View.VISIBLE : View.GONE);
            statusContent.setVisibility(show ? View.VISIBLE : View.GONE);
            statusAvatar.setVisibility(show ? View.VISIBLE : View.GONE);
            replyInfo.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        private void setDisplayName(String name, List<Emoji> emojis) {
            CharSequence emojifiedName = CustomEmojiHelper.emojify(name, emojis, displayName, true);
            displayName.setText(emojifiedName);
        }

        private void setUsername(String name) {
            Context context = username.getContext();
            String format = context.getString(R.string.status_username_format);
            String usernameText = String.format(format, name);
            username.setText(usernameText);
        }

        protected void setCreatedAt(@Nullable Date createdAt) {
            if (statusDisplayOptions.useAbsoluteTime()) {
                String time;
                if (createdAt != null) {
                    if (System.currentTimeMillis() - createdAt.getTime() > 86400000L) {
                        time = longSdf.format(createdAt);
                    } else {
                        time = shortSdf.format(createdAt);
                    }
                } else {
                    time = "??:??:??";
                }
                timestampInfo.setText(time);
            } else {
                // This is the visible timestampInfo.
                String readout;
                /* This one is for screen-readers. Frequently, they would mispronounce timestamps like "17m"
                 * as 17 meters instead of minutes. */
                CharSequence readoutAloud;
                if (createdAt != null) {
                    long then = createdAt.getTime();
                    long now = new Date().getTime();
                    readout = TimestampUtils.getRelativeTimeSpanString(timestampInfo.getContext(), then, now);
                    readoutAloud = android.text.format.DateUtils.getRelativeTimeSpanString(then, now,
                            android.text.format.DateUtils.SECOND_IN_MILLIS,
                            android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE);
                } else {
                    // unknown minutes~
                    readout = "?m";
                    readoutAloud = "? minutes";
                }
                timestampInfo.setText(readout);
                timestampInfo.setContentDescription(readoutAloud);
            }
        }

        void setMessage(NotificationViewData.Concrete notificationViewData, LinkListener listener) {
            this.statusViewData = notificationViewData.getStatusViewData();

            String displayName = StringUtils.unicodeWrap(notificationViewData.getAccount().getName());
            Notification.Type type = notificationViewData.getType();

            Context context = message.getContext();
            Drawable icon;
            SpannableStringBuilder builder = new SpannableStringBuilder();
            switch (type) {
                default:
                case FAVOURITE: {
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_star_24dp);
                    if (icon != null) {
                        icon.setColorFilter(ContextCompat.getColor(context,
                                R.color.tusky_orange), PorterDuff.Mode.SRC_ATOP);
                    }

                    String format = context.getString(R.string.notification_favourite_format);
                    builder.append(String.format(format, displayName));
                    break;
                }
                case REBLOG: {
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_repeat_24dp);
                    if (icon != null) {
                        icon.setColorFilter(ContextCompat.getColor(context,
                                R.color.tusky_blue), PorterDuff.Mode.SRC_ATOP);
                    }

                    String format = context.getString(R.string.notification_reblog_format);
                    builder.append(String.format(format, displayName));
                    break;
                }
                case STATUS: {
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_home_24dp);
                    if (icon != null) {
                        icon.setColorFilter(ContextCompat.getColor(context,
                                R.color.tusky_blue), PorterDuff.Mode.SRC_ATOP);
                    }

                    String format = context.getString(R.string.notification_subscription_format);
                    builder.append(String.format(format, displayName));
                    break;
                }
                case EMOJI_REACTION: {
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_emoji_24dp);
                    if(icon != null) {
                        icon.setColorFilter(ContextCompat.getColor(context,
                            R.color.tusky_green), PorterDuff.Mode.SRC_ATOP);
                    }

                    String format = context.getString(R.string.notification_emoji_format);
                    String emojiCode = notificationViewData.getEmoji();
                    builder.append(String.format(format, displayName, emojiCode));

                    final String emojiUrl = notificationViewData.getEmojiUrl();
                    if(emojiUrl != null) {
                        // terrible hack... ideally, there should be a CharSequence formatter
                        final int emojiPosition = format.indexOf("%s", 1)
                                - "%s".length() + displayName.length();

                        var span = CustomEmojiHelper.createEmojiSpan(emojiUrl, message, true);
                        builder.setSpan(span, emojiPosition, emojiPosition + emojiCode.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    break;
                }
            }
            message.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, displayName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            CharSequence emojifiedText = CustomEmojiHelper.emojify(builder, notificationViewData.getAccount().getEmojis(), message, true);
            message.setText(emojifiedText);

            if (statusViewData != null) {
                boolean hasSpoiler = !TextUtils.isEmpty(statusViewData.getSpoilerText());
                contentWarningDescriptionTextView.setVisibility(hasSpoiler ? View.VISIBLE : View.GONE);
                contentWarningButton.setVisibility(hasSpoiler ? View.VISIBLE : View.GONE);
                if (statusViewData.isExpanded()) {
                    contentWarningButton.setText(R.string.status_content_warning_show_less);
                } else {
                    contentWarningButton.setText(R.string.status_content_warning_show_more);
                }

                contentWarningButton.setOnClickListener(view -> {
                    if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                        notificationActionListener.onExpandedChange(!statusViewData.isExpanded(), getAdapterPosition());
                    }
                    statusContent.setVisibility(statusViewData.isExpanded() ? View.GONE : View.VISIBLE);
                });

                setupContentAndSpoiler(listener);
                setupReplyInfo();
            }

        }

        void setupButtons(final NotificationActionListener listener, final String accountId,
                          final String notificationId) {
            this.notificationActionListener = listener;
            this.accountId = accountId;
            this.notificationId = notificationId;
        }

        void setAvatar(@Nullable String statusAvatarUrl, boolean isBot) {
            statusAvatar.setPaddingRelative(0, 0, 0, 0);

            ImageLoadingHelper.loadAvatar(statusAvatarUrl,
                    statusAvatar, avatarRadius48dp, statusDisplayOptions.animateAvatars());

            if (statusDisplayOptions.showBotOverlay() && isBot) {
                notificationAvatar.setVisibility(View.VISIBLE);
                notificationAvatar.setBackgroundColor(0x50ffffff);
                Glide.with(notificationAvatar)
                        .load(R.drawable.ic_bot_24dp)
                        .into(notificationAvatar);

            } else {
                notificationAvatar.setVisibility(View.GONE);
            }
        }

        void setAvatars(@Nullable String statusAvatarUrl, @Nullable String notificationAvatarUrl) {
            int padding = Utils.dpToPx(statusAvatar.getContext(), 12);
            statusAvatar.setPaddingRelative(0, 0, padding, padding);

            ImageLoadingHelper.loadAvatar(statusAvatarUrl,
                    statusAvatar, avatarRadius36dp, statusDisplayOptions.animateAvatars());

            notificationAvatar.setVisibility(View.VISIBLE);
            ImageLoadingHelper.loadAvatar(notificationAvatarUrl, notificationAvatar,
                avatarRadius24dp, statusDisplayOptions.animateAvatars());
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.notification_container:
                case R.id.notification_content:
                    if (notificationActionListener != null)
                        notificationActionListener.onViewStatusForNotificationId(notificationId);
                    break;
                case R.id.notification_top_text:
                    if (notificationActionListener != null)
                        notificationActionListener.onViewAccount(accountId);
                    break;
            }
        }

        private void setupContentAndSpoiler(final LinkListener listener) {
            boolean shouldShowContentIfSpoiler = statusViewData.isExpanded();
            boolean hasSpoiler = !TextUtils.isEmpty(statusViewData.getSpoilerText());
            if (!shouldShowContentIfSpoiler && hasSpoiler) {
                statusContent.setVisibility(View.GONE);
            } else {
                statusContent.setVisibility(View.VISIBLE);
            }

            Spanned content = statusViewData.getContent();
            List<Emoji> emojis = statusViewData.getStatusEmojis();

            if (statusViewData.isCollapsible() && (statusViewData.isExpanded() || !hasSpoiler)) {
                contentCollapseButton.setOnClickListener(view -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && notificationActionListener != null) {
                        notificationActionListener.onNotificationContentCollapsedChange(!statusViewData.isCollapsed(), position);
                    }
                });

                contentCollapseButton.setVisibility(View.VISIBLE);
                if (statusViewData.isCollapsed()) {
                    contentCollapseButton.setText(R.string.status_content_warning_show_more);
                    statusContent.setFilters(COLLAPSE_INPUT_FILTER);
                } else {
                    contentCollapseButton.setText(R.string.status_content_warning_show_less);
                    statusContent.setFilters(NO_INPUT_FILTER);
                }
            } else {
                contentCollapseButton.setVisibility(View.GONE);
                statusContent.setFilters(NO_INPUT_FILTER);
            }

            CharSequence emojifiedText = CustomEmojiHelper.emojify(content, emojis, statusContent);
            LinkHelper.setClickableText(statusContent, emojifiedText, statusViewData.getMentions(), listener);

            CharSequence emojifiedContentWarning =
                    CustomEmojiHelper.emojify(statusViewData.getSpoilerText(), statusViewData.getStatusEmojis(), contentWarningDescriptionTextView);
            contentWarningDescriptionTextView.setText(emojifiedContentWarning);
        }

        private void setupReplyInfo() {
            if (statusViewData.getInReplyToId() != null) {
                Context context = replyInfo.getContext();
                String replyToAccount = statusViewData.getInReplyToAccountAcct();
                replyInfo.setText(context.getString(R.string.status_replied_to_format, replyToAccount));
                if (!statusViewData.getParentVisible()) {
                    replyInfo.setPaintFlags(replyInfo.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    replyInfo.setOnClickListener(null);
                } else {
                    replyInfo.setPaintFlags(replyInfo.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    replyInfo.setOnClickListener(v -> notificationActionListener.onViewReplyTo(getAdapterPosition()));
                }
                replyInfo.setVisibility(View.VISIBLE);
            } else {
                replyInfo.setVisibility(View.GONE);
            }
        }
    }
}
