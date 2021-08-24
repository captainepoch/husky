package com.keylesspalace.tusky.util

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.HttpUrlFetcher
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.HttpGlideUrlLoader
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.signature.ObjectKey
import com.keylesspalace.tusky.TuskyApplication
import com.keylesspalace.tusky.db.AccountManager
import java.io.File
import java.io.InputStream
import javax.inject.Inject

@GlideModule
class OmittedDomainAppModule : AppGlideModule() {
    @Inject
    lateinit var accountManager : AccountManager

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        (context.applicationContext as TuskyApplication).androidInjector.inject(this)

        registry.append(String::class.java, InputStream::class.java, OmittedDomainLoaderFactory(accountManager))
    }
}

class OmittedDomainLoaderFactory(val accountManager: AccountManager) : ModelLoaderFactory<String, InputStream> {
    override fun teardown() = Unit

    override fun build(factory: MultiModelLoaderFactory): ModelLoader<String, InputStream> = OmittedDomainLoader(accountManager)
}

class OmittedDomainLoader(val accountManager: AccountManager) : ModelLoader<String, InputStream> {
    override fun buildLoadData(model: String, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>?
    {
        val trueUrl = if(accountManager.activeAccount != null)
            "https://" + accountManager.activeAccount!!.domain + model
        else model

        val timeout = options.get(HttpGlideUrlLoader.TIMEOUT) ?: 100

        return ModelLoader.LoadData(ObjectKey(model), HttpUrlFetcher(GlideUrl(trueUrl), timeout))
    }


    override fun handles(model: String): Boolean {
        val file = File(model)
        return !file.exists()
    }
}