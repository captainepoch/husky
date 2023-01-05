/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2020  Alibek Omarov
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

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView.BufferType;
import androidx.emoji2.widget.EmojiButton;
import androidx.recyclerview.widget.RecyclerView;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.entity.EmojiReaction;
import com.keylesspalace.tusky.interfaces.StatusActionListener;
import com.keylesspalace.tusky.util.CustomEmojiHelper;
import java.util.List;

public class EmojiReactionsAdapter extends RecyclerView.Adapter<SingleViewHolder> {
    private final List<EmojiReaction> reactions;
    private final StatusActionListener listener;
    private final String statusId;

    EmojiReactionsAdapter(final List<EmojiReaction> reactions, final StatusActionListener listener, final String statusId) {
        this.reactions = reactions;
        this.listener = listener;
        this.statusId = statusId;
    }

    @Override
    public SingleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emoji_reaction, parent, false);
        return new SingleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SingleViewHolder holder, int position) {
        EmojiReaction reaction = reactions.get(position);
        SpannableStringBuilder builder = new SpannableStringBuilder(
                reaction.getName() + " " + reaction.getCount());

        EmojiButton btn = (EmojiButton) holder.itemView;

        var url = reaction.getUrl();
        if(url != null) {
            var span = CustomEmojiHelper.createEmojiSpan(url, btn, true);

            builder.setSpan(span, 0, reaction.getName().length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        btn.setText(builder, BufferType.SPANNABLE);
        btn.setActivated(reaction.getMe());
        btn.setOnClickListener(v -> {
            listener.onEmojiReactMenu(v, reaction, statusId);
        });
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return reactions.size();
    }
}

