package com.keylesspalace.tusky.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.entity.Emoji;
import com.keylesspalace.tusky.interfaces.StatusActionListener;
import com.keylesspalace.tusky.util.CustomEmojiHelper;
import com.keylesspalace.tusky.util.StatusDisplayOptions;
import com.keylesspalace.tusky.util.TimestampUtils;
import com.keylesspalace.tusky.viewdata.StatusViewData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MutedStatusViewHolder extends RecyclerView.ViewHolder {
    public static class Key {
        public static final String KEY_CREATED = "created";
    }

    private TextView displayName;
    private TextView username;
    private ImageButton unmuteButton;
    public TextView timestampInfo;

    private SimpleDateFormat shortSdf;
    private SimpleDateFormat longSdf;

    protected MutedStatusViewHolder(View itemView) {
        super(itemView);
        displayName = itemView.findViewById(R.id.status_display_name);
        username = itemView.findViewById(R.id.status_username);
        timestampInfo = itemView.findViewById(R.id.status_timestamp_info);
        unmuteButton = itemView.findViewById(R.id.status_toggle_mute);
        
        this.shortSdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        this.longSdf = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());
    }

    protected void setDisplayName(String name, List<Emoji> customEmojis) {
        CharSequence emojifiedName = CustomEmojiHelper.emojify(name, customEmojis, displayName, true);
        displayName.setText(emojifiedName);
    }

    protected void setUsername(String name) {
        Context context = username.getContext();
        String usernameText = context.getString(R.string.status_username_format, name);
        username.setText(usernameText);
    }

    protected void setCreatedAt(Date createdAt, StatusDisplayOptions statusDisplayOptions) {
        if (statusDisplayOptions.useAbsoluteTime()) {
            timestampInfo.setText(getAbsoluteTime(createdAt));
        } else {
            if (createdAt == null) {
                timestampInfo.setText("?m");
            } else {
                long then = createdAt.getTime();
                long now = System.currentTimeMillis();
                String readout = TimestampUtils.getRelativeTimeSpanString(timestampInfo.getContext(), then, now);
                timestampInfo.setText(readout);
            }
        }
    }

    private String getAbsoluteTime(Date createdAt) {
        if (createdAt == null) {
            return "??:??:??";
        }
        if (DateUtils.isToday(createdAt.getTime())) {
            return shortSdf.format(createdAt);
        } else {
            return longSdf.format(createdAt);
        }
    }

    private CharSequence getCreatedAtDescription(Date createdAt,
                                                 StatusDisplayOptions statusDisplayOptions) {
        if (statusDisplayOptions.useAbsoluteTime()) {
            return getAbsoluteTime(createdAt);
        } else {
            /* This one is for screen-readers. Frequently, they would mispronounce timestamps like "17m"
             * as 17 meters instead of minutes. */

            if (createdAt == null) {
                return "? minutes";
            } else {
                long then = createdAt.getTime();
                long now = System.currentTimeMillis();
                return DateUtils.getRelativeTimeSpanString(then, now,
                        DateUtils.SECOND_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
            }
        }
    }

    private void setDescriptionForStatus(@NonNull StatusViewData.Concrete status,
                                         StatusDisplayOptions statusDisplayOptions) {
        Context context = itemView.getContext();

        String description = context.getString(R.string.description_muted_status,
                status.getUserFullName(),
                getCreatedAtDescription(status.getCreatedAt(), statusDisplayOptions),
                status.getNickname()
        );
        itemView.setContentDescription(description);
    }

    
    protected void setupButtons(final StatusActionListener listener, final String accountId) {
    
        unmuteButton.setOnClickListener(v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onMute(position, false);
            }
        });
    
        itemView.setOnClickListener( v -> {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onViewThread(position);
            }
        });
    }

    public void setupWithStatus(StatusViewData.Concrete status, final StatusActionListener listener,
                                StatusDisplayOptions statusDisplayOptions) {
        this.setupWithStatus(status, listener, statusDisplayOptions, null);
    }

    protected void setupWithStatus(StatusViewData.Concrete status,
                                   final StatusActionListener listener,
                                   StatusDisplayOptions statusDisplayOptions,
                                   @Nullable Object payloads) {
        if (payloads == null) {
            setDisplayName(status.getUserFullName(), status.getAccountEmojis());
            setUsername(status.getNickname());
            setCreatedAt(status.getCreatedAt(), statusDisplayOptions);
            
            setupButtons(listener, status.getSenderId());
            setDescriptionForStatus(status, statusDisplayOptions);

            // Workaround for RecyclerView 1.0.0 / androidx.core 1.0.0
            // RecyclerView tries to set AccessibilityDelegateCompat to null
            // but ViewCompat code replaces is with the default one. RecyclerView never
            // fetches another one from its delegate because it checks that it's set so we remove it
            // and let RecyclerView ask for a new delegate.
            itemView.setAccessibilityDelegate(null);
        } else {
            if (payloads instanceof List)
                for (Object item : (List) payloads) {
                    if (Key.KEY_CREATED.equals(item)) {
                        setCreatedAt(status.getCreatedAt(), statusDisplayOptions);
                    }
                }

        }
    }
}
