package com.keylesspalace.tusky.interfaces

import android.view.View
import com.keylesspalace.tusky.entity.Chat

interface ChatActionListener: LinkListener {
    fun onLoadMore(position: Int) {}
    fun onMore(chatId: String, v: View) {}
    fun openChat(position: Int) {}
    fun onViewMedia(position: Int, view: View?) {}
}