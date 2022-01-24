package com.husky.project.core.ui.viewmodel

import com.zhuinden.simplestack.ScopedServices
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class ServicesViewModel : BaseViewModel(), ScopedServices.Registered {

    private val compositeDisposable = CompositeDisposable()

    override fun onServiceUnregistered() {
        compositeDisposable.clear()
    }

    fun addDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }
}
