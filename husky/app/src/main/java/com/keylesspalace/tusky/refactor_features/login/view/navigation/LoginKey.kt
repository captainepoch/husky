package com.keylesspalace.tusky.refactor_features.login.view.navigation

import androidx.fragment.app.Fragment
import com.keylesspalace.tusky.core.ui.navigation.BaseServiceKey
import com.keylesspalace.tusky.refactor_features.login.view.fragments.LoginFragment
import com.keylesspalace.tusky.refactor_features.login.view.viewmodel.LoginViewModel
import com.zhuinden.simplestack.ServiceBinder
import com.zhuinden.simplestackextensions.servicesktx.add
import kotlinx.android.parcel.Parcelize

@Parcelize
class LoginKey : BaseServiceKey() {

    override fun instantiateFragment(): Fragment {
        return LoginFragment()
    }

    override fun bindServices(serviceBinder: ServiceBinder) {
        serviceBinder.add(LoginViewModel())
    }
}
