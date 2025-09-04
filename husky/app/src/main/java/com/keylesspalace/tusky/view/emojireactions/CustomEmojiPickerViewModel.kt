package com.keylesspalace.tusky.view.emojireactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keylesspalace.tusky.core.extensions.empty
import com.keylesspalace.tusky.entity.Emoji
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class)
class CustomEmojiPickerViewModel : ViewModel() {

    private val allEmojis = MutableStateFlow<List<Emoji>>(emptyList())
    private val query = MutableStateFlow(String.empty())
    val emojis: StateFlow<List<Emoji>> =
        combine(
            allEmojis,
            query.debounce(100).distinctUntilChanged()
        ) { emojiList, emojiQuery ->
            if (emojiQuery.isBlank()) {
                emojiList
            }
            else {
                emojiList.filter { it.shortcode.contains(emojiQuery, ignoreCase = true) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun setEmojis(list: List<Emoji>) {
        allEmojis.value = list
    }

    fun updateQuery(newQuery: String) {
        query.value = newQuery
    }
}
