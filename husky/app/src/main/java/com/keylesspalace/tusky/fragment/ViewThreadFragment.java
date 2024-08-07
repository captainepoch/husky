/* Copyright 2017 Andrew Dawson
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

package com.keylesspalace.tusky.fragment;

import static com.uber.autodispose.AutoDispose.autoDisposable;
import static com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from;
import static org.koin.java.KoinJavaComponent.inject;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import com.keylesspalace.tusky.AccountListActivity;
import com.keylesspalace.tusky.BaseActivity;
import com.keylesspalace.tusky.BuildConfig;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.ViewThreadActivity;
import com.keylesspalace.tusky.adapter.StatusBaseViewHolder;
import com.keylesspalace.tusky.adapter.StatusDetailedViewHolder;
import com.keylesspalace.tusky.adapter.StatusViewHolder;
import com.keylesspalace.tusky.adapter.ThreadAdapter;
import com.keylesspalace.tusky.appstore.BlockEvent;
import com.keylesspalace.tusky.appstore.BookmarkEvent;
import com.keylesspalace.tusky.appstore.EmojiReactEvent;
import com.keylesspalace.tusky.appstore.EventHub;
import com.keylesspalace.tusky.appstore.FavoriteEvent;
import com.keylesspalace.tusky.appstore.MuteEvent;
import com.keylesspalace.tusky.appstore.ReblogEvent;
import com.keylesspalace.tusky.appstore.StatusComposedEvent;
import com.keylesspalace.tusky.appstore.StatusDeletedEvent;
import com.keylesspalace.tusky.components.compose.ComposeActivity;
import com.keylesspalace.tusky.components.compose.ComposeActivity.ComposeOptions;
import com.keylesspalace.tusky.components.instance.domain.repository.InstanceRepository;
import com.keylesspalace.tusky.entity.EmojiReaction;
import com.keylesspalace.tusky.entity.Filter;
import com.keylesspalace.tusky.entity.Poll;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.entity.StatusContext;
import com.keylesspalace.tusky.interfaces.StatusActionListener;
import com.keylesspalace.tusky.settings.PrefKeys;
import com.keylesspalace.tusky.util.CardViewMode;
import com.keylesspalace.tusky.util.ListStatusAccessibilityDelegate;
import com.keylesspalace.tusky.util.PairedList;
import com.keylesspalace.tusky.util.StatusDisplayOptions;
import com.keylesspalace.tusky.util.ViewDataUtils;
import com.keylesspalace.tusky.view.ConversationLineItemDecoration;
import com.keylesspalace.tusky.viewdata.StatusViewData;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public final class ViewThreadFragment extends SFragment
    implements SwipeRefreshLayout.OnRefreshListener, StatusActionListener
{

    private static final String TAG = "ViewThreadFragment";

    private final EventHub eventHub = (EventHub) inject(EventHub.class).getValue();
    private final InstanceRepository instanceRepo = (InstanceRepository) inject(InstanceRepository.class).getValue();
    private final SharedPreferences preferences = (SharedPreferences) inject(SharedPreferences.class).getValue();
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ThreadAdapter adapter;
    private String thisThreadsStatusId;
    private boolean alwaysShowSensitiveMedia;
    private boolean alwaysOpenSpoiler;

    private int statusIndex = 0;

    private final PairedList<Status, StatusViewData.Concrete> statuses =
        new PairedList<>(new Function<Status, StatusViewData.Concrete>() {
            @Override
            public StatusViewData.Concrete apply(Status input) {
                return ViewDataUtils.statusToViewData(input, alwaysShowSensitiveMedia,
                    alwaysOpenSpoiler);
            }
        });

    public static ViewThreadFragment newInstance(String id) {
        Bundle arguments = new Bundle(1);
        ViewThreadFragment fragment = new ViewThreadFragment();
        arguments.putString("id", id);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thisThreadsStatusId = getArguments().getString("id");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_view_thread, container, false);
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        recyclerView = rootView.findViewById(R.id.recyclerView);

        statuses.clear();

        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getInstanceInfo();

        eventHub.getEvents().observeOn(AndroidSchedulers.mainThread())
            .as(autoDisposable(from(this, Lifecycle.Event.ON_DESTROY))).subscribe(event -> {
                if(event instanceof FavoriteEvent) {
                    handleFavEvent((FavoriteEvent) event);
                } else if(event instanceof ReblogEvent) {
                    handleReblogEvent((ReblogEvent) event);
                } else if(event instanceof BookmarkEvent) {
                    handleBookmarkEvent((BookmarkEvent) event);
                } else if(event instanceof BlockEvent) {
                    removeAllByAccountId(((BlockEvent) event).getAccountId());
                } else if(event instanceof MuteEvent) {
                    handleMuteEvent((MuteEvent) event);
                } else if(event instanceof StatusComposedEvent) {
                    handleStatusComposedEvent((StatusComposedEvent) event);
                } else if(event instanceof StatusDeletedEvent) {
                    handleStatusDeletedEvent((StatusDeletedEvent) event);
                } else if(event instanceof EmojiReactEvent) {
                    handleEmojiReactEvent((EmojiReactEvent) event);
                }
            });

        if(thisThreadsStatusPosition != -1) {
            recyclerView.scrollToPosition(thisThreadsStatusPosition);
        }
    }

    private void getInstanceInfo() {
        instanceRepo.getInstanceInfoRx()
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(autoDisposable(from(this, Event.ON_DESTROY)))
                    .subscribe(instance -> {
                        Timber.d("Has quoting posts [%s]", instance.getQuotePosting());

                        createAdapter(instance.getQuotePosting());
                        setupRecyclerView();
                        updateAdapter();

                        makeRequests();
                    });
    }

    private void createAdapter(boolean canQuotePosts) {
        StatusDisplayOptions statusDisplayOptions =
                new StatusDisplayOptions(
                        preferences.getBoolean("animateGifAvatars", false),
                        accountManager.getValue().getActiveAccount().getMediaPreviewEnabled(),
                        preferences.getBoolean("absoluteTimeView", false),
                        preferences.getBoolean("showBotOverlay", true),
                        preferences.getBoolean("useBlurhash", true),
                        preferences.getBoolean("showCardsInTimelines", false) ? CardViewMode.INDENTED :
                                CardViewMode.NONE, preferences.getBoolean("confirmReblogs", true),
                        preferences.getBoolean(PrefKeys.RENDER_STATUS_AS_MENTION, true),
                        preferences.getBoolean(PrefKeys.WELLBEING_HIDE_STATS_POSTS, false),
                        canQuotePosts
                );
        adapter = new ThreadAdapter(statusDisplayOptions, this);
    }

    private void setupRecyclerView() {
        Context context = getContext();
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.tusky_blue);

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAccessibilityDelegateCompat(
                new ListStatusAccessibilityDelegate(recyclerView, this, statuses::getPairedItemOrNull));
        DividerItemDecoration divider =
                new DividerItemDecoration(context, layoutManager.getOrientation());
        recyclerView.addItemDecoration(divider);

        recyclerView.addItemDecoration(new ConversationLineItemDecoration(context));
        alwaysShowSensitiveMedia =
                accountManager.getValue().getActiveAccount().getAlwaysShowSensitiveMedia();
        alwaysOpenSpoiler = accountManager.getValue().getActiveAccount().getAlwaysOpenSpoiler();
        reloadFilters(preferences, false);

        recyclerView.setAdapter(adapter);
    }

    public void onRevealPressed() {
        boolean allExpanded = allExpanded();
        for(int i = 0; i < statuses.size(); i++) {
            StatusViewData.Concrete newViewData =
                new StatusViewData.Concrete.Builder(statuses.getPairedItem(i)).setIsExpanded(
                    !allExpanded).createStatusViewData();
            statuses.setPairedItem(i, newViewData);
        }
        updateAdapter();
        updateRevealIcon();
    }

    private boolean allExpanded() {
        boolean allExpanded = true;
        for(int i = 0; i < statuses.size(); i++) {
            if(!statuses.getPairedItem(i).isExpanded()) {
                allExpanded = false;
                break;
            }
        }
        return allExpanded;
    }

    private void makeRequests() {
        sendStatusRequest(thisThreadsStatusId);
        sendThreadRequest(thisThreadsStatusId);
    }

    @Override
    public void onRefresh() {
        makeRequests();
    }

    @Override
    public void onReply(int position) {
        super.reply(statuses.get(position));
    }

    @Override
    public void onMenuReblog(final boolean reblog, final int position) {
        onReblog(reblog, position, false);
    }

    @Override
    public void onReblog(final boolean reblog, final int position, final boolean canQuote) {
        final Status status = statuses.get(position);

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
                             (newStatus) -> updateStatus(position, status, reblog),
                             (err) -> Timber.e(
                                     err,
                                     "Failed to reblog status %s, Error[%s]",
                                     status.getId(),
                                     err.getMessage()
                             )
                     );
    }

    @Override
    public void onFavourite(final boolean favourite, final int position) {
        final Status status = statuses.get(position);

        timelineCases.getValue().favourite(statuses.get(position), favourite)
            .observeOn(AndroidSchedulers.mainThread()).as(autoDisposable(from(this)))
            .subscribe((newStatus) -> updateStatus(position, newStatus, false),
                (t) -> Log.d(TAG, "Failed to favourite status: " + status.getId(), t));
    }

    @Override
    public void onBookmark(final boolean bookmark, final int position) {
        final Status status = statuses.get(position);

        timelineCases.getValue().bookmark(statuses.get(position), bookmark)
            .observeOn(AndroidSchedulers.mainThread()).as(autoDisposable(from(this)))
            .subscribe((newStatus) -> updateStatus(position, newStatus, false),
                (t) -> Log.d(TAG, "Failed to bookmark status: " + status.getId(), t));
    }

    // TODO: remove boolean reblog because it's a nasty hack
    private void updateStatus(int position, Status status, boolean reblog) {
        if(position >= 0 && position < statuses.size()) {
            Status actionableStatus = status.getActionableStatus();

            if (reblog) {
                if (!actionableStatus.getReblogged() && recyclerView != null) {
                    ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
                    if (holder instanceof StatusViewHolder || holder instanceof StatusDetailedViewHolder) {
                        ((StatusBaseViewHolder) holder).reblogButtonAnimate();
                    }
                }
            }

            StatusViewData.Concrete viewData =
                new StatusViewData.Builder(statuses.getPairedItem(position)).setReblogged(
                        actionableStatus.getReblogged())
                    .setReblogsCount(actionableStatus.getReblogsCount())
                    .setFavourited(actionableStatus.getFavourited())
                    .setBookmarked(actionableStatus.getBookmarked())
                    .setFavouritesCount(actionableStatus.getFavouritesCount())
                    .createStatusViewData();
            statuses.setPairedItem(position, viewData);

            adapter.setItem(position, viewData, true);
        }
    }

    @Override
    public void onMore(@NonNull View view, int position) {
        super.more(statuses.get(position), view, position);
    }

    @Override
    public void onViewMedia(int position, int attachmentIndex, @NonNull View view) {
        Status status = statuses.get(position);
        super.viewMedia(attachmentIndex, status, view);
    }

    @Override
    public void onViewThread(int position) {
        Status status = statuses.get(position);
        if(thisThreadsStatusId.equals(status.getId())) {
            // If already viewing this thread, don't reopen it.
            return;
        }
        super.viewThread(status);
    }

    @Override
    public void onViewReplyTo(int position) {
        Status status = statuses.get(position);
        if(thisThreadsStatusId.equals(status.getInReplyToId())) {
            return;
        }
        super.onShowReplyTo(status.getInReplyToId());
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
        // there should be no reblogs in the thread but let's implement it to be sure
        super.openReblog(statuses.get(position));
    }

    @Override
    public void onExpandedChange(boolean expanded, int position) {
        StatusViewData.Concrete newViewData =
            new StatusViewData.Builder(statuses.getPairedItem(position)).setIsExpanded(expanded)
                .createStatusViewData();
        statuses.setPairedItem(position, newViewData);
        adapter.setItem(position, newViewData, true);
        updateRevealIcon();
    }

    @Override
    public void onContentHiddenChange(boolean isShowing, int position) {
        StatusViewData.Concrete newViewData = new StatusViewData.Builder(
            statuses.getPairedItem(position)).setIsShowingSensitiveContent(isShowing)
            .createStatusViewData();
        statuses.setPairedItem(position, newViewData);
        adapter.setItem(position, newViewData, true);
    }

    @Override
    public void onLoadMore(int position) {

    }

    @Override
    public void onShowReblogs(int position) {
        String statusId = statuses.get(position).getId();
        Intent intent =
            AccountListActivity.newIntent(getContext(), AccountListActivity.Type.REBLOGGED,
                statusId);
        ((BaseActivity) getActivity()).startActivityWithSlideInAnimation(intent);
    }

    @Override
    public void onShowFavs(int position) {
        String statusId = statuses.get(position).getId();
        Intent intent =
            AccountListActivity.newIntent(getContext(), AccountListActivity.Type.FAVOURITED,
                statusId);
        ((BaseActivity) getActivity()).startActivityWithSlideInAnimation(intent);
    }

    @Override
    public void onContentCollapsedChange(boolean isCollapsed, int position) {
        if(position < 0 || position >= statuses.size()) {
            Log.e(TAG,
                String.format("Tried to access out of bounds status position: %d of %d", position,
                    statuses.size() - 1));
            return;
        }

        StatusViewData.Concrete status = statuses.getPairedItem(position);
        if(status == null) {
            // Statuses PairedList contains a base type of StatusViewData.Concrete and also doesn't
            // check for null values when adding values to it although this doesn't seem to be an issue.
            Log.e(TAG, String.format(
                "Expected StatusViewData.Concrete, got null instead at position: %d of %d",
                position, statuses.size() - 1));
            return;
        }

        StatusViewData.Concrete updatedStatus =
            new StatusViewData.Builder(status).setCollapsed(isCollapsed).createStatusViewData();
        statuses.setPairedItem(position, updatedStatus);
        recyclerView.post(() -> adapter.setItem(position, updatedStatus, true));
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
    public void removeItem(int position) {
        if(position == statusIndex) {
            //the status got removed, close the activity
            getActivity().finish();
        }
        statuses.remove(position);
        updateAdapter();
    }

    public void onVoteInPoll(int position, @NonNull List<Integer> choices) {
        final Status status = statuses.get(position).getActionableStatus();

        setVoteForPoll(position, status.getPoll().votedCopy(choices));

        timelineCases.getValue().voteInPoll(status, choices)
            .observeOn(AndroidSchedulers.mainThread()).as(autoDisposable(from(this)))
            .subscribe((newPoll) -> setVoteForPoll(position, newPoll),
                (t) -> Log.d(TAG, "Failed to vote in poll: " + status.getId(), t));

    }

    private void setVoteForPoll(int position, Poll newPoll) {

        StatusViewData.Concrete viewData = statuses.getPairedItem(position);

        StatusViewData.Concrete newViewData =
            new StatusViewData.Builder(viewData).setPoll(newPoll).createStatusViewData();
        statuses.setPairedItem(position, newViewData);
        adapter.setItem(position, newViewData, true);
    }

    private void updateAdapter() {
        adapter.setStatuses(statuses.getPairedCopy());
    }

    private void removeAllByAccountId(String accountId) {
        Status status = null;
        if(!statuses.isEmpty()) {
            status = statuses.get(statusIndex);
        }
        // using iterator to safely remove items while iterating
        Iterator<Status> iterator = statuses.iterator();
        while(iterator.hasNext()) {
            Status s = iterator.next();
            if(s.getAccount().getId().equals(accountId) ||
               s.getActionableStatus().getAccount().getId().equals(accountId)) {
                iterator.remove();
            }
        }
        statusIndex = statuses.indexOf(status);
        if(statusIndex == -1) {
            //the status got removed, close the activity
            getActivity().finish();
            return;
        }
        adapter.setDetailedStatusPosition(statusIndex);
        updateAdapter();
    }

    private void sendStatusRequest(final String id) {
        Call<Status> call = mastodonApi.getValue().status(id);
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if(response.isSuccessful()) {
                    int position = setStatus(response.body());
                    recyclerView.scrollToPosition(position);
                } else {
                    onThreadRequestFailure(id);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                onThreadRequestFailure(id);
            }
        });
        callList.add(call);
    }

    private void sendThreadRequest(final String id) {
        Call<StatusContext> call = mastodonApi.getValue().statusContext(id);
        call.enqueue(new Callback<StatusContext>() {
            @Override
            public void onResponse(@NonNull Call<StatusContext> call,
                @NonNull Response<StatusContext> response)
            {
                StatusContext context = response.body();
                if(response.isSuccessful() && context != null) {
                    swipeRefreshLayout.setRefreshing(false);
                    setContext(context.getAncestors(), context.getDescendants());
                } else {
                    onThreadRequestFailure(id);
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatusContext> call, @NonNull Throwable t) {
                onThreadRequestFailure(id);
            }
        });
        callList.add(call);
    }

    private void onThreadRequestFailure(final String id) {
        View view = getView();
        swipeRefreshLayout.setRefreshing(false);
        if(view != null) {
            Snackbar.make(view, R.string.error_generic, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_retry, v -> {
                    sendThreadRequest(id);
                    sendStatusRequest(id);
                }).show();
        } else {
            Log.e(TAG, "Couldn't display thread fetch error message");
        }
    }

    private int setStatus(Status status) {
        if(statuses.size() > 0 && statusIndex < statuses.size() &&
           statuses.get(statusIndex).equals(status)) {
            // Do not add this status on refresh, it's already in there.
            statuses.set(statusIndex, status);
            return statusIndex;
        }
        int i = statusIndex;
        statuses.add(i, status);
        adapter.setDetailedStatusPosition(i);
        adapter.addItem(i, statuses.getPairedItem(i));
        updateRevealIcon();
        return i;
    }

    private void setContext(List<Status> unfilteredAncestors, List<Status> unfilteredDescendants) {
        Status mainStatus = null;

        // In case of refresh, remove old ancestors and descendants first. We'll remove all blindly,
        // as we have no guarantee on their order to be the same as before
        int oldSize = statuses.size();
        if(oldSize > 1) {
            mainStatus = statuses.get(statusIndex);
            statuses.clear();
            adapter.clearItems();
        }

        ArrayList<Status> ancestors = new ArrayList<>();
        for(Status status : unfilteredAncestors) {
            if(!shouldFilterStatus(status)) {
                ancestors.add(status);
            }
        }

        // Insert newly fetched ancestors
        statusIndex = ancestors.size();
        adapter.setDetailedStatusPosition(statusIndex);
        statuses.addAll(0, ancestors);
        List<StatusViewData.Concrete> ancestorsViewDatas =
            statuses.getPairedCopy().subList(0, statusIndex);
        if(BuildConfig.DEBUG && ancestors.size() != ancestorsViewDatas.size()) {
            String error = String.format(Locale.getDefault(),
                "Incorrectly got statusViewData sublist." +
                " ancestors.size == %d ancestorsViewDatas.size == %d," + " statuses.size == %d",
                ancestors.size(), ancestorsViewDatas.size(), statuses.size());
            throw new AssertionError(error);
        }
        adapter.addAll(0, ancestorsViewDatas);

        if(mainStatus != null) {
            // In case we needed to delete everything (which is way easier than deleting
            // everything except one), re-insert the remaining status here.
            // Not filtering the main status, since the user explicitly chose to be here
            statuses.add(statusIndex, mainStatus);
            StatusViewData.Concrete viewData = statuses.getPairedItem(statusIndex);

            adapter.addItem(statusIndex, viewData);
        }

        ArrayList<Status> descendants = new ArrayList<>();
        for(Status status : unfilteredDescendants) {
            if(!shouldFilterStatus(status)) {
                descendants.add(status);
            }
        }

        // Insert newly fetched descendants
        statuses.addAll(descendants);
        List<StatusViewData.Concrete> descendantsViewData;
        descendantsViewData =
            statuses.getPairedCopy().subList(statuses.size() - descendants.size(), statuses.size());
        if(BuildConfig.DEBUG && descendants.size() != descendantsViewData.size()) {
            String error = String.format(Locale.getDefault(),
                "Incorrectly got statusViewData sublist." +
                " descendants.size == %d descendantsViewData.size == %d," + " statuses.size == %d",
                descendants.size(), descendantsViewData.size(), statuses.size());
            throw new AssertionError(error);
        }
        adapter.addAll(descendantsViewData);
        updateRevealIcon();
    }

    private void setMutedStatusForStatus(int position, Status status, boolean muted) {
        StatusViewData.Builder statusViewData =
            new StatusViewData.Builder(statuses.getPairedItem(position));
        statusViewData.setMuted(muted);

        statuses.setPairedItem(position, statusViewData.createStatusViewData());
    }

    private void handleMuteEvent(MuteEvent event) {
        String id = event.getAccountId();
        boolean muting = event.getMute();

        if(isFilteringMuted()) {
            removeAllByAccountId(id);
        } else {
            for(int i = 0; i < statuses.size(); i++) {
                Status status = statuses.get(i);
                if(status != null && status.getAccount().getId().equals(id) &&
                   !status.isThreadMuted()) {
                    setMutedStatusForStatus(i, status, muting);
                }
            }
            updateAdapter();
        }
    }


    private void handleFavEvent(FavoriteEvent event) {
        Pair<Integer, Status> posAndStatus = findStatusAndPos(event.getStatusId());
        if(posAndStatus == null) {
            return;
        }

        boolean favourite = event.getFavourite();
        posAndStatus.second.setFavourited(favourite);

        if(posAndStatus.second.getReblog() != null) {
            posAndStatus.second.getReblog().setFavourited(favourite);
        }

        StatusViewData.Concrete viewdata = statuses.getPairedItem(posAndStatus.first);

        StatusViewData.Builder viewDataBuilder = new StatusViewData.Builder((viewdata));
        viewDataBuilder.setFavourited(favourite);

        StatusViewData.Concrete newViewData = viewDataBuilder.createStatusViewData();

        statuses.setPairedItem(posAndStatus.first, newViewData);
        adapter.setItem(posAndStatus.first, newViewData, true);
    }

    private void handleReblogEvent(ReblogEvent event) {
        Pair<Integer, Status> posAndStatus = findStatusAndPos(event.getStatusId());
        if(posAndStatus == null) {
            return;
        }

        boolean reblog = event.getReblog();
        posAndStatus.second.setReblogged(reblog);

        if(posAndStatus.second.getReblog() != null) {
            posAndStatus.second.getReblog().setReblogged(reblog);
        }

        StatusViewData.Concrete viewdata = statuses.getPairedItem(posAndStatus.first);

        StatusViewData.Builder viewDataBuilder = new StatusViewData.Builder((viewdata));
        viewDataBuilder.setReblogged(reblog);

        StatusViewData.Concrete newViewData = viewDataBuilder.createStatusViewData();

        statuses.setPairedItem(posAndStatus.first, newViewData);
        adapter.setItem(posAndStatus.first, newViewData, true);
    }

    private void handleBookmarkEvent(BookmarkEvent event) {
        Pair<Integer, Status> posAndStatus = findStatusAndPos(event.getStatusId());
        if(posAndStatus == null) {
            return;
        }

        boolean bookmark = event.getBookmark();
        posAndStatus.second.setBookmarked(bookmark);

        if(posAndStatus.second.getReblog() != null) {
            posAndStatus.second.getReblog().setBookmarked(bookmark);
        }

        StatusViewData.Concrete viewdata = statuses.getPairedItem(posAndStatus.first);

        StatusViewData.Builder viewDataBuilder = new StatusViewData.Builder((viewdata));
        viewDataBuilder.setBookmarked(bookmark);

        StatusViewData.Concrete newViewData = viewDataBuilder.createStatusViewData();

        statuses.setPairedItem(posAndStatus.first, newViewData);
        adapter.setItem(posAndStatus.first, newViewData, true);
    }

    private void handleStatusComposedEvent(StatusComposedEvent event) {
        Status eventStatus = event.getStatus();
        if(eventStatus.getInReplyToId() == null) {
            return;
        }

        if(eventStatus.getInReplyToId().equals(thisThreadsStatusId)) {
            insertStatus(eventStatus, statuses.size());
        } else {
            // If new status is a reply to some status in the thread, insert new status after it
            // We only check statuses below main status, ones on top don't belong to this thread
            for(int i = statusIndex; i < statuses.size(); i++) {
                Status status = statuses.get(i);
                if(eventStatus.getInReplyToId().equals(status.getId())) {
                    insertStatus(eventStatus, i + 1);
                    break;
                }
            }
        }
    }

    private int thisThreadsStatusPosition = -1;

    private void insertStatus(Status status, int at) {
        statuses.add(at, status);
        adapter.addItem(at, statuses.getPairedItem(at));
        if(status.getId().equals(thisThreadsStatusId)) {
            thisThreadsStatusPosition = at;
        }
    }

    private void handleStatusDeletedEvent(StatusDeletedEvent event) {
        Pair<Integer, Status> posAndStatus = findStatusAndPos(event.getStatusId());
        if(posAndStatus == null) {
            return;
        }

        @SuppressWarnings("ConstantConditions") int pos = posAndStatus.first;
        statuses.remove(pos);
        adapter.removeItem(pos);
    }

    @Nullable
    private Pair<Integer, Status> findStatusAndPos(@NonNull String statusId) {
        for(int i = 0; i < statuses.size(); i++) {
            if(statusId.equals(statuses.get(i).getId())) {
                return new Pair<>(i, statuses.get(i));
            }
        }
        return null;
    }

    private void updateRevealIcon() {
        ViewThreadActivity activity = ((ViewThreadActivity) getActivity());
        if(activity == null) {
            return;
        }

        boolean hasAnyWarnings = false;
        // Statuses are updated from the main thread so nothing should change while iterating
        for(int i = 0; i < statuses.size(); i++) {
            if(!TextUtils.isEmpty(statuses.get(i).getSpoilerText())) {
                hasAnyWarnings = true;
                break;
            }
        }
        if(!hasAnyWarnings) {
            activity.setRevealButtonState(ViewThreadActivity.REVEAL_BUTTON_HIDDEN);
            return;
        }
        activity.setRevealButtonState(allExpanded() ? ViewThreadActivity.REVEAL_BUTTON_HIDE :
            ViewThreadActivity.REVEAL_BUTTON_REVEAL);
    }

    @Override
    protected boolean filterIsRelevant(@NonNull Filter filter) {
        return filter.getContext().contains(Filter.THREAD);
    }

    @Override
    protected void refreshAfterApplyingFilters() {
        onRefresh();
    }

    private void setEmojiReactionForStatus(int position, Status status) {
        StatusViewData.Concrete newViewData = ViewDataUtils.statusToViewData(status, false, false);

        statuses.setPairedItem(position, newViewData);
        adapter.setItem(position, newViewData, true);
    }

    public void handleEmojiReactEvent(EmojiReactEvent event) {
        Pair<Integer, Status> posAndStatus =
            findStatusAndPos(event.getNewStatus().getActionableId());
        if(posAndStatus == null) {
            return;
        }
        setEmojiReactionForStatus(posAndStatus.first, event.getNewStatus());
    }

    @Override
    public void onEmojiReact(final boolean react, final String emoji, final String statusId) {
        Pair<Integer, Status> statusAndPos = findStatusAndPos(statusId);

        if(statusAndPos == null) {
            return;
        }
        int position = statusAndPos.first;

        timelineCases.getValue().react(emoji, statusId, react)
            .observeOn(AndroidSchedulers.mainThread()).as(autoDisposable(from(this)))
            .subscribe((newStatus) -> setEmojiReactionForStatus(position, newStatus),
                (t) -> Log.d(TAG, "Failed to react with " + emoji + " on status: " + statusId, t));
    }

    @Override
    public void onEmojiReactMenu(@NonNull View view, final EmojiReaction emoji,
        final String statusId)
    {
        super.emojiReactMenu(statusId, emoji, view, this);
    }
}
