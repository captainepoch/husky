package com.keylesspalace.tusky.core.ui.fragment

abstract class BaseBackFragment(layoutRes: Int) : BaseFragment(layoutRes) {

    abstract fun onHandleBack(): Boolean
}
