package com.keylesspalace.tusky.refactor_features.splash.view.fragments

import android.os.Bundle
import android.view.View
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.core.ui.fragment.BaseFragment
import com.keylesspalace.tusky.databinding.FragmentSplashBinding
import com.keylesspalace.tusky.refactor_features.login.view.navigation.LoginKey
import com.keylesspalace.tusky.refactor_features.splash.view.viewmodel.SplashViewModel
import com.zhuinden.simplestack.History
import com.zhuinden.simplestack.StateChange
import com.zhuinden.simplestackextensions.fragmentsktx.backstack
import com.zhuinden.simplestackextensions.fragmentsktx.lookup

class SplashFragment : BaseFragment(R.layout.fragment_splash) {

    private val binding by viewBinding(FragmentSplashBinding::bind)
    private val viewModel by lazy { lookup<SplashViewModel>() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backstack.setHistory(History.single(LoginKey()), StateChange.FORWARD)
    }
}
