package com.husky.project.features.login.view.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.husky.project.features.login.view.viewmodel.LoginViewModelKeys.Bundle
import com.keylesspalace.tusky.core.extensions.orEmpty
import com.husky.project.core.ui.viewmodel.ServicesViewModel
import com.zhuinden.statebundle.StateBundle

class LoginViewModel : ServicesViewModel() {

    private val mVerifyUrl = MutableLiveData<Boolean>()
    val verifyUrl: LiveData<Boolean>
        get() = mVerifyUrl

    override fun toBundle(): StateBundle = StateBundle().apply {
        putBoolean(
            Bundle.VERIFY_URL,
            mVerifyUrl.value.orEmpty()
        )
    }

    override fun fromBundle(bundle: StateBundle?) {
        bundle?.run {
            mVerifyUrl.value = getBoolean(Bundle.VERIFY_URL)
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
