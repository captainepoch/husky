package com.keylesspalace.tusky.di

import com.keylesspalace.tusky.util.OmittedDomainAppModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class GlideModule {
    @ContributesAndroidInjector
    abstract fun provideOmittedDomainAppModule() : OmittedDomainAppModule

}