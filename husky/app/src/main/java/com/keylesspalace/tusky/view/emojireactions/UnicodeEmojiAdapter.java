package com.keylesspalace.tusky.view.emojireactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.emoji2.widget.EmojiButton;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.adapter.SingleViewHolder;
import com.keylesspalace.tusky.util.Emojis;
import com.keylesspalace.tusky.view.CustomGridLayoutManager;
import com.keylesspalace.tusky.view.EmojiKeyboard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UnicodeEmojiAdapter
        extends RecyclerView.Adapter<SingleViewHolder>
        implements TabLayoutMediator.TabConfigurationStrategy, EmojiKeyboard.EmojiKeyboardAdapter {

    private final String id;
    private final EmojiKeyboard.OnEmojiSelectedListener listener;
    private List<String> recents = new ArrayList<>();
    private UnicodeEmojiPageAdapter recentsAdapter;

    public UnicodeEmojiAdapter(String id, EmojiKeyboard.OnEmojiSelectedListener listener) {
        super();
        this.id = id;
        this.listener = listener;
    }

    @Override
    public void onConfigureTab(TabLayout.Tab tab, int position) {
        if (position == 0) {
            tab.setIcon(R.drawable.ic_access_time);
        } else {
            tab.setText(Emojis.EMOJIS[position - 1][0]);
        }
    }

    @Override
    public int getItemCount() {
        return Emojis.EMOJIS.length + 1;
    }

    @Override
    public void onBindViewHolder(SingleViewHolder holder, int position) {
        RecyclerView recyclerView = (RecyclerView) holder.itemView;
        UnicodeEmojiPageAdapter pageAdapter = (UnicodeEmojiPageAdapter) recyclerView.getAdapter();

        if (pageAdapter != null) {
            if (position == 0) {
                pageAdapter.updateData(recents);
            } else {
                pageAdapter.updateData(Arrays.asList(Emojis.EMOJIS[position - 1]));
            }
        }
    }

    @NonNull
    @Override
    public SingleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.item_emoji_keyboard_page, parent, false);
        RecyclerView recyclerView = (RecyclerView) view;

        final float density = parent.getContext().getResources().getDisplayMetrics().density;
        final int emojiSizePx = (int) (48.0f * density);

        CustomGridLayoutManager layoutManager = new CustomGridLayoutManager(parent.getContext(), emojiSizePx);
        recyclerView.setLayoutManager(layoutManager);

        UnicodeEmojiPageAdapter pageAdapter = new UnicodeEmojiPageAdapter(new ArrayList<>(), id, listener);
        recyclerView.setAdapter(pageAdapter);

        return new SingleViewHolder(view);
    }

    @Override
    public void onRecentsUpdate(Set<String> set) {
        recents = new ArrayList<>(set);
        Collections.reverse(recents);
        if (recentsAdapter != null) {
            recentsAdapter.updateData(recents);
        }
    }

    private abstract class UnicodeEmojiBasePageAdapter extends RecyclerView.Adapter<SingleViewHolder> {
        private final EmojiKeyboard.OnEmojiSelectedListener listener;
        private final String id;

        public UnicodeEmojiBasePageAdapter(String id, EmojiKeyboard.OnEmojiSelectedListener listener) {
            this.id = id;
            this.listener = listener;
        }

        abstract public String getEmoji(int position);

        @Override
        public void onBindViewHolder(SingleViewHolder holder, int position) {
            String emoji = getEmoji(position);
            EmojiButton btn = (EmojiButton)holder.itemView;
            btn.setText(emoji);
            btn.setOnClickListener(v -> listener.onEmojiSelected(id, emoji));
        }

        @Override
        public SingleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.item_emoji_keyboard_emoji, parent, false);
            return new SingleViewHolder(view);
        }
    }

    private class UnicodeEmojiPageAdapter extends UnicodeEmojiBasePageAdapter {

        private List<String> emojis;

        public UnicodeEmojiPageAdapter(List<String> emojis, String id, EmojiKeyboard.OnEmojiSelectedListener listener) {
            super(id, listener);
            this.emojis = emojis == null ? new ArrayList<>() : emojis;
        }

        @Override
        public int getItemViewType(int position) {
            return 1;
        }

        public void updateData(List<String> newEmojis) {
            this.emojis = newEmojis;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return emojis.size();
        }

        @Override
        public String getEmoji(int position) {
            return emojis.get(position);
        }
    }
}
