package com.keylesspalace.tusky.testingclasses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.keylesspalace.tusky.databinding.LayoutEmojiDialog2Binding
import com.keylesspalace.tusky.view.EmojiKeyboard

class UnicodeEmojiPickerPage : Fragment() {

    private lateinit var binding: LayoutEmojiDialog2Binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = LayoutEmojiDialog2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dialogEmojiKeyboard.setupKeyboard(
            "POST_ID", EmojiKeyboard.UNICODE_MODE
        ) { id, emoji -> }
    }
}
