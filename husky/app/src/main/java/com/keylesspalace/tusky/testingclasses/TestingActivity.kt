package com.keylesspalace.tusky.testingclasses

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityTestingBinding
import com.keylesspalace.tusky.db.AccountManager
import org.koin.android.ext.android.inject

class TestingActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityTestingBinding::inflate)
    private val accountManager by inject<AccountManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.showEmojis.setOnClickListener {
            val dialog = EmojiDialogFragment(accountManager.activeAccount?.emojis)
            dialog.show(supportFragmentManager, "MyDialog")
        }
    }
}
