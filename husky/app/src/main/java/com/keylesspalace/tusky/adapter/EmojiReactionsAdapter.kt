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

package com.keylesspalace.tusky.adapter

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView.BufferType.SPANNABLE
import androidx.emoji2.widget.EmojiButton
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.keylesspalace.tusky.R.layout
import com.keylesspalace.tusky.entity.EmojiReaction
import com.keylesspalace.tusky.interfaces.StatusActionListener
import com.keylesspalace.tusky.util.createEmojiSpan

class EmojiReactionsAdapter internal constructor(
    private val reactions: List<EmojiReaction>,
    private val listener: StatusActionListener,
    private val statusId: String
) : Adapter<SingleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layout.item_emoji_reaction, parent, false)
        return SingleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SingleViewHolder, position: Int) {
        val reaction = reactions[position]
        val builder = SpannableStringBuilder("${reaction.name} ${reaction.count}")
        val btn = holder.itemView as EmojiButton

        val url = reaction.url
        if (url != null) {
            builder.setSpan(
                createEmojiSpan(url, btn, true),
                0, reaction.name.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        btn.apply {
            setText(builder, SPANNABLE)
            isActivated = reaction.me
            setOnClickListener { v: View? ->
                listener.onEmojiReactMenu(
                    v!!, reaction, statusId
                )
            }
        }
    }

    // Number of rows
    override fun getItemCount(): Int {
        return reactions.size
    }
}
