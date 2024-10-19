package com.keylesspalace.tusky.testingclasses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.keylesspalace.tusky.databinding.LayoutEmojiDialog2Binding

class Fragment2 : Fragment() {

    private lateinit var binding: LayoutEmojiDialog2Binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutEmojiDialog2Binding.inflate(inflater, container, false)
        return binding.root
    }
}
