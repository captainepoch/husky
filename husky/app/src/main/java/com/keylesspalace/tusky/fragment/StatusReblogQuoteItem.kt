package com.keylesspalace.tusky.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.databinding.StatusReblogQuoteItemBinding

class StatusReblogQuoteItem @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = StatusReblogQuoteItemBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private var buttonIcon: Drawable? = null
    private var buttonText: String? = null

    init {
        initComponent()
        setUp()
    }

    private fun initComponent() {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.StatusReblogQuoteItem)
            buttonIcon = typedArray.getDrawable(R.styleable.StatusReblogQuoteItem_icon)
            buttonText = typedArray.getString(R.styleable.StatusReblogQuoteItem_text)
            typedArray.recycle()
        }
    }

    private fun setUp() {
        binding.reblogIcon.setImageDrawable(buttonIcon)
        binding.reblogText.text = buttonText
    }
}
