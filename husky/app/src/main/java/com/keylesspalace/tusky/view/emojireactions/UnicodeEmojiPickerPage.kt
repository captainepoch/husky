package com.keylesspalace.tusky.view.emojireactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.keylesspalace.tusky.databinding.LayoutEmojiUnicodeBinding
import com.keylesspalace.tusky.view.EmojiKeyboard

class UnicodeEmojiPickerPage : Fragment() {

    private lateinit var binding: LayoutEmojiUnicodeBinding
    private var onEmojiClick: ((String) -> Unit)? = null

    companion object {
        private const val CUSTOM_EMOJI_KEYBOARD = "custom_emoji_keyboard"

        fun newInstance(
            onEmojiClick: (String) -> Unit
        ): UnicodeEmojiPickerPage {
            return UnicodeEmojiPickerPage().apply {
                this.onEmojiClick = onEmojiClick
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutEmojiUnicodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEmojiKeyboard()
    }

    private fun setupEmojiKeyboard() {
        binding.dialogEmojiKeyboard.setupKeyboard(
            CUSTOM_EMOJI_KEYBOARD,
            EmojiKeyboard.UNICODE_MODE
        ) { _, emoji ->
            onEmojiClick?.invoke(emoji)
        }
    }
}
