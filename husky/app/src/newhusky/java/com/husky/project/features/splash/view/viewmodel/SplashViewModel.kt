package com.husky.project.features.splash.view.viewmodel

import com.husky.project.core.ui.viewmodel.BaseViewModel
import com.zhuinden.statebundle.StateBundle

class SplashViewModel : BaseViewModel() {

    override fun toBundle(): StateBundle {
        val stateBundle = StateBundle()

        return stateBundle
    }

    override fun fromBundle(bundle: StateBundle?) {
        bundle?.let {}
    }
}