package com.keylesspalace.tusky.refactor_features.splash.view.fragments

import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.core.ui.fragment.BaseFragment
import com.keylesspalace.tusky.databinding.FragmentSplashBinding
import com.keylesspalace.tusky.refactor_features.splash.view.viewmodel.SplashViewModel
import com.zhuinden.simplestackextensions.fragmentsktx.lookup


class SplashFragment : BaseFragment(R.layout.fragment_splash) {

    private val binding by viewBinding(FragmentSplashBinding::bind)
    private val viewModel: SplashViewModel by lazy { lookup() }

}
