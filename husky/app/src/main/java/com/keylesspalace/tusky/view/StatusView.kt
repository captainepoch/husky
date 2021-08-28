package com.keylesspalace.tusky.view

import android.view.*
import android.content.*
import android.util.*
import android.widget.*
import android.app.*
import android.text.*
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager

import com.keylesspalace.tusky.adapter.StatusViewHolder
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.interfaces.StatusActionListener
import com.keylesspalace.tusky.util.CardViewMode
import com.keylesspalace.tusky.util.ViewDataUtils
import com.keylesspalace.tusky.util.StatusDisplayOptions
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.settings.PrefKeys

import java.util.*;

class StatusView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private var viewHolder : StatusViewHolder
    private var statusDisplayOptions : StatusDisplayOptions
    init {
        View.inflate(context, R.layout.item_status, this)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        statusDisplayOptions = StatusDisplayOptions(
                animateAvatars = preferences.getBoolean("animateGifAvatars", false),
                mediaPreviewEnabled = true,
                useAbsoluteTime = preferences.getBoolean("absoluteTimeView", false),
                showBotOverlay = false,
                useBlurhash = preferences.getBoolean("useBlurhash", true),
                cardViewMode = CardViewMode.NONE,
                confirmReblogs = preferences.getBoolean("confirmReblogs", true),
                renderStatusAsMention = preferences.getBoolean(PrefKeys.RENDER_STATUS_AS_MENTION, true),
                hideStats = false
        )
        viewHolder = StatusViewHolder(this)
    }
    
    fun setupWithStatus(status: Status) {
        val concrete = ViewDataUtils.statusToViewData(status, false, false)
        viewHolder.setupWithStatus(concrete, DummyStatusActionListener(), statusDisplayOptions)
    }
    
    class DummyStatusActionListener: StatusActionListener {
        override fun onReply(position: Int) { }
        override fun onReblog(reblog: Boolean, position: Int) { }
        override fun onFavourite(favourite: Boolean, position: Int) { }
        override fun onBookmark(bookmark: Boolean, position: Int) { }
        override fun onMore(view: View, position: Int) { }
        override fun onViewMedia(position: Int, attachmentIndex: Int, view: View?) { }
        override fun onViewThread(position: Int) { }
        override fun onViewReplyTo(position: Int) { }
        override fun onOpenReblog(position: Int) { }
        override fun onExpandedChange(expanded: Boolean, position: Int) { }
        override fun onContentHiddenChange(isShowing: Boolean, position: Int) { }
        override fun onLoadMore(position: Int) { }
        override fun onContentCollapsedChange(isCollapsed: Boolean, position: Int) { }
        override fun onVoteInPoll(position: Int, choices: MutableList<Int>) { }
        override fun onViewAccount(id: String) { }
        override fun onViewTag(id: String) { }
        override fun onViewUrl(id: String) { }
    }
}
