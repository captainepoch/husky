package com.keylesspalace.tusky.fragment

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.keylesspalace.tusky.databinding.StatusReblogQuoteBinding
import com.keylesspalace.tusky.fragment.StatusReblogQuoteType.QUOTE
import com.keylesspalace.tusky.fragment.StatusReblogQuoteType.REBLOG

class StatusReblogQuoteDialog(
    context: Context
) : BottomSheetDialog(context) {

    private lateinit var binding: StatusReblogQuoteBinding

    var onStatusActionListener: (StatusReblogQuoteType) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = StatusReblogQuoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.statusReblog.setOnClickListener {
            onStatusActionListener(REBLOG)
            dismiss()
        }

        binding.statusQuote.setOnClickListener {
            onStatusActionListener(QUOTE)
            dismiss()
        }
    }
}

enum class StatusReblogQuoteType {
    REBLOG,
    QUOTE;
}
