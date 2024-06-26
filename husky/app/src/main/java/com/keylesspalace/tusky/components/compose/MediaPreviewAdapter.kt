/* Copyright 2019 Tusky Contributors
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

package com.keylesspalace.tusky.components.compose

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.components.compose.view.ProgressImageView
import com.keylesspalace.tusky.components.compose.view.ProgressTextView

class MediaPreviewAdapter(
    context: Context,
    private val onAddCaption: (ComposeActivity.QueuedMedia) -> Unit,
    private val onRemove: (ComposeActivity.QueuedMedia) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun submitList(list: List<ComposeActivity.QueuedMedia>) {
        this.differ.submitList(list)
    }

    private fun onMediaClick(position: Int, view: View) {
        val item = differ.currentList[position]
        val popup = PopupMenu(view.context, view)
        val addCaptionId = 1
        val removeId = 2
        popup.menu.add(0, addCaptionId, 0, R.string.action_set_caption)
        popup.menu.add(0, removeId, 0, R.string.action_remove)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                addCaptionId -> onAddCaption(item)
                removeId -> onRemove(item)
            }
            true
        }
        popup.show()
    }

    private val thumbnailViewSize =
        context.resources.getDimensionPixelSize(R.dimen.compose_media_preview_size)

    override fun getItemCount(): Int = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        val item = differ.currentList[position]
        return item.type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            ComposeActivity.QueuedMedia.Type.UNKNOWN -> {
                return TextViewHolder(ProgressTextView(parent.context))
            }
            else -> {
                return PreviewViewHolder(ProgressImageView(parent.context))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = differ.currentList[position]

        when (item.type) {
            ComposeActivity.QueuedMedia.Type.UNKNOWN -> {
                (holder as TextViewHolder).view.setText(item.originalFileName)
                holder.view.setChecked(!item.description.isNullOrEmpty())
                holder.view.setProgress(item.uploadPercent)
            }
            ComposeActivity.QueuedMedia.Type.AUDIO -> {
                (holder as PreviewViewHolder).view.setChecked(!item.description.isNullOrEmpty())
                holder.view.setProgress(item.uploadPercent)
                holder.view.setImageResource(R.drawable.ic_music_box_preview_24dp)
            }
            else -> {
                (holder as PreviewViewHolder).view.setChecked(!item.description.isNullOrEmpty())
                holder.view.setProgress(item.uploadPercent)

                Glide.with(holder.itemView.context)
                    .load(item.uri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .into(holder.view)
            }
        }
    }

    private val differ = AsyncListDiffer(
        this,
        object : DiffUtil.ItemCallback<ComposeActivity.QueuedMedia>() {
            override fun areItemsTheSame(oldItem: ComposeActivity.QueuedMedia, newItem: ComposeActivity.QueuedMedia): Boolean {
                return oldItem.localId == newItem.localId
            }

            override fun areContentsTheSame(oldItem: ComposeActivity.QueuedMedia, newItem: ComposeActivity.QueuedMedia): Boolean {
                return oldItem == newItem
            }
        }
    )

    inner class TextViewHolder(val view: ProgressTextView) :
        RecyclerView.ViewHolder(view) {
        init {
            val layoutParams = ConstraintLayout.LayoutParams(thumbnailViewSize, thumbnailViewSize)
            val margin = itemView.context.resources
                .getDimensionPixelSize(R.dimen.compose_media_preview_margin)
            val marginBottom = itemView.context.resources
                .getDimensionPixelSize(R.dimen.compose_media_preview_margin_bottom)
            layoutParams.setMargins(margin, 0, margin, marginBottom)
            view.layoutParams = layoutParams
            view.gravity = Gravity.CENTER
            view.setHorizontallyScrolling(true)
            view.ellipsize = TextUtils.TruncateAt.MARQUEE
            view.marqueeRepeatLimit = -1
            view.setSingleLine()
            view.setSelected(true)
            view.textSize = 16.0f
            view.setOnClickListener {
                onMediaClick(adapterPosition, view)
            }
        }
    }

    inner class PreviewViewHolder(val view: ProgressImageView) :
        RecyclerView.ViewHolder(view) {
        init {
            val layoutParams = ConstraintLayout.LayoutParams(thumbnailViewSize, thumbnailViewSize)
            val margin = itemView.context.resources
                .getDimensionPixelSize(R.dimen.compose_media_preview_margin)
            val marginBottom = itemView.context.resources
                .getDimensionPixelSize(R.dimen.compose_media_preview_margin_bottom)
            layoutParams.setMargins(margin, 0, margin, marginBottom)
            view.layoutParams = layoutParams
            view.scaleType = ImageView.ScaleType.CENTER_CROP
            view.setOnClickListener {
                onMediaClick(adapterPosition, view)
            }
        }
    }
}
