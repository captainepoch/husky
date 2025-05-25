package com.keylesspalace.tusky.testingclasses

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityTestingBinding
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.network.MastodonApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import timber.log.Timber.Forest

class TestingActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityTestingBinding::inflate)
    private val accountManager by inject<AccountManager>()
    private val mastoApi by inject<MastodonApi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        groupEmojis()

        binding.showEmojis.setOnClickListener {
            val dialog = EmojiDialogFragment(mastoApi.getCustomEmojis().blockingGet()) { emoji ->
                Timber.d("Emoji called: $emoji")
            }
            dialog.show(supportFragmentManager, "MyDialog")
        }
    }

    private fun groupEmojis() {
        lifecycleScope.launch(Dispatchers.IO) {

        }
    }
}
