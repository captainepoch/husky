package com.keylesspalace.tusky.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.emoji.widget.EmojiAppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.entity.EmojiReaction;
import com.keylesspalace.tusky.interfaces.StatusActionListener;
import com.keylesspalace.tusky.util.CardViewMode;
import com.keylesspalace.tusky.util.CustomEmojiHelper;
import com.keylesspalace.tusky.util.LinkHelper;
import com.keylesspalace.tusky.util.StatusDisplayOptions;
import com.keylesspalace.tusky.viewdata.StatusViewData;

import java.text.DateFormat;
import java.util.List;
import java.util.Date;

    
public class EmojiReactionsAdapter extends RecyclerView.Adapter<SingleViewHolder> {
    private final List<EmojiReaction> reactions;
    private final StatusActionListener listener;
    private final String statusId;

    EmojiReactionsAdapter(final List<EmojiReaction> reactions, final StatusActionListener listener, final String statusId) {
        this.reactions = reactions;
        this.listener  = listener;
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

        EmojiAppCompatButton btn = (EmojiAppCompatButton)holder.itemView;

        var url = reaction.getUrl();
        if (url != null) {
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

