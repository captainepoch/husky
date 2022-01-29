import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.project.starter.easylauncher.filter.ColorRibbonFilter

plugins {
    id(AppPlugins.androidApplication)

    kotlin(AppPlugins.androidBase)
    kotlin(AppPlugins.kapt)
    id(AppPlugins.kotlinExtensions)
    // id(AppPlugins.kotlinParcelize)

    id(AppPlugins.easylauncher) version (AppPlugins.Versions.easylauncher)
}

android {
    compileSdk = AndroidSDK.compileSdk
    buildToolsVersion = AndroidSDK.buildTools

    defaultConfig {
        applicationId = DefaultConfig.applicationID

        minSdk = DefaultConfig.minSdk
        targetSdk = DefaultConfig.targetSdk

        versionCode = DefaultConfig.versionCodeRel
        versionName = DefaultConfig.versionNameRel

        testInstrumentationRunner = DefaultConfig.instrumentationRunner

        vectorDrawables.useSupportLibrary = true

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.incremental", true)
            }
        }

        // TODO: remove, just for compiling
        buildConfigField(
            "String", "APPLICATION_NAME",
            "\"${CustomHuskyBuild.applicationName}\""
        )
        buildConfigField(
            "String", "CUSTOM_LOGO_URL",
            "\"${CustomHuskyBuild.customLogo}\""
        )
        buildConfigField(
            "String", "CUSTOM_INSTANCE",
            "\"${CustomHuskyBuild.customInstance}\""
        )
        buildConfigField(
            "String", "SUPPORT_ACCOUNT_URL",
            "\"${CustomHuskyBuild.supportAccountUrl}\""
        )
    }

    buildTypes {
        getByName(BuildTypes.debug) {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }

        getByName(BuildTypes.release) {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(ProguardFile.defaultFile),
                ProguardFile.defaultRules
            )
        }
    }

    flavorDimensions.addAll(
        listOf(
            Flavors.Dimensions.husky,
            Flavors.Dimensions.release
        )
    )
    productFlavors {
        create(Flavors.newhusky) {
            dimension = Flavors.Dimensions.release

            applicationId = DefaultConfig.NewHusky.applicationID

            minSdk = DefaultConfig.minSdk
            targetSdk = DefaultConfig.targetSdk

            versionCode = DefaultConfig.NewHusky.versionCode
            versionName = DefaultConfig.NewHusky.versionName
        }

        create(Flavors.husky) {
            dimension = Flavors.Dimensions.husky
        }

        create(Flavors.dev) {
            dimension = Flavors.Dimensions.release

            applicationId = DefaultConfig.Dev.applicationID

            minSdk = DefaultConfig.minSdk
            targetSdk = DefaultConfig.targetSdk

            versionCode = DefaultConfig.Dev.versionCode
            versionName = DefaultConfig.Dev.versionName
        }

        create(Flavors.beta) {
            dimension = Flavors.Dimensions.release

            versionNameSuffix = "-${BetaConfig.betaSuffix}${BetaConfig.betaSuffixVersion}"
        }

        create(Flavors.stable) {
            dimension = Flavors.Dimensions.release
        }
    }

    applicationVariants.all {
        outputs.forEach { output ->
            if(output is BaseVariantOutputImpl) {
                output.outputFileName =
                    "husky_${versionName}.${output.outputFile.extension}"
            }
        }
    }

    lint {
        // isAbortOnError = true
        disable("MissingTranslation")
        disable("ExtraTranslation")
        disable("AppCompatCustomView")
        disable("UseRequireInsteadOfGet")
    }

    compileOptions {
        sourceCompatibility = DefaultConfig.javaVersion
        targetCompatibility = DefaultConfig.javaVersion
    }

    kotlinOptions {
        jvmTarget = DefaultConfig.jvmTarget
    }

    buildFeatures {
        viewBinding = true
    }

    // TODO: remove this, only for compiling
    androidExtensions {
        isExperimental = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }

    packagingOptions {
        resources.excludes.addAll(
            listOf(
                "LICENSE_OFL",
                "LICENSE_UNICODE"
            )
        )
    }

    bundle {
        language {
            enableSplit = true
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(ApplicationLibs.AndroidX.appCompat)
    implementation(ApplicationLibs.AndroidX.browser)
    implementation(ApplicationLibs.AndroidX.cardView)
    implementation(ApplicationLibs.AndroidX.constraintLayout)
    implementation(ApplicationLibs.AndroidX.coreKtx)
    implementation(ApplicationLibs.AndroidX.emoji)
    implementation(ApplicationLibs.AndroidX.emojiAppCompat)
    implementation(ApplicationLibs.AndroidX.emojiBundled)
    implementation(ApplicationLibs.AndroidX.exifInterface)
    implementation(ApplicationLibs.AndroidX.fragmentKtx)
    implementation(ApplicationLibs.AndroidX.pagingRuntimeKtx)
    implementation(ApplicationLibs.AndroidX.preferenceKtx)
    implementation(ApplicationLibs.AndroidX.recyclerView)
    kapt(ApplicationLibs.AndroidX.roomCompiler)
    implementation(ApplicationLibs.AndroidX.roomRuntime)
    implementation(ApplicationLibs.AndroidX.roomRxJava)
    implementation(ApplicationLibs.AndroidX.shareTarget)
    implementation(ApplicationLibs.AndroidX.swipeRefreshLayout)
    implementation(ApplicationLibs.AndroidX.viewpager2)
    implementation(ApplicationLibs.AndroidX.workRuntime)
    implementation(ApplicationLibs.AndroidX.Lifecycle.commonJava)
    implementation(ApplicationLibs.AndroidX.Lifecycle.liveDataKtx)
    implementation(ApplicationLibs.AndroidX.Lifecycle.reactiveStreamsKtx)
    implementation(ApplicationLibs.AndroidX.Lifecycle.viewmodelKtx)

    implementation(ApplicationLibs.Dagger.dagger)
    implementation(ApplicationLibs.Dagger.daggerAndroid)
    kapt(ApplicationLibs.Dagger.daggerCompiler)
    kapt(ApplicationLibs.Dagger.daggerProcessor)
    implementation(ApplicationLibs.Dagger.daggerSupport)

    implementation(ApplicationLibs.Glide.glide)
    kapt(ApplicationLibs.Glide.glideCompiler)
    implementation(ApplicationLibs.Glide.glideImage)
    implementation(ApplicationLibs.Glide.glideImageViewFactory)
    implementation(ApplicationLibs.Glide.glideOkhttp)

    implementation(ApplicationLibs.Google.flexbox)
    implementation(ApplicationLibs.Google.exoplayer)
    implementation(ApplicationLibs.Google.materialDesign)

    implementation(ApplicationLibs.Kotlin.stdlib)
    implementation(ApplicationLibs.Kotlin.stdlibJdk)

    implementation(ApplicationLibs.RxJava.rxAndroid)
    implementation(ApplicationLibs.RxJava.rxJava)
    implementation(ApplicationLibs.RxJava.rxKotlin)

    implementation(ApplicationLibs.SimpleStack.ext)
    implementation(ApplicationLibs.SimpleStack.lib)

    implementation(ApplicationLibs.Square.retrofit)
    implementation(ApplicationLibs.Square.retrofitAdapterRxJ2)
    implementation(ApplicationLibs.Square.retrofitConvGson)
    implementation(ApplicationLibs.Square.logginInterceptor)
    implementation(ApplicationLibs.Square.okhttp)
    implementation(ApplicationLibs.Square.okhttpBrotli)

    implementation(ApplicationLibs.acraMail)
    implementation(ApplicationLibs.acraNotification)
    implementation(ApplicationLibs.androidImageCropper)
    implementation(ApplicationLibs.androidSvg)
    implementation(ApplicationLibs.autodispose)
    implementation(ApplicationLibs.autodisposeAndroidArchComp)
    implementation(ApplicationLibs.bigImageViewer)
    implementation(ApplicationLibs.conscryptAndroid)
    implementation(ApplicationLibs.filemojiCompat)
    implementation(ApplicationLibs.fragmentviewbindingdelegateKt)
    implementation(ApplicationLibs.markdownEdit)
    implementation(ApplicationLibs.materialDrawer)
    implementation(ApplicationLibs.materialDrawerIconics)
    implementation(ApplicationLibs.materialDrawerTypeface)
    implementation(ApplicationLibs.sparkButton)
    implementation(ApplicationLibs.timber)

    testImplementation(TestLibs.extJunit)
    testImplementation(TestLibs.junit)
    testImplementation(TestLibs.mockitoInline)
    testImplementation(TestLibs.mockitoKotlin)
    testImplementation(TestLibs.roboelectric)

    androidTestImplementation(TestLibs.espresso)
    androidTestImplementation(TestLibs.junit)
    androidTestImplementation(TestLibs.roomTesting)
}

easylauncher {
    productFlavors {
        register(Flavors.beta) {
            filters(
                customRibbon(
                    label = "Beta",
                    gravity = ColorRibbonFilter.Gravity.TOPRIGHT,
                    ribbonColor = "#DCDCDC",
                    labelColor = "#000000"
                )
            )
        }

        register(Flavors.newhusky) {
            filters(
                customRibbon(
                    label = "NEW",
                    gravity = ColorRibbonFilter.Gravity.TOPRIGHT,
                    ribbonColor = "#DCDCDC",
                    labelColor = "#000000"
                )
            )
        }
    }
}
