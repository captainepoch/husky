package com.keylesspalace.tusky.testingclasses

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.keylesspalace.tusky.components.instance.domain.repository.InstanceRepository
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityTestingBinding
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.network.MastodonApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class TestingActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityTestingBinding::inflate)
    private val accountManager by inject<AccountManager>()
    private val instanceRepository by inject<InstanceRepository>()
    private val mastoApi by inject<MastodonApi>()

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
        val a = mastoApi.reactWithEmoji(
            "AuRbqaAumbbwNuWwpU", emoji
        ).subscribe({ a ->
            Timber.d("${a.getEmojiReactions()}")
        }, { a ->
            Timber.d(a)
        })
    }
}
