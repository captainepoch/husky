package com.husky.project.features.login.view.viewmodel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.husky.project.core.extensions.cancelIfActive
import com.husky.project.core.ui.viewmodel.BaseViewModel
import com.husky.project.features.login.view.viewmodel.LoginViewModelKeys.Bundle
import com.keylesspalace.tusky.core.extensions.orEmpty
import com.zhuinden.statebundle.StateBundle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LoginViewModel : BaseViewModel() {

    private var ver: Job? = null

    private val _verifyUrl = MutableLiveData<Boolean>()
    val verifyUrl: LiveData<Boolean>
        get() = _verifyUrl

    override fun toBundle(): StateBundle = StateBundle().apply {
        putBoolean(
            Bundle.VERIFY_URL,
            _verifyUrl.value.orEmpty()
        )
    }

    override fun fromBundle(bundle: StateBundle?) {
        bundle?.run {
            _verifyUrl.value = getBoolean(Bundle.VERIFY_URL)
        }
    }

    fun verifyUrl(url: String) {
        ver?.cancelIfActive()
        ver = viewModelScope.launch {

        }
        _verifyUrl.value = Patterns.WEB_URL.matcher(url).matches()
    }
}
