package com.keylesspalace.tusky.refactor_features.login.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.keylesspalace.tusky.core.ui.viewmodel.BaseViewModel
import com.zhuinden.statebundle.StateBundle
import timber.log.Timber

class LoginViewModel : BaseViewModel() {

    private val mVerifyUrl = MutableLiveData<Boolean>()
    val verifyUrl: LiveData<Boolean>
        get() = mVerifyUrl

    override fun toBundle(): StateBundle {
        val stateBundle = StateBundle()

        stateBundle.putBoolean(LoginViewModelKeys.Bundle.VERIFY_URL, mVerifyUrl.value ?: false)

        Timber.d("TO BUNDLE")

        return stateBundle
    }

    override fun fromBundle(bundle: StateBundle?) {
        bundle?.let {
            mVerifyUrl.value = it.getBoolean(LoginViewModelKeys.Bundle.VERIFY_URL)

            Timber.d("FROM BUNDLE")
        }
    }

    fun verifyUrl(url: String?) {
        mVerifyUrl.value = false
    }
}
