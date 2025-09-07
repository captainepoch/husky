package com.keylesspalace.tusky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keylesspalace.tusky.components.instance.domain.repository.InstanceRepository
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.viewmodel.viewstate.SplashViewState
import com.keylesspalace.tusky.viewmodel.viewstate.SplashViewState.NO_USER
import com.keylesspalace.tusky.viewmodel.viewstate.SplashViewState.USER
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel(
    val accountManager: AccountManager, private val instanceRepository: InstanceRepository
) : ViewModel() {

    private val _splashViewState: MutableStateFlow<SplashViewState> =
        MutableStateFlow(SplashViewState.DEFAULT)
    val splashViewState = _splashViewState

    init {
        loadInstanceSettings()
    }

    private fun loadInstanceSettings() {
        viewModelScope.launch {
            instanceRepository.getInstanceInfo()
                .catch {
                    _splashViewState.update { NO_USER }
                }.collect {
                _splashViewState.update {
                    accountManager.activeAccount?.let { USER } ?: NO_USER
                }
            }
        }
    }
}
