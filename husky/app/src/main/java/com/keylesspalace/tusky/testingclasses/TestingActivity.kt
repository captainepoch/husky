package com.keylesspalace.tusky.testingclasses

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.keylesspalace.tusky.components.instance.domain.repository.InstanceRepository
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityTestingBinding
import com.keylesspalace.tusky.network.TimelineCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class TestingActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityTestingBinding::inflate)
    private val timelineCases by inject<TimelineCases>()
    private val instanceRepository by inject<InstanceRepository>()

    var reacted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.showEmojis.setOnClickListener {
            groupEmojis()
        }
    }

    private fun groupEmojis() {
        lifecycleScope.launch(Dispatchers.IO) {
            instanceRepository.getInstanceInfo().collect { instance ->
                val dialog = EmojiDialogFragment(instance.asRight().emojiList) { emoji ->
                    Timber.d("Emoji Called: $emoji")

                    react(emoji)
                }
                dialog.show(supportFragmentManager, "MyDialog")
            }
        }
    }

    @SuppressLint("AutoDispose")
    private fun react(emoji: String) {
        val a = timelineCases.react(emoji, "AuRbqaAumbbwNuWwpU", !reacted)
            .subscribe({ success -> reacted = !reacted}, { error -> })

        Timber.d("$reacted - $a")
    }
}
