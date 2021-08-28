package com.keylesspalace.tusky.adapter

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.entity.StickerPack
import com.keylesspalace.tusky.view.EmojiKeyboard
import com.keylesspalace.tusky.view.EmojiKeyboard.EmojiKeyboardAdapter
import java.util.*

class StickerAdapter(
        private val stickerPacks: Array<StickerPack>,
        private val listener: EmojiKeyboard.OnEmojiSelectedListener
    ) : RecyclerView.Adapter<SingleViewHolder>(), TabConfigurationStrategy, EmojiKeyboardAdapter {

    private val recentsAdapter = StickerPageAdapter(null, listener, emptyList())
    // this value doesn't reflect actual button width but how much we want for button to take space
    // this is bad, only villains do that
    private val BUTTON_WIDTH_DP = 90.0f

    override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
        if (position == 0) {
            tab.setIcon(R.drawable.ic_access_time)
            return
        }

        val pack = stickerPacks[position - 1]
        val imageView = ImageView(tab.view.context)
        imageView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT)
        Glide.with(imageView)
            .asDrawable()
            .load(pack.internal_url + pack.tabIcon)
            .thumbnail()
            .centerCrop()
            .into( object: CustomTarget<Drawable>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    // tab.icon = resource
                    imageView.setImageDrawable(resource)
                    tab.customView = imageView
                }
            })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emoji_keyboard_page, parent, false)
        val holder = SingleViewHolder(view)

        val dm = parent.context.resources.displayMetrics
        val wdp = dm.widthPixels / dm.density
        val rows = (wdp / BUTTON_WIDTH_DP + 0.5).toInt()

        (view as RecyclerView).layoutManager = GridLayoutManager(view.getContext(), rows)
        return holder
    }

    override fun getItemCount(): Int {
        return stickerPacks.size + 1
    }

    override fun onRecentsUpdate(set: MutableSet<String>) {
        val list = set.toMutableList()
        list.reverse()
        recentsAdapter.stickers = list
        recentsAdapter.notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: SingleViewHolder, position: Int) {
        if( position == 0 ) {
            (holder.itemView as RecyclerView).adapter = recentsAdapter
        } else {
            val pack = stickerPacks[position - 1]
            (holder.itemView as RecyclerView).adapter = StickerPageAdapter(pack.internal_url, listener, pack.stickers)
        }
    }

    private class StickerPageAdapter(
        private val url: String?,
        var listener: EmojiKeyboard.OnEmojiSelectedListener,
        var stickers: List<String>
    ) : RecyclerView.Adapter<SingleViewHolder>() {
        override fun getItemCount(): Int {
            return stickers.size
        }

        override fun onBindViewHolder(holder: SingleViewHolder, position: Int) {
            (holder.itemView as AppCompatImageButton).setOnClickListener {
                listener.onEmojiSelected("", ( url ?: "" ) + stickers[position])
            }
            Glide.with(holder.itemView)
                .load(( url ?: "" ) + stickers[position])
                .thumbnail()
                .into(holder.itemView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_emoji_keyboard_sticker, parent, false)
            return SingleViewHolder(view)
        }
    }
}