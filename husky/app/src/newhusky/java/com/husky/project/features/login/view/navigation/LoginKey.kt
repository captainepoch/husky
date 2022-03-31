package com.husky.project.features.login.view.navigation

import androidx.fragment.app.Fragment
import com.husky.project.core.ui.navigation.BaseServiceKey
import com.husky.project.features.login.view.fragments.LoginFragment
import com.husky.project.features.login.view.viewmodel.LoginViewModel
import com.zhuinden.simplestack.ServiceBinder
import com.zhuinden.simplestackextensions.servicesktx.add
import kotlinx.android.parcel.Parcelize

@Parcelize
class LoginKey : BaseServiceKey() {

    override fun instantiateFragment(): Fragment {
        return LoginFragment()
    }

    override fun bindServices(serviceBinder: ServiceBinder) {
        with(serviceBinder) {
            add(LoginViewModel())
        }
    }
}
