package com.husky.project.features.splash.view.navigation

import androidx.fragment.app.Fragment
import com.husky.project.core.ui.navigation.BaseServiceKey
import com.husky.project.features.splash.view.fragments.SplashFragment
import com.husky.project.features.splash.view.viewmodel.SplashViewModel
import com.zhuinden.simplestack.ServiceBinder
import com.zhuinden.simplestackextensions.servicesktx.add
import kotlinx.android.parcel.Parcelize

@Parcelize
class SplashKey : BaseServiceKey() {

    override fun instantiateFragment(): Fragment {
        return SplashFragment()
    }

    override fun bindServices(serviceBinder: ServiceBinder) {
        with(serviceBinder) {
            add(SplashViewModel())
        }
    }
}
