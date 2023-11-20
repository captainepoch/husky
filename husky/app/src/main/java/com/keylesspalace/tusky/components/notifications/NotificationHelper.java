/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2022  Conny Duck
 * Copyright (C) 2018  Jeremiasz Nelz <remi6397(a)gmail.com>
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

package com.keylesspalace.tusky.components.notifications;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.FutureTarget;
import com.keylesspalace.tusky.BuildConfig;
import com.keylesspalace.tusky.MainActivity;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.components.unifiedpush.NotificationWorker;
import com.keylesspalace.tusky.db.AccountEntity;
import com.keylesspalace.tusky.db.AccountManager;
import com.keylesspalace.tusky.entity.Notification;
import com.keylesspalace.tusky.entity.Poll;
import com.keylesspalace.tusky.entity.PollOption;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.receiver.SendStatusBroadcastReceiver;
import com.keylesspalace.tusky.util.StringUtils;
import com.keylesspalace.tusky.viewdata.PollViewDataKt;
import static com.keylesspalace.tusky.viewdata.PollViewDataKt.buildDescription;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import timber.log.Timber;

public class NotificationHelper {

    private static int notificationId = 0;

    /**
     * constants used in Intents
     */
    public static final String ACCOUNT_ID = "account_id";

    private static final String TAG = "NotificationHelper";

    public static final String REPLY_ACTION = "REPLY_ACTION";

    public static final String COMPOSE_ACTION = "COMPOSE_ACTION";

    public static final String CHAT_REPLY_ACTION = "CHAT_REPLY_ACTION";

    public static final String KEY_REPLY = "KEY_REPLY";

    public static final String KEY_SENDER_ACCOUNT_ID = "KEY_SENDER_ACCOUNT_ID";

    public static final String KEY_SENDER_ACCOUNT_IDENTIFIER = "KEY_SENDER_ACCOUNT_IDENTIFIER";

    public static final String KEY_SENDER_ACCOUNT_FULL_NAME = "KEY_SENDER_ACCOUNT_FULL_NAME";

    public static final String KEY_NOTIFICATION_ID = "KEY_NOTIFICATION_ID";

    public static final String KEY_CITED_STATUS_ID = "KEY_CITED_STATUS_ID";

    public static final String KEY_VISIBILITY = "KEY_VISIBILITY";

    public static final String KEY_SPOILER = "KEY_SPOILER";

    public static final String KEY_MENTIONS = "KEY_MENTIONS";

    public static final String KEY_CITED_TEXT = "KEY_CITED_TEXT";

    public static final String KEY_CITED_AUTHOR_LOCAL = "KEY_CITED_AUTHOR_LOCAL";

    public static final String KEY_CHAT_ID = "KEY_CHAT_ID";

    /**
     * notification channels used on Android O+
     **/
    public static final String CHANNEL_MENTION = "CHANNEL_MENTION";
    public static final String CHANNEL_FOLLOW = "CHANNEL_FOLLOW";
    public static final String CHANNEL_FOLLOW_REQUEST = "CHANNEL_FOLLOW_REQUEST";
    public static final String CHANNEL_BOOST = "CHANNEL_BOOST";
    public static final String CHANNEL_FAVOURITE = "CHANNEL_FAVOURITE";
    public static final String CHANNEL_POLL = "CHANNEL_POLL";
    public static final String CHANNEL_EMOJI_REACTION = "CHANNEL_EMOJI_REACTION";
    public static final String CHANNEL_CHAT_MESSAGES = "CHANNEL_CHAT_MESSAGES";
    public static final String CHANNEL_SUBSCRIPTIONS = "CHANNEL_SUBSCRIPTIONS";
    public static final String CHANNEL_MOVE = "CHANNEL_MOVE";

    /**
     * WorkManager Tag
     */
    private static final String NOTIFICATION_PULL_TAG = "pullNotifications";

