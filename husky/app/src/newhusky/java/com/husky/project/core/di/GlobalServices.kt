package com.husky.project.core.di

import android.content.Context
import com.zhuinden.simplestack.GlobalServices

class HuskyServices(private val appContext: Context) {

    fun getGlobalServices(): GlobalServices {
        val builder = GlobalServices.builder()

        with(builder) {
        }

        return builder.build()
    }
}
