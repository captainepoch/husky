package com.keylesspalace.tusky.util

import android.content.Context
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
import com.keylesspalace.tusky.db.AccountManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.InputStream

@GlideModule
class OmittedDomainAppModule : AppGlideModule(), KoinComponent {

    private val accountManager: AccountManager by inject()

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(
            String::class.java,
            InputStream::class.java,
            OmittedDomainLoaderFactory(accountManager)
        )
    }
}

class OmittedDomainLoaderFactory(val accountManager: AccountManager) : ModelLoaderFactory<String, InputStream> {
    override fun teardown() = Unit

    override fun build(factory: MultiModelLoaderFactory): ModelLoader<String, InputStream> =
        OmittedDomainLoader(accountManager)
}

class OmittedDomainLoader(val accountManager: AccountManager) : ModelLoader<String, InputStream> {
    override fun buildLoadData(
        model: String,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> {
        val trueUrl = if (accountManager.activeAccount != null) {
            "https://" + accountManager.activeAccount!!.domain + model
        } else {
            model
        }

        val timeout = options.get(HttpGlideUrlLoader.TIMEOUT) ?: 100

        return ModelLoader.LoadData(ObjectKey(model), HttpUrlFetcher(GlideUrl(trueUrl), timeout))
    }

    override fun handles(model: String): Boolean {
        val file = File(model)
        return !file.exists()
    }
}
