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

package com.keylesspalace.tusky.fragment;

import static com.keylesspalace.tusky.util.StringUtils.isLessThan;
import static com.uber.autodispose.AutoDispose.autoDisposable;
import static com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from;
import static org.koin.java.KoinJavaComponent.inject;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.arch.core.util.Function;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import at.connyduck.sparkbutton.helpers.Utils;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.adapter.NotificationsAdapter;
import com.keylesspalace.tusky.adapter.StatusBaseViewHolder;
import com.keylesspalace.tusky.adapter.StatusViewHolder;
import com.keylesspalace.tusky.appstore.BlockEvent;
import com.keylesspalace.tusky.appstore.BookmarkEvent;
import com.keylesspalace.tusky.appstore.EmojiReactEvent;
import com.keylesspalace.tusky.appstore.EventHub;
import com.keylesspalace.tusky.appstore.FavoriteEvent;
import com.keylesspalace.tusky.appstore.MuteConversationEvent;
import com.keylesspalace.tusky.appstore.MuteEvent;
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent;
import com.keylesspalace.tusky.appstore.ReblogEvent;
import com.keylesspalace.tusky.components.compose.ComposeActivity;
import com.keylesspalace.tusky.components.compose.ComposeActivity.ComposeOptions;
import com.keylesspalace.tusky.components.instance.domain.repository.InstanceRepository;
import com.keylesspalace.tusky.core.functional.Either;
import com.keylesspalace.tusky.db.AccountEntity;
import com.keylesspalace.tusky.entity.EmojiReaction;
import com.keylesspalace.tusky.entity.Notification;
import com.keylesspalace.tusky.entity.Poll;
import com.keylesspalace.tusky.entity.Relationship;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.interfaces.AccountActionListener;
import com.keylesspalace.tusky.interfaces.ActionButtonActivity;
import com.keylesspalace.tusky.interfaces.ReselectableFragment;
import com.keylesspalace.tusky.interfaces.StatusActionListener;
import com.keylesspalace.tusky.settings.PrefKeys;
import com.keylesspalace.tusky.util.CardViewMode;
import com.keylesspalace.tusky.util.HttpHeaderLink;
import com.keylesspalace.tusky.util.ListStatusAccessibilityDelegate;
import com.keylesspalace.tusky.util.ListUtils;
import com.keylesspalace.tusky.util.NotificationTypeConverterKt;
import com.keylesspalace.tusky.util.PairedList;
import com.keylesspalace.tusky.util.StatusDisplayOptions;
import com.keylesspalace.tusky.util.ViewDataUtils;
import com.keylesspalace.tusky.view.BackgroundMessageView;
import com.keylesspalace.tusky.view.EndlessOnScrollListener;
import com.keylesspalace.tusky.viewdata.NotificationViewData;
import com.keylesspalace.tusky.viewdata.StatusViewData;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class NotificationsFragment extends SFragment
    implements SwipeRefreshLayout.OnRefreshListener, StatusActionListener,
    NotificationsAdapter.NotificationActionListener, AccountActionListener, ReselectableFragment
{

    private static final String TAG = "NotificationF"; // logging tag

    private static final int LOAD_AT_ONCE = 30;
    private int maxPlaceholderId = 0;


    private final Set<Notification.Type> notificationFilter = new HashSet<>();

    private enum FetchEnd {
        TOP, BOTTOM, MIDDLE
    }

    /**
     * Placeholder for the notificationsEnabled. Consider moving to the separate class to hide constructor
     * and reuse in different places as needed.
     */
    private static final class Placeholder {
        final long id;

        public static Placeholder getInstance(long id) {
            return new Placeholder(id);
        }

        private Placeholder(long id) {
            this.id = id;
        }
    }

    private final SharedPreferences preferences = (SharedPreferences) inject(SharedPreferences.class).getValue();
    private final EventHub eventHub = (EventHub) inject(EventHub.class).getValue();
    private final InstanceRepository instanceRepo =
            (InstanceRepository) inject(InstanceRepository.class).getValue();
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private BackgroundMessageView statusView;
    private AppBarLayout appBarOptions;

    private LinearLayoutManager layoutManager;
    private EndlessOnScrollListener scrollListener;
    private NotificationsAdapter adapter;
    private Button buttonFilter;
    private boolean hideFab;
    private boolean topLoading;
    private boolean bottomLoading;
    private String bottomId;
    private boolean alwaysShowSensitiveMedia;
    private boolean alwaysOpenSpoiler;
    private boolean showNotificationsFilter;
    private boolean showingError;
    private boolean withMuted;

    // Each element is either a Notification for loading data or a Placeholder
    private final PairedList<Either<Placeholder, Notification>, NotificationViewData>
        notifications =
        new PairedList<>(new Function<Either<Placeholder, Notification>, NotificationViewData>() {
            @Override
            public NotificationViewData apply(Either<Placeholder, Notification> input) {
                if(input.isRight()) {
                    Notification notification =
                        Notification.rewriteToStatusTypeIfNeeded(input.asRight(),
                            accountManager.getValue().getActiveAccount().getAccountId());

                    return ViewDataUtils.notificationToViewData(notification,
                        alwaysShowSensitiveMedia, alwaysOpenSpoiler);
                } else {
                    return new NotificationViewData.Placeholder(input.asLeft().id, false);
                }
            }
        });

    public static NotificationsFragment newInstance() {
        NotificationsFragment fragment = new NotificationsFragment();
        Bundle arguments = new Bundle();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState)
    {
        View rootView =
            inflater.inflate(R.layout.fragment_timeline_notifications, container, false);

        @NonNull Context context = inflater.getContext(); // from inflater to silence warning
        layoutManager = new LinearLayoutManager(context);

        boolean showNotificationsFilterSetting =
            preferences.getBoolean("showNotificationsFilter", true);
        //Clear notifications on filter visibility change to force refresh
        if(showNotificationsFilterSetting != showNotificationsFilter) {
            notifications.clear();
        }
        showNotificationsFilter = showNotificationsFilterSetting;

        // Setup the SwipeRefreshLayout.
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        recyclerView = rootView.findViewById(R.id.recyclerView);
        progressBar = rootView.findViewById(R.id.progressBar);
        statusView = rootView.findViewById(R.id.statusView);
        appBarOptions = rootView.findViewById(R.id.appBarOptions);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.tusky_blue);

        loadNotificationsFilter();

        alwaysShowSensitiveMedia =
            accountManager.getValue().getActiveAccount().getAlwaysShowSensitiveMedia();
        alwaysOpenSpoiler = accountManager.getValue().getActiveAccount().getAlwaysOpenSpoiler();

        withMuted = !preferences.getBoolean(PrefKeys.HIDE_MUTED_USERS, false);

        topLoading = false;
        bottomLoading = false;
        bottomId = null;

        Button buttonClear = rootView.findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(v -> confirmClearNotifications());
        buttonFilter = rootView.findViewById(R.id.buttonFilter);
        buttonFilter.setOnClickListener(v -> showFilterMenu());

        if(notifications.isEmpty()) {
            swipeRefreshLayout.setEnabled(false);
            sendInitialRequest();
        } else {
            progressBar.setVisibility(View.GONE);
        }

        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        updateFilterVisibility();

        return rootView;
    }

    private void sendInitialRequest() {
        instanceRepo.getInstanceInfoRx()
                .observeOn(AndroidSchedulers.mainThread())
                .as(autoDisposable(from(this, Event.ON_DESTROY)))
                .subscribe(instance -> {
                    Timber.d("Has quoting posts [%s]", instance.getQuotePosting());

                    createNotificationsAdapter(instance.getQuotePosting());
                    setupRecyclerView();
                    updateAdapter();

                    sendFetchNotificationsRequest(null, null, FetchEnd.BOTTOM, -1);
                });
    }

    private void createNotificationsAdapter(boolean canQuotePosts) {
        StatusDisplayOptions statusDisplayOptions =
                new StatusDisplayOptions(
                        preferences.getBoolean("animateGifAvatars", false),
                        accountManager.getValue().getActiveAccount().getMediaPreviewEnabled(),
                        preferences.getBoolean("absoluteTimeView", false),
                        preferences.getBoolean("showBotOverlay", true),
                        preferences.getBoolean("useBlurhash", true), CardViewMode.NONE,
                        preferences.getBoolean("confirmReblogs", true),
                        preferences.getBoolean(PrefKeys.RENDER_STATUS_AS_MENTION, true),
                        preferences.getBoolean(PrefKeys.WELLBEING_HIDE_STATS_POSTS, false),
                        canQuotePosts
                );

        adapter = new NotificationsAdapter(accountManager.getValue().getActiveAccount().getAccountId(),
                        dataSource, statusDisplayOptions, this, this, this);
    }

    private void setupRecyclerView() {
        // Setup the RecyclerView.
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAccessibilityDelegateCompat(
                new ListStatusAccessibilityDelegate(recyclerView, this, (pos) -> {
                    NotificationViewData notification = notifications.getPairedItemOrNull(pos);
                    // We support replies only for now
                    if(notification instanceof NotificationViewData.Concrete) {
                        return ((NotificationViewData.Concrete) notification).getStatusViewData();
                    } else {
                        return null;
                    }
                }));

        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(scrollListener);
    }

    private void updateFilterVisibility() {
        CoordinatorLayout.LayoutParams params =
            (CoordinatorLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
        if(showNotificationsFilter && !showingError) {
            appBarOptions.setExpanded(true, false);
            appBarOptions.setVisibility(View.VISIBLE);
            //Set content behaviour to hide filter on scroll
            params.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        } else {
            appBarOptions.setExpanded(false, false);
            appBarOptions.setVisibility(View.GONE);
            //Clear behaviour to hide app bar
            params.setBehavior(null);
        }
    }

    private void confirmClearNotifications() {
        new AlertDialog.Builder(getContext()).setMessage(R.string.notification_clear_text)
            .setPositiveButton(android.R.string.yes,
                (DialogInterface dia, int which) -> clearNotifications())
            .setNegativeButton(android.R.string.no, null).show();
    }

    private void handleFavEvent(FavoriteEvent event) {
        Pair<Integer, Notification> posAndNotification = findReplyPosition(event.getStatusId());
        if(posAndNotification == null) {
            return;
        }
        //noinspection ConstantConditions
        setFavouriteForStatus(posAndNotification.first, posAndNotification.second.getStatus(),
            event.getFavourite());
    }

    private void handleBookmarkEvent(BookmarkEvent event) {
        Pair<Integer, Notification> posAndNotification = findReplyPosition(event.getStatusId());
        if(posAndNotification == null) {
            return;
        }
        //noinspection ConstantConditions
        setBookmarkForStatus(posAndNotification.first, posAndNotification.second.getStatus(),
            event.getBookmark());
    }

    private void handleReblogEvent(ReblogEvent event) {
        Pair<Integer, Notification> posAndNotification = findReplyPosition(event.getStatusId());
        if(posAndNotification == null) {
            return;
        }
        //noinspection ConstantConditions
        setReblogForStatus(posAndNotification.first, posAndNotification.second.getStatus(),
            event.getReblog());
    }

    private void handleMuteStatusEvent(MuteConversationEvent event) {
        Pair<Integer, Notification> posAndNotification = findReplyPosition(event.getStatusId());
        if(posAndNotification == null) {
            return;
        }

        String conversationId = posAndNotification.second.getStatus().getConversationId();

        if(conversationId.isEmpty()) { // invalid conversation ID
            if(withMuted) {
                setMutedStatusForStatus(posAndNotification.first,
                    posAndNotification.second.getStatus(), event.getMute(), event.getMute());
            } else {
                notifications.remove(posAndNotification.first);
            }
        } else {
            //noinspection ConstantConditions
            if(withMuted) {
                for(int i = 0; i < notifications.size(); i++) {
                    Notification notification = notifications.get(i).asRightOrNull();
                    if(notification != null && notification.getStatus() != null &&
                       notification.getType() == Notification.Type.MENTION &&
                       notification.getStatus().getConversationId() == conversationId) {
                        setMutedStatusForStatus(i, notification.getStatus(), event.getMute(),
                            event.getMute());
                    }
                }
            } else {
                removeAllByConversationId(conversationId);
            }
        }
        updateAdapter();
    }

    private void handleMuteEvent(MuteEvent event) {
        String id = event.getAccountId();
        boolean mute = event.getMute();

        if(withMuted) {
            for(int i = 0; i < notifications.size(); i++) {
                Notification notification = notifications.get(i).asRightOrNull();
                if(notification != null && notification.getStatus() != null &&
                   notification.getType() == Notification.Type.MENTION &&
                   notification.getAccount().getId().equals(id) &&
                   !notification.getStatus().isThreadMuted()) {
                    setMutedStatusForStatus(i, notification.getStatus(), mute, false);
                }
            }
            updateAdapter();
        } else {
            removeAllByAccountId(id);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        if(activity == null) {
            throw new AssertionError("Activity is null");
        }

        /* This is delayed until onActivityCreated solely because MainActivity.composeButton isn't
         * guaranteed to be set until then.
         * Use a modified scroll listener that both loads more notificationsEnabled as it goes, and hides
         * the compose button on down-scroll. */
        hideFab = preferences.getBoolean("fabHide", false);
        scrollListener = new EndlessOnScrollListener(layoutManager) {
            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                super.onScrolled(view, dx, dy);

                ActionButtonActivity activity = (ActionButtonActivity) getActivity();
                FloatingActionButton composeButton = activity.getActionButton();

                if(composeButton != null) {
                    if(hideFab) {
                        if(dy > 0 && composeButton.isShown()) {
                            composeButton.hide(); // hides the button if we're scrolling down
                        } else if(dy < 0 && !composeButton.isShown()) {
                            composeButton.show(); // shows it if we are scrolling up
                        }
                    } else if(!composeButton.isShown()) {
                        composeButton.show();
                    }
                }
            }

            @Override
            public void onLoadMore(int totalItemsCount, RecyclerView view) {
                NotificationsFragment.this.onLoadMore();
            }
        };

        eventHub.getEvents().observeOn(AndroidSchedulers.mainThread())
            .as(autoDisposable(from(this, Lifecycle.Event.ON_DESTROY))).subscribe(event -> {
                if(event instanceof FavoriteEvent) {
                    handleFavEvent((FavoriteEvent) event);
                } else if(event instanceof BookmarkEvent) {
                    handleBookmarkEvent((BookmarkEvent) event);
                } else if(event instanceof ReblogEvent) {
                    handleReblogEvent((ReblogEvent) event);
                } else if(event instanceof MuteConversationEvent) {
                    handleMuteStatusEvent((MuteConversationEvent) event);
                } else if(event instanceof BlockEvent) {
                    removeAllByAccountId(((BlockEvent) event).getAccountId());
                } else if(event instanceof MuteEvent) {
                    handleMuteEvent((MuteEvent) event);
                } else if(event instanceof PreferenceChangedEvent) {
                    onPreferenceChanged(((PreferenceChangedEvent) event).getPreferenceKey());
                } else if(event instanceof EmojiReactEvent) {
                    handleEmojiReactEvent((EmojiReactEvent) event);
                }
            });
    }

    @Override
    public void onRefresh() {
        this.statusView.setVisibility(View.GONE);
        this.showingError = false;
        Either<Placeholder, Notification> first = CollectionsKt.firstOrNull(this.notifications);
        String topId;
        if(first != null && first.isRight()) {
            topId = first.asRight().getId();
        } else {
            topId = null;
        }
        sendFetchNotificationsRequest(null, topId, FetchEnd.TOP, -1);
    }

    @Override
    public void onReply(int position) {
        super.reply(notifications.get(position).asRight().getStatus());
    }

    @Override
    public void onMenuReblog(final boolean reblog, final int position) {
        onReblog(reblog, position, false);
    }

    @Override
    public void onReblog(final boolean reblog, final int position, final boolean canQuote) {
        final Notification notification = notifications.get(position).asRight();
        final Status status = notification.getStatus();

        if(status == null) {
            return;
        }

        if(!reblog) {
            callReblogService(status, reblog, position);
            return;
        }

        if(!canQuote) {
            Timber.d("Reblog, user cannot quote");
            callReblogService(status, reblog, position);
        } else {
            Timber.d("Quotes are enabled");

            FragmentActivity activity = getActivity();
            if(activity != null) {
                final StatusReblogQuoteDialog dialog = new StatusReblogQuoteDialog(activity);
                dialog.setOnStatusActionListener(type -> {
                    if(type == StatusReblogQuoteType.QUOTE) {
                        ComposeOptions options = new ComposeOptions();
                        options.setQuotePostId(status.getId());
                        startActivity(ComposeActivity.startIntent(activity, options));
                    } else {
                        callReblogService(status, reblog, position);
                    }

                    return null;
                });
                dialog.show();
            }
        }
    }

    private void callReblogService(final Status status, final boolean reblog, final int position) {
        timelineCases.getValue().reblog(status, reblog)
                     .observeOn(AndroidSchedulers.mainThread())
                     .as(autoDisposable(from(this, Lifecycle.Event.ON_DESTROY)))
                     .subscribe(
                             (newStatus) -> setReblogForStatus(position, status, reblog),
                             (err) -> Timber.e(
                                     err,
                                     "Failed to reblog status %s, Error[%s]",
                                     status.getId(),
                                     err.getMessage()
                             )
                     );
    }

    private void setReblogForStatus(int position, Status status, boolean reblog) {
        if (reblog && recyclerView != null) {
            ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
            if (holder instanceof StatusViewHolder) {
                ((StatusViewHolder) holder).reblogButtonAnimate();
            }
        }

        status.setReblogged(reblog);

        if(status.getReblog() != null) {
            status.getReblog().setReblogged(reblog);
        }

        NotificationViewData.Concrete viewdata =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);

        StatusViewData.Builder viewDataBuilder =
            new StatusViewData.Builder(viewdata.getStatusViewData());
        viewDataBuilder.setReblogged(reblog);

        NotificationViewData.Concrete newViewData =
            new NotificationViewData.Concrete(viewdata.getType(), viewdata.getId(),
                viewdata.getAccount(), viewDataBuilder.createStatusViewData(), viewdata.getEmoji(),
                viewdata.getEmojiUrl(), viewdata.getTarget());
        notifications.setPairedItem(position, newViewData);
        updateAdapter();
    }

    @Override
    public void onFavourite(final boolean favourite, final int position) {
        final Notification notification = notifications.get(position).asRight();
        final Status status = notification.getStatus();

        timelineCases.getValue().favourite(status, favourite)
            .observeOn(AndroidSchedulers.mainThread()).as(autoDisposable(from(this)))
            .subscribe((newStatus) -> setFavouriteForStatus(position, status, favourite),
                (t) -> Log.d(getClass().getSimpleName(),
                    "Failed to favourite status: " + status.getId(), t));
    }

    private void setFavouriteForStatus(int position, Status status, boolean favourite) {
        status.setFavourited(favourite);

        if(status.getReblog() != null) {
            status.getReblog().setFavourited(favourite);
        }

        NotificationViewData.Concrete viewdata =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);

        StatusViewData.Builder viewDataBuilder =
            new StatusViewData.Builder(viewdata.getStatusViewData());
        viewDataBuilder.setFavourited(favourite);

        NotificationViewData.Concrete newViewData =
            new NotificationViewData.Concrete(viewdata.getType(), viewdata.getId(),
                viewdata.getAccount(), viewDataBuilder.createStatusViewData(), viewdata.getEmoji(),
                viewdata.getEmojiUrl(), viewdata.getTarget());

        notifications.setPairedItem(position, newViewData);
        updateAdapter();
    }

    @Override
    public void onBookmark(final boolean bookmark, final int position) {
        final Notification notification = notifications.get(position).asRight();
        final Status status = notification.getStatus();

        timelineCases.getValue().bookmark(status, bookmark)
            .observeOn(AndroidSchedulers.mainThread()).as(autoDisposable(from(this)))
            .subscribe((newStatus) -> setBookmarkForStatus(position, status, bookmark),
                (t) -> Log.d(getClass().getSimpleName(),
                    "Failed to bookmark status: " + status.getId(), t));
    }

    private void setBookmarkForStatus(int position, Status status, boolean bookmark) {
        status.setBookmarked(bookmark);

        if(status.getReblog() != null) {
            status.getReblog().setBookmarked(bookmark);
        }

        NotificationViewData.Concrete viewdata =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);

        StatusViewData.Builder viewDataBuilder =
            new StatusViewData.Builder(viewdata.getStatusViewData());
        viewDataBuilder.setBookmarked(bookmark);

        NotificationViewData.Concrete newViewData =
            new NotificationViewData.Concrete(viewdata.getType(), viewdata.getId(),
                viewdata.getAccount(), viewDataBuilder.createStatusViewData(), viewdata.getEmoji(),
                viewdata.getEmojiUrl(), viewdata.getTarget());

        notifications.setPairedItem(position, newViewData);
        updateAdapter();
    }

    public void onVoteInPoll(int position, @NonNull List<Integer> choices) {
        final Notification notification = notifications.get(position).asRight();
        final Status status = notification.getStatus();

        timelineCases.getValue().voteInPoll(status, choices)
            .observeOn(AndroidSchedulers.mainThread()).as(autoDisposable(from(this)))
            .subscribe((newPoll) -> setVoteForPoll(position, newPoll),
                (t) -> Log.d(TAG, "Failed to vote in poll: " + status.getId(), t));
    }

    private void setVoteForPoll(int position, Poll poll) {

        NotificationViewData.Concrete viewdata =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);

        StatusViewData.Builder viewDataBuilder =
            new StatusViewData.Builder(viewdata.getStatusViewData());
        viewDataBuilder.setPoll(poll);

        NotificationViewData.Concrete newViewData =
            new NotificationViewData.Concrete(viewdata.getType(), viewdata.getId(),
                viewdata.getAccount(), viewDataBuilder.createStatusViewData(), viewdata.getEmoji(),
                viewdata.getEmojiUrl(), viewdata.getTarget());

        notifications.setPairedItem(position, newViewData);
        updateAdapter();
    }

    @Override
    public void onMore(@NonNull View view, int position) {
        Notification notification = notifications.get(position).asRight();
        super.more(notification.getStatus(), view, position);
    }

    @Override
    public void onViewMedia(int position, int attachmentIndex, @Nullable View view) {
        Notification notification = notifications.get(position).asRightOrNull();
        if(notification == null || notification.getStatus() == null) {
            return;
        }
        super.viewMedia(attachmentIndex, notification.getStatus(), view);
    }

    @Override
    public void onViewThread(int position) {
        Notification notification = notifications.get(position).asRight();
        super.viewThread(notification.getStatus());
    }

    @Override
    public void onViewReplyTo(int position) {
        Notification notification = notifications.get(position).asRightOrNull();
        if(notification == null) {
            return;
        }
        super.onShowReplyTo(notification.getStatus().getInReplyToId());
    }

    @Override
    public void onViewQuote(
        final String quotedStatusId,
        final String quotedStatusUrl
    ) {
        if (quotedStatusId != null && quotedStatusUrl != null) {
            super.viewQuote(quotedStatusId, quotedStatusUrl);
        }
    }

    @Override
    public void onOpenReblog(int position) {
        Notification notification = notifications.get(position).asRight();
        onViewAccount(notification.getAccount().getId());
    }

    @Override
    public void onExpandedChange(boolean expanded, int position) {
        NotificationViewData.Concrete old =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);
        StatusViewData.Concrete statusViewData =
            new StatusViewData.Builder(old.getStatusViewData()).setIsExpanded(expanded)
                .createStatusViewData();
        NotificationViewData notificationViewData =
            new NotificationViewData.Concrete(old.getType(), old.getId(), old.getAccount(),
                statusViewData, old.getEmoji(), old.getEmojiUrl(), old.getTarget());
        notifications.setPairedItem(position, notificationViewData);
        updateAdapter();
    }

    @Override
    public void onContentHiddenChange(boolean isShowing, int position) {
        NotificationViewData.Concrete old =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);
        StatusViewData.Concrete statusViewData =
            new StatusViewData.Builder(old.getStatusViewData()).setIsShowingSensitiveContent(
                isShowing).createStatusViewData();
        NotificationViewData notificationViewData =
            new NotificationViewData.Concrete(old.getType(), old.getId(), old.getAccount(),
                statusViewData, old.getEmoji(), old.getEmojiUrl(), old.getTarget());
        notifications.setPairedItem(position, notificationViewData);
        updateAdapter();
    }

    @Override
    public void onMute(int position, boolean isMuted) {
        NotificationViewData.Concrete old =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);
        StatusViewData.Concrete statusViewData =
            new StatusViewData.Builder(old.getStatusViewData()).setMuted(isMuted)
                .createStatusViewData();
        NotificationViewData notificationViewData =
            new NotificationViewData.Concrete(old.getType(), old.getId(), old.getAccount(),
                statusViewData, old.getEmoji(), old.getEmojiUrl(), old.getTarget());
        notifications.setPairedItem(position, notificationViewData);
        updateAdapter();
    }

    private void setMutedStatusForStatus(int position, Status status, boolean muted,
        boolean threadMuted)
    {
        status.setThreadMuted(threadMuted);

        NotificationViewData.Concrete viewdata =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);

        StatusViewData.Builder viewDataBuilder =
            new StatusViewData.Builder(viewdata.getStatusViewData());
        viewDataBuilder.setThreadMuted(threadMuted);
        viewDataBuilder.setMuted(muted);

        NotificationViewData.Concrete newViewData =
            new NotificationViewData.Concrete(viewdata.getType(), viewdata.getId(),
                viewdata.getAccount(), viewDataBuilder.createStatusViewData(), viewdata.getEmoji(),
                viewdata.getEmojiUrl(), viewdata.getTarget());

        notifications.setPairedItem(position, newViewData);
    }


    @Override
    public void onLoadMore(int position) {
        //check bounds before accessing list,
        if(notifications.size() >= position && position > 0) {
            Notification previous = notifications.get(position - 1).asRightOrNull();
            Notification next = notifications.get(position + 1).asRightOrNull();
            if(previous == null || next == null) {
                Log.e(TAG, "Failed to load more, invalid placeholder position: " + position);
                return;
            }
            sendFetchNotificationsRequest(previous.getId(), next.getId(), FetchEnd.MIDDLE,
                position);
            Placeholder placeholder = notifications.get(position).asLeft();
            NotificationViewData notificationViewData =
                new NotificationViewData.Placeholder(placeholder.id, true);
            notifications.setPairedItem(position, notificationViewData);
            updateAdapter();
        } else {
            Log.d(TAG, "error loading more");
        }
    }

    @Override
    public void onContentCollapsedChange(boolean isCollapsed, int position) {
        if(position < 0 || position >= notifications.size()) {
            Log.e(TAG,
                String.format("Tried to access out of bounds status position: %d of %d", position,
                    notifications.size() - 1));
            return;
        }

        NotificationViewData notification = notifications.getPairedItem(position);
        if(!(notification instanceof NotificationViewData.Concrete)) {
            Log.e(TAG, String.format(
                "Expected NotificationViewData.Concrete, got %s instead at position: %d of %d",
                notification == null ? "null" : notification.getClass().getSimpleName(), position,
                notifications.size() - 1));
            return;
        }

        StatusViewData.Concrete status =
            ((NotificationViewData.Concrete) notification).getStatusViewData();
        StatusViewData.Concrete updatedStatus =
            new StatusViewData.Builder(status).setCollapsed(isCollapsed).createStatusViewData();

        NotificationViewData.Concrete concreteNotification =
            (NotificationViewData.Concrete) notification;
        NotificationViewData updatedNotification =
            new NotificationViewData.Concrete(concreteNotification.getType(),
                concreteNotification.getId(), concreteNotification.getAccount(), updatedStatus,
                concreteNotification.getEmoji(), concreteNotification.getEmojiUrl(),
                concreteNotification.getTarget());
        notifications.setPairedItem(position, updatedNotification);
        updateAdapter();

        // Since we cannot notify to the RecyclerView right away because it may be scrolling
        // we run this when the RecyclerView is done doing measurements and other calculations.
        // To test this is not bs: try getting a notification while scrolling, without wrapping
        // notifyItemChanged in a .post() call. App will crash.
        recyclerView.post(() -> adapter.notifyItemChanged(position, notification));
    }

    @Override
    public void onNotificationContentCollapsedChange(boolean isCollapsed, int position) {
        onContentCollapsedChange(isCollapsed, position);
    }

    private void clearNotifications() {
        //Cancel all ongoing requests
        swipeRefreshLayout.setRefreshing(false);
        resetNotificationsLoad();

        //Show friend elephant
        this.statusView.setVisibility(View.VISIBLE);
        this.statusView.setup(R.drawable.elephant_friend_empty, R.string.message_empty, null);
        updateFilterVisibility();

        //Update adapter
        updateAdapter();

        //Execute clear notifications request
        Call<ResponseBody> call = mastodonApi.getValue().clearNotifications();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                @NonNull Response<ResponseBody> response)
            {
                if(isAdded()) {
                    if(!response.isSuccessful()) {
                        //Reload notifications on failure
                        fullyRefreshWithProgressBar(true);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                //Reload notifications on failure
                fullyRefreshWithProgressBar(true);
            }
        });
        callList.add(call);
    }

    private void resetNotificationsLoad() {
        for(Call callItem : callList) {
            callItem.cancel();
        }
        callList.clear();
        bottomLoading = false;
        topLoading = false;

        //Disable load more
        bottomId = null;

        //Clear exists notifications
        notifications.clear();
    }


    private void showFilterMenu() {
        List<Notification.Type> notificationsList = Notification.Type.Companion.getAsList();
        List<String> list = new ArrayList<>();
        for(Notification.Type type : notificationsList) {
            // ignore chat messages, as we don't work with them in main notification fragment
            if(type == Notification.Type.CHAT_MESSAGE) {
                continue;
            }

            list.add(getNotificationText(type));
        }

        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice,
                list);
        PopupWindow window = new PopupWindow(getContext());
        View view = LayoutInflater.from(getContext())
            .inflate(R.layout.notifications_filter, (ViewGroup) getView(), false);
        final ListView listView = view.findViewById(R.id.listView);
        view.findViewById(R.id.buttonApply).setOnClickListener(v -> {
            SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
            Set<Notification.Type> excludes = new HashSet<>();
            for(int i = 0; i < notificationsList.size(); i++) {
                if(!checkedItems.get(i, false)) {
                    excludes.add(notificationsList.get(i));
                }
            }
            window.dismiss();
            applyFilterChanges(excludes);

        });

        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for(int i = 0; i < notificationsList.size(); i++) {
            if(!notificationFilter.contains(notificationsList.get(i))) {
                listView.setItemChecked(i, true);
            }
        }
        window.setContentView(view);
        window.setFocusable(true);
        window.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        window.showAsDropDown(buttonFilter);

    }

    private String getNotificationText(Notification.Type type) {
        switch(type) {
            case MENTION:
                return getString(R.string.notification_mention_name);
            case FAVOURITE:
                return getString(R.string.notification_favourite_name);
            case REBLOG:
                return getString(R.string.notification_boost_name);
            case FOLLOW:
                return getString(R.string.notification_follow_name);
            case FOLLOW_REQUEST:
                return getString(R.string.notification_follow_request_name);
            case POLL:
                return getString(R.string.notification_poll_name);
            case EMOJI_REACTION:
                return getString(R.string.notification_emoji_name);
            case MOVE:
                return getString(R.string.notification_move_name);
            case STATUS:
                return getString(R.string.notification_subscription_name);
            default:
                return "Unknown";
        }
    }

    private void applyFilterChanges(Set<Notification.Type> newSet) {
        List<Notification.Type> notifications = Notification.Type.Companion.getAsList();
        boolean isChanged = false;
        for(Notification.Type type : notifications) {
            if(notificationFilter.contains(type) && !newSet.contains(type)) {
                notificationFilter.remove(type);
                isChanged = true;
            } else if(!notificationFilter.contains(type) && newSet.contains(type)) {
                notificationFilter.add(type);
                isChanged = true;
            }
        }
        if(isChanged) {
            saveNotificationsFilter();
            fullyRefreshWithProgressBar(true);
        }

    }

    private void loadNotificationsFilter() {
        AccountEntity account = accountManager.getValue().getActiveAccount();
        if(account != null) {
            notificationFilter.clear();
            notificationFilter.addAll(
                NotificationTypeConverterKt.deserialize(account.getNotificationsFilter()));
        }
    }

    private void saveNotificationsFilter() {
        AccountEntity account = accountManager.getValue().getActiveAccount();
        if(account != null) {
            account.setNotificationsFilter(
                NotificationTypeConverterKt.serialize(notificationFilter));
            accountManager.getValue().saveAccount(account);
        }
    }

    @Override
    public void onViewTag(String tag) {
        super.viewTag(tag);
    }

    @Override
    public void onViewAccount(String id) {
        super.viewAccount(id);
    }

    @Override
    public void onMute(boolean mute, String id, int position, boolean notifications) {
        // No muting from notifications yet
    }

    @Override
    public void onBlock(boolean block, String id, int position) {
        // No blocking from notifications yet
    }

    @Override
    public void onRespondToFollowRequest(boolean accept, String id, int position) {
        Single<Relationship> request =
            accept ? mastodonApi.getValue().authorizeFollowRequestObservable(id) :
                mastodonApi.getValue().rejectFollowRequestObservable(id);
        request.observeOn(AndroidSchedulers.mainThread())
            .as(autoDisposable(from(this, Lifecycle.Event.ON_DESTROY)))
            .subscribe((relationship) -> fullyRefreshWithProgressBar(true), (error) -> Log.e(TAG,
                String.format("Failed to %s account id %s", accept ? "accept" : "reject", id)));
    }

    @Override
    public void onViewStatusForNotificationId(String notificationId) {
        for(Either<Placeholder, Notification> either : notifications) {
            Notification notification = either.asRightOrNull();
            if(notification != null && notification.getId().equals(notificationId)) {
                super.viewThread(notification.getStatus());
                return;
            }
        }
        Log.w(TAG, "Didn't find a notification for ID: " + notificationId);
    }

    private void onPreferenceChanged(String key) {
        switch(key) {
            case "fabHide": {
                hideFab = preferences.getBoolean("fabHide", false);
                break;
            }
            case "mediaPreviewEnabled": {
                boolean enabled =
                    accountManager.getValue().getActiveAccount().getMediaPreviewEnabled();
                if(enabled != adapter.isMediaPreviewEnabled()) {
                    adapter.setMediaPreviewEnabled(enabled);
                    fullyRefresh();
                }
            }
            case "showNotificationsFilter": {
                if(isAdded()) {
                    showNotificationsFilter =
                            preferences.getBoolean("showNotificationsFilter", true);
                    updateFilterVisibility();
                    fullyRefreshWithProgressBar(true);
                }
            }
            case PrefKeys.HIDE_MUTED_USERS: {
                withMuted = !preferences.getBoolean(PrefKeys.HIDE_MUTED_USERS, false);
                fullyRefresh();
            }
        }
    }

    @Override
    public void removeItem(int position) {
        notifications.remove(position);
        updateAdapter();
    }

    private void removeAllByConversationId(String conversationId) {
        // using iterator to safely remove items while iterating
        Iterator<Either<Placeholder, Notification>> iterator = notifications.iterator();
        while(iterator.hasNext()) {
            Either<Placeholder, Notification> placeholderOrNotification = iterator.next();
            Notification notification = placeholderOrNotification.asRightOrNull();
            if(notification != null && notification.getStatus() != null &&
               notification.getType() == Notification.Type.MENTION &&
               notification.getStatus().getConversationId().equalsIgnoreCase(conversationId)) {
                iterator.remove();
            }
        }
        updateAdapter();
    }

    private void removeAllByAccountId(String accountId) {
        // using iterator to safely remove items while iterating
        Iterator<Either<Placeholder, Notification>> iterator = notifications.iterator();
        while(iterator.hasNext()) {
            Either<Placeholder, Notification> notification = iterator.next();
            Notification maybeNotification = notification.asRightOrNull();
            if(maybeNotification != null &&
               maybeNotification.getAccount().getId().equals(accountId)) {
                iterator.remove();
            }
        }
        updateAdapter();
    }

    private void onLoadMore() {
        if(bottomId == null) {
            // already loaded everything
            return;
        }

        // Check for out-of-bounds when loading
        // This is required to allow full-timeline reloads of collapsible statuses when the settings
        // change.
        if(notifications.size() > 0) {
            Either<Placeholder, Notification> last = notifications.get(notifications.size() - 1);
            if(last.isRight()) {
                final Placeholder placeholder = newPlaceholder();
                notifications.add(new Either.Left<>(placeholder));
                NotificationViewData viewData =
                    new NotificationViewData.Placeholder(placeholder.id, true);
                notifications.setPairedItem(notifications.size() - 1, viewData);
                updateAdapter();
            }
        }

        sendFetchNotificationsRequest(bottomId, null, FetchEnd.BOTTOM, -1);
    }

    private Placeholder newPlaceholder() {
        Placeholder placeholder = Placeholder.getInstance(maxPlaceholderId);
        maxPlaceholderId--;
        return placeholder;
    }

    private void jumpToTop() {
        if(isAdded()) {
            appBarOptions.setExpanded(true, false);
            layoutManager.scrollToPosition(0);
            scrollListener.reset();
        }
    }

    private void sendFetchNotificationsRequest(String fromId, String uptoId,
        final FetchEnd fetchEnd, final int pos)
    {
        /* If there is a fetch already ongoing, record however many fetches are requested and
         * fulfill them after it's complete. */
        if(fetchEnd == FetchEnd.TOP && topLoading) {
            return;
        }
        if(fetchEnd == FetchEnd.BOTTOM && bottomLoading) {
            return;
        }
        if(fetchEnd == FetchEnd.TOP) {
            topLoading = true;
        }
        if(fetchEnd == FetchEnd.BOTTOM) {
            bottomLoading = true;
        }

        Call<List<Notification>> call = mastodonApi.getValue()
            .notifications(fromId, uptoId, LOAD_AT_ONCE,
                showNotificationsFilter ? notificationFilter : null, withMuted);

        call.enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call,
                @NonNull Response<List<Notification>> response)
            {
                if(response.isSuccessful()) {
                    String linkHeader = response.headers().get("Link");
                    onFetchNotificationsSuccess(response.body(), linkHeader, fetchEnd, pos);
                } else {
                    onFetchNotificationsFailure(new Exception(response.message()), fetchEnd, pos);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
                if(!call.isCanceled()) {
                    onFetchNotificationsFailure((Exception) t, fetchEnd, pos);
                }
            }
        });
        callList.add(call);
    }

    private void onFetchNotificationsSuccess(List<Notification> notifications, String linkHeader,
        FetchEnd fetchEnd, int pos)
    {
        List<HttpHeaderLink> links = HttpHeaderLink.parse(linkHeader);
        HttpHeaderLink next = HttpHeaderLink.findByRelationType(links, "next");
        String fromId = null;
        if(next != null) {
            fromId = next.uri.getQueryParameter("max_id");
        }

        switch(fetchEnd) {
            case TOP: {
                update(notifications, this.notifications.isEmpty() ? fromId : null);
                break;
            }
            case MIDDLE: {
                replacePlaceholderWithNotifications(notifications, pos);
                break;
            }
            case BOTTOM: {

                if(!this.notifications.isEmpty() &&
                   !this.notifications.get(this.notifications.size() - 1).isRight()) {
                    this.notifications.remove(this.notifications.size() - 1);
                    updateAdapter();
                }

                if(adapter.getItemCount() > 1) {
                    addItems(notifications, fromId);
                } else {
                    update(notifications, fromId);
                }

                break;
            }
        }

        saveNewestNotificationId(notifications);

        if(fetchEnd == FetchEnd.TOP) {
            topLoading = false;
        }
        if(fetchEnd == FetchEnd.BOTTOM) {
            bottomLoading = false;
        }

        if(notifications.size() == 0 && adapter.getItemCount() == 0) {
            this.statusView.setVisibility(View.VISIBLE);
            this.statusView.setup(R.drawable.elephant_friend_empty, R.string.message_empty, null);
        } else {
            swipeRefreshLayout.setEnabled(true);
        }
        updateFilterVisibility();
        swipeRefreshLayout.setRefreshing(false);
        progressBar.setVisibility(View.GONE);
    }

    private void onFetchNotificationsFailure(Exception exception, FetchEnd fetchEnd, int position) {
        swipeRefreshLayout.setRefreshing(false);
        if(fetchEnd == FetchEnd.MIDDLE && !notifications.get(position).isRight()) {
            Placeholder placeholder = notifications.get(position).asLeft();
            NotificationViewData placeholderVD =
                new NotificationViewData.Placeholder(placeholder.id, false);
            notifications.setPairedItem(position, placeholderVD);
            updateAdapter();
        } else if(this.notifications.isEmpty()) {
            this.statusView.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setEnabled(false);
            this.showingError = true;
            if(exception instanceof IOException) {
                this.statusView.setup(R.drawable.elephant_offline, R.string.error_network, __ -> {
                    this.progressBar.setVisibility(View.VISIBLE);
                    this.onRefresh();
                    return Unit.INSTANCE;
                });
            } else {
                this.statusView.setup(R.drawable.elephant_error, R.string.error_generic, __ -> {
                    this.progressBar.setVisibility(View.VISIBLE);
                    this.onRefresh();
                    return Unit.INSTANCE;
                });
            }
            updateFilterVisibility();
        }
        Log.e(TAG, "Fetch failure: " + exception.getMessage());

        if(fetchEnd == FetchEnd.TOP) {
            topLoading = false;
        }
        if(fetchEnd == FetchEnd.BOTTOM) {
            bottomLoading = false;
        }

        progressBar.setVisibility(View.GONE);
    }

    private void saveNewestNotificationId(List<Notification> notifications) {

        AccountEntity account = accountManager.getValue().getActiveAccount();
        if(account != null) {
            String lastNotificationId = account.getLastNotificationId();

            for(Notification noti : notifications) {
                if(isLessThan(lastNotificationId, noti.getId())) {
                    lastNotificationId = noti.getId();
                }
            }

            if(!account.getLastNotificationId().equals(lastNotificationId)) {
                Log.d(TAG, "saving newest noti id: " + lastNotificationId);
                account.setLastNotificationId(lastNotificationId);
                accountManager.getValue().saveAccount(account);
            }
        }
    }

    private void update(@Nullable List<Notification> newNotifications, @Nullable String fromId) {
        if(ListUtils.isEmpty(newNotifications)) {
            updateAdapter();
            return;
        }
        if(fromId != null) {
            bottomId = fromId;
        }
        List<Either<Placeholder, Notification>> liftedNew = liftNotificationList(newNotifications);
        if(notifications.isEmpty()) {
            notifications.addAll(liftedNew);
        } else {
            int index = notifications.indexOf(liftedNew.get(newNotifications.size() - 1));
            for(int i = 0; i < index; i++) {
                notifications.remove(0);
            }

            int newIndex = liftedNew.indexOf(notifications.get(0));
            if(newIndex == -1) {
                if(index == -1 && liftedNew.size() >= LOAD_AT_ONCE) {
                    liftedNew.add(new Either.Left<>(newPlaceholder()));
                }
                notifications.addAll(0, liftedNew);
            } else {
                notifications.addAll(0, liftedNew.subList(0, newIndex));
            }
        }
        updateAdapter();
    }

    private void addItems(List<Notification> newNotifications, @Nullable String fromId) {
        bottomId = fromId;
        if(ListUtils.isEmpty(newNotifications)) {
            return;
        }
        int end = notifications.size();
        List<Either<Placeholder, Notification>> liftedNew = liftNotificationList(newNotifications);
        Either<Placeholder, Notification> last = notifications.get(end - 1);
        if(last != null && liftedNew.indexOf(last) == -1) {
            notifications.addAll(liftedNew);
            updateAdapter();
        }
    }

    private void replacePlaceholderWithNotifications(List<Notification> newNotifications, int pos) {
        // Remove placeholder
        notifications.remove(pos);

        if(ListUtils.isEmpty(newNotifications)) {
            updateAdapter();
            return;
        }

        List<Either<Placeholder, Notification>> liftedNew = liftNotificationList(newNotifications);

        // If we fetched less posts than in the limit, it means that the hole is not filled
        // If we fetched at least as much it means that there are more posts to load and we should
        // insert new placeholder
        if(newNotifications.size() >= LOAD_AT_ONCE) {
            liftedNew.add(new Either.Left<>(newPlaceholder()));
        }

        notifications.addAll(pos, liftedNew);
        updateAdapter();
    }

    private final Function1<Notification, Either<Placeholder, Notification>> notificationLifter =
        Either.Right::new;

    private List<Either<Placeholder, Notification>> liftNotificationList(List<Notification> list) {
        return CollectionsKt.map(list, notificationLifter);
    }

    private void fullyRefreshWithProgressBar(boolean isShow) {
        resetNotificationsLoad();
        if(isShow) {
            progressBar.setVisibility(View.VISIBLE);
            statusView.setVisibility(View.GONE);
        }
        updateAdapter();
        sendFetchNotificationsRequest(null, null, FetchEnd.TOP, -1);
    }

    private void fullyRefresh() {
        fullyRefreshWithProgressBar(false);
    }

    @Nullable
    private Pair<Integer, Notification> findReplyPosition(@NonNull String statusId) {
        for(int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i).asRightOrNull();
            if(notification != null && notification.getStatus() != null &&
               notification.getType() == Notification.Type.MENTION &&
               (statusId.equals(notification.getStatus().getId()) ||
                (notification.getStatus().getReblog() != null &&
                 statusId.equals(notification.getStatus().getReblog().getId())))) {
                return new Pair<>(i, notification);
            }
        }
        return null;
    }

    private void updateAdapter() {
        // Filter null values that come from the server
        // TODO: This should be done in the ViewModel before sending it to the view (future).
        List<NotificationViewData> newList =
                CollectionsKt.mapNotNull(
                        CollectionsKt.filterIsInstance(
                                notifications.getPairedCopy(),
                                NotificationViewData.Concrete.class
                        ),
                        notificationViewData -> {
                            if (notificationViewData.getStatusViewData() == null &&
                                    notificationViewData.getType() != Notification.Type.FOLLOW) {
                                return null;
                            } else {
                                return notificationViewData;
                            }

                        }
                );

        differ.submitList(newList);
    }

    private final ListUpdateCallback listUpdateCallback = new ListUpdateCallback() {
        @Override
        public void onInserted(int position, int count) {
            if(isAdded()) {
                adapter.notifyItemRangeInserted(position, count);
                Context context = getContext();
                // scroll up when new items at the top are loaded while being at the start
                // https://github.com/tuskyapp/Tusky/pull/1905#issuecomment-677819724
                if(position == 0 && context != null && adapter.getItemCount() != count) {
                    recyclerView.scrollBy(0, Utils.dpToPx(context, -30));
                }
            }
        }

        @Override
        public void onRemoved(int position, int count) {
            adapter.notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            adapter.notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            adapter.notifyItemRangeChanged(position, count, payload);
        }
    };

    private final AsyncListDiffer<NotificationViewData> differ =
        new AsyncListDiffer<>(listUpdateCallback,
            new AsyncDifferConfig.Builder<>(diffCallback).build());

    private final NotificationsAdapter.AdapterDataSource<NotificationViewData> dataSource =
        new NotificationsAdapter.AdapterDataSource<NotificationViewData>() {
            @Override
            public int getItemCount() {
                return differ.getCurrentList().size();
            }

            @Override
            public NotificationViewData getItemAt(int pos) {
                return differ.getCurrentList().get(pos);
            }
        };

    private static final DiffUtil.ItemCallback<NotificationViewData> diffCallback =
        new DiffUtil.ItemCallback<NotificationViewData>() {

            @Override
            public boolean areItemsTheSame(NotificationViewData oldItem,
                NotificationViewData newItem)
            {
                return oldItem.getViewDataId() == newItem.getViewDataId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull NotificationViewData oldItem,
                @NonNull NotificationViewData newItem)
            {
                return false;
            }

            @Nullable
            @Override
            public Object getChangePayload(@NonNull NotificationViewData oldItem,
                @NonNull NotificationViewData newItem)
            {
                if(oldItem.deepEquals(newItem)) {
                    //If items are equal - update timestamp only
                    return Collections.singletonList(StatusBaseViewHolder.Key.KEY_CREATED);
                } else
                // If items are different - update a whole view holder
                {
                    return null;
                }
            }
        };

    @Override
    public void onResume() {
        super.onResume();
        String rawAccountNotificationFilter =
            accountManager.getValue().getActiveAccount().getNotificationsFilter();
        Set<Notification.Type> accountNotificationFilter =
            NotificationTypeConverterKt.deserialize(rawAccountNotificationFilter);
        if(!notificationFilter.equals(accountNotificationFilter)) {
            loadNotificationsFilter();
            fullyRefreshWithProgressBar(true);
        }
        startUpdateTimestamp();
    }

    /**
     * Start to update adapter every minute to refresh timestamp
     * If setting absoluteTimeView is false
     * Auto dispose observable on pause
     */
    private void startUpdateTimestamp() {
        boolean useAbsoluteTime = preferences.getBoolean("absoluteTimeView", false);
        if(!useAbsoluteTime) {
            Observable.interval(1, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread())
                .as(autoDisposable(from(this, Lifecycle.Event.ON_PAUSE)))
                .subscribe(interval -> updateAdapter());
        }

    }

    @Override
    public void onReselect() {
        jumpToTop();
    }

    private void setEmojiReactForStatus(int position, Status newStatus) {
        NotificationViewData.Concrete viewdata =
            (NotificationViewData.Concrete) notifications.getPairedItem(position);

        NotificationViewData.Concrete newViewData =
            new NotificationViewData.Concrete(viewdata.getType(), viewdata.getId(),
                viewdata.getAccount(), ViewDataUtils.statusToViewData(newStatus, false, false),
                viewdata.getEmoji(), viewdata.getEmojiUrl(), viewdata.getTarget());

        notifications.setPairedItem(position, newViewData);
        updateAdapter();
    }

    private void handleEmojiReactEvent(EmojiReactEvent event) {
        Pair<Integer, Notification> posAndNotification =
            findReplyPosition(event.getNewStatus().getActionableId());
        if(posAndNotification == null) {
            return;
        }
        //noinspection ConstantConditions
        setEmojiReactForStatus(posAndNotification.first, event.getNewStatus());
    }


    @Override
    public void onEmojiReact(final boolean react, final String emoji, final String statusId) {
        Pair<Integer, Notification> posAndNotification = findReplyPosition(statusId);
        if(posAndNotification == null) {
            return;
        }

        timelineCases.getValue().react(emoji, statusId, react)
            .observeOn(AndroidSchedulers.mainThread()).as(autoDisposable(from(this)))
            .subscribe((newStatus) -> setEmojiReactForStatus(posAndNotification.first, newStatus),
                (t) -> Log.d(TAG, "Failed to react with " + emoji + " on status: " + statusId, t));

    }

    @Override
    public void onEmojiReactMenu(@NonNull View view, final EmojiReaction emoji,
        final String statusId)
    {
        super.emojiReactMenu(statusId, emoji, view, this);
    }
}
