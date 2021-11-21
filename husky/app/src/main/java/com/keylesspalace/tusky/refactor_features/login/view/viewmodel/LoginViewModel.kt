package com.keylesspalace.tusky.refactor_features.login.view.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.keylesspalace.tusky.core.extensions.orEmpty
import com.keylesspalace.tusky.core.ui.viewmodel.RegisteredViewModel
import com.zhuinden.statebundle.StateBundle

class LoginViewModel : RegisteredViewModel() {

    private val mVerifyUrl = MutableLiveData<Boolean>()
    val verifyUrl: LiveData<Boolean>
        get() = mVerifyUrl

    override fun toBundle(): StateBundle = StateBundle().apply {
        putBoolean(
            LoginViewModelKeys.Bundle.VERIFY_URL,
            mVerifyUrl.value.orEmpty()
        )
    }

    override fun fromBundle(bundle: StateBundle?) {
        bundle?.run {
            mVerifyUrl.value = getBoolean(LoginViewModelKeys.Bundle.VERIFY_URL)
        }
    }

    override fun onServiceRegistered() {
    }

    override fun onServiceUnregistered() {
        super.onServiceUnregistered()
    }

    fun verifyUrl(url: String) {
        mVerifyUrl.value = Patterns.WEB_URL.matcher(url).matches()
    }
}
