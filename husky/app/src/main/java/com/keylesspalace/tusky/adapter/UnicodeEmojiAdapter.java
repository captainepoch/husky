package com.keylesspalace.tusky.adapter;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.emoji2.widget.EmojiButton;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.util.Emojis;
import com.keylesspalace.tusky.view.EmojiKeyboard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UnicodeEmojiAdapter
    extends RecyclerView.Adapter<SingleViewHolder>
    implements TabLayoutMediator.TabConfigurationStrategy, EmojiKeyboard.EmojiKeyboardAdapter {

    private String id;
    private List<String> recents;
    private EmojiKeyboard.OnEmojiSelectedListener listener;
    private RecyclerView recentsView;

    private final static float BUTTON_WIDTH_DP = 65.0f; // empirically found value :(

    public UnicodeEmojiAdapter(String id, EmojiKeyboard.OnEmojiSelectedListener listener) {
        super();
        this.id = id;
        this.listener = listener;
    }

    @Override
    public void onConfigureTab(TabLayout.Tab tab, int position) {
        if(position == 0) {
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
        if(position == 0) {
            recentsView = ((RecyclerView)holder.itemView);
            recentsView.setAdapter(new UnicodeEmojiPageAdapter(recents, id, listener));

            recentsView.post(recentsView::requestLayout);
        } else {
            ((RecyclerView)holder.itemView).setAdapter(
                new UnicodeEmojiPageAdapter(Arrays.asList(Emojis.EMOJIS[position - 1]), id, listener));
        }
    }

    @Override
    public SingleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_emoji_keyboard_page, parent, false);
        SingleViewHolder holder = new SingleViewHolder(view);

        DisplayMetrics dm = parent.getContext().getResources().getDisplayMetrics();
        float wdp = dm.widthPixels / dm.density;
        int rows = (int) (wdp / BUTTON_WIDTH_DP + 0.5);

        ((RecyclerView)view).setLayoutManager(new GridLayoutManager(view.getContext(), rows));
        return holder;
    }

    @Override
    public void onRecentsUpdate(Set<String> set) {
        recents = new ArrayList<>(set);
        Collections.reverse(recents);
        if(recentsView != null) {
            recentsView.getAdapter().notifyDataSetChanged();
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
        private final List<String> emojis;

        public UnicodeEmojiPageAdapter(List<String> emojis, String id, EmojiKeyboard.OnEmojiSelectedListener listener) {
            super(id, listener);
            this.emojis = emojis;
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