    /**
     * by setting this as false, it's possible to test legacy notification channels on newer devices
     */
    // public static final boolean NOTIFICATION_USE_CHANNELS = false;
    public static final boolean NOTIFICATION_USE_CHANNELS =
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);

    /**
     * Takes a given Mastodon notification and either creates a new Android notification or updates
     * the state of the existing notification to reflect the new interaction.
     *
     * @param context to access application preferences and services
     * @param body    a new Mastodon notification
     * @param account the account for which the notification should be shown
     * @return An Android notification, null otherwise
     */
    public static android.app.Notification make(
        final Context context,
        NotificationManagerCompat notificationManager,
        Notification body,
        AccountEntity account,
        boolean isFirstOfBatch)
    {
        body = Notification.rewriteToStatusTypeIfNeeded(body, account.getAccountId());

        if(!filterNotification(account, body, context)) {
            return null;
        }

        // Pleroma extension: don't notify about seen notifications
        if(body.getPleroma() != null && body.getPleroma().getSeen()) {
            return null;
        }

        if(body.getStatus() != null &&
           (body.getStatus().isUserMuted() || body.getStatus().isThreadMuted())) {
            return null;
        }

        String rawCurrentNotifications = account.getActiveNotifications();
        JSONArray currentNotifications;

        try {
            currentNotifications = new JSONArray(rawCurrentNotifications);
        } catch(JSONException e) {
            currentNotifications = new JSONArray();
        }

        for(int i = 0; i < currentNotifications.length(); i++) {
            try {
                if(currentNotifications.getString(i).equals(body.getAccount().getName())) {
                    currentNotifications.remove(i);
                    break;
                }
            } catch(JSONException e) {
                Timber.e(e);
            }
        }

        currentNotifications.put(body.getAccount().getName());

        account.setActiveNotifications(currentNotifications.toString());

        // Notification group member
        // =========================
        final NotificationCompat.Builder builder = newNotification(context, body, account, false);

        notificationId++;

        builder.setContentTitle(titleForType(context, body, account))
            .setContentText(bodyForType(body, context));

        if(body.getType() == Notification.Type.MENTION ||
           body.getType() == Notification.Type.POLL) {
            builder.setStyle(
                new NotificationCompat.BigTextStyle().bigText(bodyForType(body, context)));
        }

        //load the avatar synchronously
        Bitmap accountAvatar;
        try {
            FutureTarget<Bitmap> target =
                Glide.with(context).asBitmap().load(body.getAccount().getAvatar())
                    .transform(new RoundedCorners(20)).submit();

            accountAvatar = target.get();
        } catch(ExecutionException | InterruptedException e) {
            Timber.e("Error loading account avatar %s", e);
            accountAvatar =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.avatar_default);
        }

        builder.setLargeIcon(accountAvatar);

        // Reply to mention action; RemoteInput is available from KitKat Watch, but buttons are available from Nougat
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if(body.getType() == Notification.Type.MENTION) {
                RemoteInput replyRemoteInput = new RemoteInput.Builder(KEY_REPLY).setLabel(
                    context.getString(R.string.label_quick_reply)).build();

                PendingIntent quickReplyPendingIntent =
                    getStatusReplyIntent(REPLY_ACTION, context, body, account);

                NotificationCompat.Action quickReplyAction =
                    new NotificationCompat.Action.Builder(R.drawable.ic_reply_24dp,
                        context.getString(R.string.action_quick_reply),
                        quickReplyPendingIntent).addRemoteInput(replyRemoteInput).build();

                builder.addAction(quickReplyAction);

                PendingIntent composePendingIntent =
                    getStatusReplyIntent(COMPOSE_ACTION, context, body, account);

                NotificationCompat.Action composeAction =
                    new NotificationCompat.Action.Builder(R.drawable.ic_reply_24dp,
                        context.getString(R.string.action_compose_shortcut),
                        composePendingIntent).build();

                builder.addAction(composeAction);
            } else if(body.getType() == Notification.Type.CHAT_MESSAGE) {
                RemoteInput replyRemoteInput = new RemoteInput.Builder(KEY_REPLY).setLabel(
                    context.getString(R.string.label_quick_reply)).build();

                PendingIntent quickReplyPendingIntent =
                    getStatusReplyIntent(CHAT_REPLY_ACTION, context, body, account);

                NotificationCompat.Action quickReplyAction =
                    new NotificationCompat.Action.Builder(R.drawable.ic_reply_24dp,
                        context.getString(R.string.action_quick_reply),
                        quickReplyPendingIntent).addRemoteInput(replyRemoteInput).build();

                builder.addAction(quickReplyAction);
            }
        }

        builder.setSubText(account.getFullName());
        builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        builder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
        builder.setOnlyAlertOnce(true);

        // only alert for the first notification of a batch to avoid multiple alerts at once
        if(!isFirstOfBatch) {
            builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
        }

        // Summary
        // =======
        final NotificationCompat.Builder summaryBuilder =
            newNotification(context, body, account, true);

        if(currentNotifications.length() != 1) {
            try {
                String title = context.getString(R.string.notification_title_summary,
                    currentNotifications.length());
                String text = joinNames(context, currentNotifications);
                summaryBuilder.setContentTitle(title).setContentText(text);
            } catch(JSONException e) {
                Timber.e(e);
            }
        }

        summaryBuilder.setSubText(account.getFullName());
        summaryBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        summaryBuilder.setCategory(NotificationCompat.CATEGORY_SOCIAL);
        summaryBuilder.setOnlyAlertOnce(true);
        summaryBuilder.setGroupSummary(true);

        return builder.build();
    }

    private static NotificationCompat.Builder newNotification(Context context, Notification body, AccountEntity account, boolean summary) {
        Intent summaryResultIntent = new Intent(context, MainActivity.class);
        summaryResultIntent.putExtra(ACCOUNT_ID, account.getId());
        TaskStackBuilder summaryStackBuilder = TaskStackBuilder.create(context);
        summaryStackBuilder.addParentStack(MainActivity.class);
        summaryStackBuilder.addNextIntent(summaryResultIntent);

        PendingIntent summaryResultPendingIntent = summaryStackBuilder.getPendingIntent(
                (int) (notificationId + account.getId() * 10000),
                pendingIntentFlags(false)
        );

        // We have to switch account here
        Intent eventResultIntent = new Intent(context, MainActivity.class);
        eventResultIntent.putExtra(ACCOUNT_ID, account.getId());
        TaskStackBuilder eventStackBuilder = TaskStackBuilder.create(context);
        eventStackBuilder.addParentStack(MainActivity.class);
        eventStackBuilder.addNextIntent(eventResultIntent);

        PendingIntent eventResultPendingIntent = eventStackBuilder.getPendingIntent(
                (int) account.getId(),
                pendingIntentFlags(false));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, getChannelId(account, body))
                .setSmallIcon(R.drawable.ic_notify)
                .setContentIntent(summary ? summaryResultPendingIntent : eventResultPendingIntent)
                //.setColor(BuildConfig.FLAVOR == "green" ? Color.parseColor("#19A341") : ContextCompat.getColor(context, R.color.tusky_orange))
                .setGroup(account.getAccountId())
                .setAutoCancel(true)
                .setShortcutId(Long.toString(account.getId()))
                .setDefaults(0); // So it doesn't ring twice, notify only in Target callback

        setupPreferences(account, builder);

        return builder;
    }

    private static PendingIntent getStatusReplyIntent(String action, Context context, Notification body, AccountEntity account) {
        Intent replyIntent = new Intent(context, SendStatusBroadcastReceiver.class)
                .setAction(action)
                .putExtra(KEY_SENDER_ACCOUNT_ID, account.getId())
                .putExtra(KEY_SENDER_ACCOUNT_IDENTIFIER, account.getIdentifier())
                .putExtra(KEY_SENDER_ACCOUNT_FULL_NAME, account.getFullName())
                .putExtra(KEY_NOTIFICATION_ID, notificationId);

        if(action == CHAT_REPLY_ACTION) {
            replyIntent.putExtra(KEY_CHAT_ID, body.getChatMessage().getChatId());
        } else {
            Status status = body.getStatus();

            String citedLocalAuthor = status.getAccount().getLocalUsername();
            String citedText = status.getContent().toString();
            String inReplyToId = status.getId();
            Status actionableStatus = status.getActionableStatus();
            Status.Visibility replyVisibility = actionableStatus.getVisibility();
            String contentWarning = actionableStatus.getSpoilerText();
            Status.Mention[] mentions = actionableStatus.getMentions();
            List<String> mentionedUsernames = new ArrayList<>();
            mentionedUsernames.add(actionableStatus.getAccount().getUsername());
            for(Status.Mention mention : mentions) {
                mentionedUsernames.add(mention.getUsername());
            }
            mentionedUsernames.removeAll(Collections.singleton(account.getUsername()));
            mentionedUsernames = new ArrayList<>(new LinkedHashSet<>(mentionedUsernames));

            replyIntent.putExtra(KEY_CITED_AUTHOR_LOCAL, citedLocalAuthor)
                    .putExtra(KEY_CITED_TEXT, citedText)
                    .putExtra(KEY_CITED_STATUS_ID, inReplyToId)
                    .putExtra(KEY_VISIBILITY, replyVisibility)
                    .putExtra(KEY_SPOILER, contentWarning)
                    .putExtra(KEY_MENTIONS, mentionedUsernames.toArray(new String[0]));
        }

        return PendingIntent.getBroadcast(context.getApplicationContext(),
                notificationId,
                replyIntent,
                pendingIntentFlags(true));
    }

    public static void createNotificationChannelsForAccount(@NonNull AccountEntity account, @NonNull Context context) {
        if(NOTIFICATION_USE_CHANNELS) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            String[] channelIds = new String[]{
                    CHANNEL_MENTION + account.getIdentifier(),
                    CHANNEL_FOLLOW + account.getIdentifier(),
                    CHANNEL_FOLLOW_REQUEST + account.getIdentifier(),
                    CHANNEL_BOOST + account.getIdentifier(),
                    CHANNEL_FAVOURITE + account.getIdentifier(),
                    CHANNEL_POLL + account.getIdentifier(),
                    CHANNEL_EMOJI_REACTION + account.getIdentifier(),
                    CHANNEL_CHAT_MESSAGES + account.getIdentifier(),
                    CHANNEL_SUBSCRIPTIONS + account.getIdentifier(),
                    CHANNEL_MOVE + account.getIdentifier()
            };

            int[] channelNames = {
                    R.string.notification_mention_name,
                    R.string.notification_follow_name,
                    R.string.notification_follow_request_name,
                    R.string.notification_boost_name,
                    R.string.notification_favourite_name,
                    R.string.notification_poll_name,
                    R.string.notification_emoji_name,
                    R.string.notification_chat_message_name,
                    R.string.notification_subscription_name,
                    R.string.notification_move_name
            };

            int[] channelDescriptions = {
                    R.string.notification_mention_descriptions,
                    R.string.notification_follow_description,
                    R.string.notification_follow_request_description,
                    R.string.notification_boost_description,
                    R.string.notification_favourite_description,
                    R.string.notification_poll_description,
                    R.string.notification_emoji_description,
                    R.string.notification_chat_message_description,
                    R.string.notification_subscription_description,
                    R.string.notification_move_description
            };

            List<NotificationChannel> channels = new ArrayList<>(9);

            NotificationChannelGroup channelGroup = new NotificationChannelGroup(account.getIdentifier(), account.getFullName());

            //noinspection ConstantConditions
            notificationManager.createNotificationChannelGroup(channelGroup);

            for(int i = 0; i < channelIds.length; i++) {
                String id = channelIds[i];
                String name = context.getString(channelNames[i]);
                String description = context.getString(channelDescriptions[i]);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(id, name, importance);

                channel.setDescription(description);
                channel.enableLights(true);
                channel.setLightColor(0xFF2B90D9);
                channel.enableVibration(true);
                channel.setShowBadge(true);
                channel.setGroup(account.getIdentifier());
                channels.add(channel);
            }

            notificationManager.createNotificationChannels(channels);
        }
    }

    public static void deleteNotificationChannelsForAccount(@NonNull AccountEntity account, @NonNull Context context) {
        if(NOTIFICATION_USE_CHANNELS) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            //noinspection ConstantConditions
            notificationManager.deleteNotificationChannelGroup(account.getIdentifier());
        }
    }

    public static void deleteLegacyNotificationChannels(@NonNull Context context, @NonNull AccountManager accountManager) {
        if(NOTIFICATION_USE_CHANNELS) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // used until Tusky 1.4
            //noinspection ConstantConditions
            notificationManager.deleteNotificationChannel(CHANNEL_MENTION);
            notificationManager.deleteNotificationChannel(CHANNEL_FAVOURITE);
            notificationManager.deleteNotificationChannel(CHANNEL_BOOST);
            notificationManager.deleteNotificationChannel(CHANNEL_FOLLOW);

            // used until Tusky 1.7
            for(AccountEntity account : accountManager.getAllAccountsOrderedByActive()) {
                notificationManager.deleteNotificationChannel(CHANNEL_FAVOURITE + " " + account.getIdentifier());
            }
        }
    }

    public static boolean areNotificationsEnabled(@NonNull Context context, @NonNull AccountManager accountManager) {
        if(NOTIFICATION_USE_CHANNELS) {
            // on Android >= O, notifications are enabled, if at least one channel is enabled
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            //noinspection ConstantConditions
            if(notificationManager.areNotificationsEnabled()) {
                for(NotificationChannel channel : notificationManager.getNotificationChannels()) {
                    if(channel.getImportance() > NotificationManager.IMPORTANCE_NONE) {
                        Timber.d("NotificationsEnabled");
                        return true;
                    }
                }
            }
            Timber.d("NotificationsDisabled");

            return false;
        } else {
            // on Android < O, notifications are enabled, if at least one account has notification enabled
            return accountManager.areNotificationsEnabled();
        }
    }

    public static void enablePullNotifications(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelAllWorkByTag(NOTIFICATION_PULL_TAG);

        WorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS
        )
                .addTag(NOTIFICATION_PULL_TAG)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();

        workManager.enqueue(workRequest);

        Timber.d("enabled notification checks with ${PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS} ms interval");
    }

    public static void disablePullNotifications(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(NOTIFICATION_PULL_TAG);
        Timber.d("Disabled notification checks");
    }

    public static void clearNotificationsForActiveAccount(@NonNull Context context, @NonNull AccountManager accountManager) {
        AccountEntity account = accountManager.getActiveAccount();
        if(account != null && !account.getActiveNotifications().equals("[]")) {
            Single.fromCallable(() -> {
                        account.setActiveNotifications("[]");
                        accountManager.saveAccount(account);

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        //noinspection ConstantConditions
                        notificationManager.cancel((int) account.getId());
                        return true;
                    })
                    .subscribeOn(Schedulers.io())
                    .subscribe();
        }
    }

    private static boolean filterNotification(AccountEntity account, Notification notification,
                                              Context context) {
        if(NOTIFICATION_USE_CHANNELS) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            String channelId = getChannelId(account, notification);
            if(channelId == null) {
                // unknown notificationtype
                return false;
            }
            //noinspection ConstantConditions
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            return channel.getImportance() > NotificationManager.IMPORTANCE_NONE;
        }

        switch(notification.getType()) {
            case MENTION:
                return account.getNotificationsMentioned();
            case STATUS:
                return account.getNotificationsSubscriptions();
            case FOLLOW:
                return account.getNotificationsFollowed();
            case FOLLOW_REQUEST:
                return account.getNotificationsFollowRequested();
            case REBLOG:
                return account.getNotificationsReblogged();
            case FAVOURITE:
                return account.getNotificationsFavorited();
            case POLL:
                return account.getNotificationsPolls();
            case EMOJI_REACTION:
                return account.getNotificationsEmojiReactions();
            case CHAT_MESSAGE:
                return account.getNotificationsChatMessages();
            case MOVE:
                return account.getNotificationsMove();
            default:
                return false;
        }
    }

    @Nullable
    private static String getChannelId(AccountEntity account, Notification notification) {
        switch(notification.getType()) {
            case MENTION:
                return CHANNEL_MENTION + account.getIdentifier();
            case STATUS:
                return CHANNEL_SUBSCRIPTIONS + account.getIdentifier();
            case FOLLOW:
                return CHANNEL_FOLLOW + account.getIdentifier();
            case FOLLOW_REQUEST:
                return CHANNEL_FOLLOW_REQUEST + account.getIdentifier();
            case REBLOG:
                return CHANNEL_BOOST + account.getIdentifier();
            case FAVOURITE:
                return CHANNEL_FAVOURITE + account.getIdentifier();
            case POLL:
                return CHANNEL_POLL + account.getIdentifier();
            case EMOJI_REACTION:
                return CHANNEL_EMOJI_REACTION + account.getIdentifier();
            case CHAT_MESSAGE:
                return CHANNEL_CHAT_MESSAGES + account.getIdentifier();
            case MOVE:
                return CHANNEL_MOVE + account.getIdentifier();
            default:
                return null;
        }
    }

    private static void setupPreferences(
            AccountEntity account,
            NotificationCompat.Builder builder) {
        if(NOTIFICATION_USE_CHANNELS) {
            return;  //do nothing on Android O or newer, the system uses the channel settings anyway
        }

        if(account.getNotificationSound()) {
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        }

        if(account.getNotificationVibration()) {
            builder.setVibrate(new long[]{500, 500});
        }

        if(account.getNotificationLight()) {
            builder.setLights(0xFF2B90D9, 300, 1000);
        }
    }

    private static String wrapItemAt(JSONArray array, int index) throws JSONException {
        return StringUtils.unicodeWrap(array.get(index).toString());
    }

    @Nullable
    private static String joinNames(Context context, JSONArray array) throws JSONException {
        if(array.length() > 3) {
            int length = array.length();
            return String.format(context.getString(R.string.notification_summary_large),
                    wrapItemAt(array, length - 1),
                    wrapItemAt(array, length - 2),
                    wrapItemAt(array, length - 3),
                    length - 3);
        } else if(array.length() == 3) {
            return String.format(context.getString(R.string.notification_summary_medium),
                    wrapItemAt(array, 2),
                    wrapItemAt(array, 1),
                    wrapItemAt(array, 0));
        } else if(array.length() == 2) {
            return String.format(context.getString(R.string.notification_summary_small),
                    wrapItemAt(array, 1),
                    wrapItemAt(array, 0));
        }

        return null;
    }

    @Nullable
    private static String titleForType(Context context, Notification notification, AccountEntity account) {
        String accountName = StringUtils.unicodeWrap(notification.getAccount().getName());
        switch(notification.getType()) {
            case MENTION:
                return String.format(context.getString(R.string.notification_mention_format),
                        accountName);
            case STATUS:
                return String.format(context.getString(R.string.notification_subscription_format),
                        accountName);
            case FOLLOW:
                return String.format(context.getString(R.string.notification_follow_format),
                        accountName);
            case FOLLOW_REQUEST:
                return String.format(context.getString(R.string.notification_follow_request_format),
                        accountName);
            case FAVOURITE:
                return String.format(context.getString(R.string.notification_favourite_format),
                        accountName);
            case REBLOG:
                return String.format(context.getString(R.string.notification_reblog_format),
                        accountName);
            case EMOJI_REACTION:
                return String.format(context.getString(R.string.notification_emoji_format),
                        accountName, notification.getEmoji());
            case POLL:
                if(notification.getStatus().getAccount().getId().equals(account.getAccountId())) {
                    return context.getString(R.string.poll_ended_created);
                } else {
                    return context.getString(R.string.poll_ended_voted);
                }
            case CHAT_MESSAGE:
                return String.format(context.getString(R.string.notification_chat_message_format),
                        accountName);
            case MOVE: {
                return String.format(context.getString(R.string.notification_move_format), accountName);
            }
        }

        return null;
    }

    private static String bodyForType(Notification notification, Context context) {
        switch(notification.getType()) {
            case MOVE:
                return "@" + notification.getTarget().getUsername();
            case FOLLOW:
            case FOLLOW_REQUEST:
                return "@" + notification.getAccount().getUsername();
            case MENTION:
            case FAVOURITE:
            case REBLOG:
            case EMOJI_REACTION:
            case STATUS:
                if(!TextUtils.isEmpty(notification.getStatus().getSpoilerText())) {
                    return notification.getStatus().getSpoilerText();
                } else {
                    return notification.getStatus().getContent().toString();
                }
            case POLL:
                if(!TextUtils.isEmpty(notification.getStatus().getSpoilerText())) {
                    return notification.getStatus().getSpoilerText();
                } else {
                    StringBuilder builder = new StringBuilder(notification.getStatus().getContent());
                    builder.append('\n');
                    Poll poll = notification.getStatus().getPoll();
                    for(PollOption option : poll.getOptions()) {
                        builder.append(buildDescription(option.getTitle(),
                                PollViewDataKt.calculatePercent(option.getVotesCount(), poll.getVotesCount()),
                                context));
                        builder.append('\n');
                    }
                    return builder.toString();
                }
            case CHAT_MESSAGE:
                if(!TextUtils.isEmpty(notification.getChatMessage().getContent())) {
                    return notification.getChatMessage().getContent().toString();
                } else if(notification.getChatMessage().getAttachment() != null) {
                    return context.getString(notification.getChatMessage().getAttachment().describeAttachmentType());
                } else if(notification.getChatMessage().getCard() != null) {
                    return context.getString(R.string.link);
                } else {
                    return "";
                }
        }
        return null;
    }

    public static int pendingIntentFlags(boolean mutable) {
        return (PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                (mutable ? PendingIntent.FLAG_MUTABLE : PendingIntent.FLAG_IMMUTABLE) : 0)
        );
    }
}
