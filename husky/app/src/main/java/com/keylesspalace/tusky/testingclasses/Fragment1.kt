package com.keylesspalace.tusky.testingclasses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.keylesspalace.tusky.databinding.LayoutEmojiDialog1Binding

class Fragment1 : Fragment() {

    private lateinit var binding: LayoutEmojiDialog1Binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutEmojiDialog1Binding.inflate(inflater, container, false)
        return binding.root
    }
}
