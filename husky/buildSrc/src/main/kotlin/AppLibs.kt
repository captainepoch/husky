object AppPlugins {
    const val androidApplication = "com.android.application"
    const val androidBase = "android"
    const val kapt = "kapt"
    const val kotlinExtensions = "kotlin-android-extensions"
    const val kotlinParcelize = "kotlin-parcelize"
    const val manesVersions = "com.github.ben-manes.versions"
}

object ApplicationLibs {
    private object Versions {
        const val androidImageCropper = "2.8.0"
        const val appcompat = "1.3.1"
        const val autodispose = "1.4.0"
        const val bigImageViewer = "1.7.0"
        const val browser = "1.3.0"
        const val cardView = "1.0.0"
        const val conscryptAndroid = "2.5.2"
        const val constraintlayout = "2.1.1"
        const val coreKtx = "1.3.2"
        const val dagger = "2.38.1"
        const val emoji = "1.1.0"
        const val exifInterface = "1.3.3"
        const val exoplayer = "2.16.0"
        const val filemojiCompat = "1.0.17"
        const val flexbox = "2.0.1"
        const val fragmentKtx = "1.2.5"
        const val fragmentviewbindingdelegateKt = "1.0.0"
        const val glide = "4.12.0"
        const val glideImage = "1.8.0"
        const val lifecycle = "2.3.1"
        const val markdownEdit = "1.0.0"
        const val materialDesign = "1.4.0"
        const val materialDrawer = "8.2.0"
        const val materialDrawerTypeface = "3.0.1.4.original-kotlin@aar"
        const val pagingRuntimeKtx = "2.1.2"
        const val preferenceKtx = "1.1.1"
        const val okhttpVersion = "4.9.2"
        const val recyclerView = "1.2.1"
        const val retrofit = "2.9.0"
        const val room = "2.2.5"
        const val rxAndroid = "2.1.1"
        const val rxJava = "2.2.21"
        const val rxKotlin = "2.4.0"
        const val shareTarget = "1.0.0"
        const val simplestack = "2.6.2"
        const val simplestackExt = "2.2.2"
        const val sparkButton = "4.1.0"
        const val swipeRefreshLayout = "1.1.0"
        const val timber = "5.0.1"
        const val viewpager2 = "1.0.0"
        const val workRuntime = "2.4.0"
    }

    object AndroidX {
        const val appCompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
        const val browser = "androidx.browser:browser:${Versions.browser}"
        const val cardView = "androidx.cardview:cardview:${Versions.cardView}"
        const val constraintLayout =
            "androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}"
        const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
        const val emoji = "androidx.emoji:emoji:${Versions.emoji}"
        const val emojiAppCompat = "androidx.emoji:emoji-appcompat:${Versions.emoji}"
        const val emojiBundled = "androidx.emoji:emoji-bundled:${Versions.emoji}"
        const val exifInterface = "androidx.exifinterface:exifinterface:${Versions.exifInterface}"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:${Versions.fragmentKtx}"
        const val pagingRuntimeKtx =
            "androidx.paging:paging-runtime-ktx:${Versions.pagingRuntimeKtx}"
        const val preferenceKtx = "androidx.preference:preference-ktx:${Versions.preferenceKtx}"
        const val recyclerView = "androidx.recyclerview:recyclerview:${Versions.recyclerView}"
        const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
        const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
        const val roomRxJava = "androidx.room:room-rxjava2:${Versions.room}"
        const val shareTarget = "androidx.sharetarget:sharetarget:${Versions.shareTarget}"
        const val swipeRefreshLayout =
            "androidx.swiperefreshlayout:swiperefreshlayout:${Versions.swipeRefreshLayout}"
        const val viewpager2 = "androidx.viewpager2:viewpager2:${Versions.viewpager2}"
        const val workRuntime = "androidx.work:work-runtime:${Versions.workRuntime}"

        object Lifecycle {
            const val commonJava = "androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycle}"
            const val liveDataKtx =
                "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}"
            const val reactiveStreamsKtx =
                "androidx.lifecycle:lifecycle-reactivestreams-ktx:${Versions.lifecycle}"
            const val viewmodelKtx =
                "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
        }
    }

    object Dagger {
        const val dagger = "com.google.dagger:dagger:${Versions.dagger}"
        const val daggerAndroid = "com.google.dagger:dagger-android:${Versions.dagger}"
        const val daggerCompiler = "com.google.dagger:dagger-compiler:${Versions.dagger}"
        const val daggerProcessor = "com.google.dagger:dagger-android-processor:${Versions.dagger}"
        const val daggerSupport = "com.google.dagger:dagger-android-support:${Versions.dagger}"
    }

    object Glide {
        const val glide = "com.github.bumptech.glide:glide:${Versions.glide}"
        const val glideCompiler = "com.github.bumptech.glide:compiler:${Versions.glide}"
        const val glideOkhttp = "com.github.bumptech.glide:okhttp3-integration:${Versions.glide}"
    }

    object Google {
        const val flexbox = "com.google.android:flexbox:${Versions.flexbox}"
        const val exoplayer = "com.google.android.exoplayer:exoplayer:${Versions.exoplayer}"
        const val materialDesign = "com.google.android.material:material:${Versions.materialDesign}"
    }

    object Kotlin {
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion}"
        const val stdlibJdk = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}"
    }

    object RxJava {
        const val rxAndroid = "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroid}"
        const val rxJava = "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"
        const val rxKotlin = "io.reactivex.rxjava2:rxkotlin:${Versions.rxKotlin}"
    }

    object SimpleStack {
        const val lib = "com.github.Zhuinden:simple-stack:${Versions.simplestack}"
        const val ext = "com.github.Zhuinden:simple-stack-extensions:${Versions.simplestackExt}"
    }

    object Square {
        const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
        const val retrofitAdapterRxJ2 =
            "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
        const val retrofitConvGson = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"

        const val logginInterceptor =
            "com.squareup.okhttp3:logging-interceptor:${Versions.okhttpVersion}"
        const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttpVersion}"
        const val okhttpBrotli = "com.squareup.okhttp3:okhttp-brotli:${Versions.okhttpVersion}"
    }

    const val androidImageCropper =
        "com.theartofdev.edmodo:android-image-cropper:${Versions.androidImageCropper}"
    const val autodispose = "com.uber.autodispose:autodispose:${Versions.autodispose}"
    const val autodisposeAndroidArchComp =
        "com.uber.autodispose:autodispose-android-archcomponents:${Versions.autodispose}"
    const val bigImageViewer = "com.github.piasy:BigImageViewer:${Versions.bigImageViewer}"
    const val conscryptAndroid = "org.conscrypt:conscrypt-android:${Versions.conscryptAndroid}"
    const val filemojiCompat = "de.c1710:filemojicompat:${Versions.filemojiCompat}"
    const val fragmentviewbindingdelegateKt =
        "com.github.Zhuinden:fragmentviewbindingdelegate-kt:${Versions.fragmentviewbindingdelegateKt}"
    const val glideImage = "com.github.piasy:GlideImageLoader:${Versions.glideImage}"
    const val glideImageViewFactory =
        "com.github.piasy:GlideImageViewFactory:${Versions.glideImage}"
    const val markdownEdit = "com.github.Tunous:MarkdownEdit:${Versions.markdownEdit}"
    const val materialDrawer = "com.mikepenz:materialdrawer:${Versions.materialDrawer}"
    const val materialDrawerIconics =
        "com.mikepenz:materialdrawer-iconics:${Versions.materialDrawer}"
    const val materialDrawerTypeface =
        "com.mikepenz:google-material-typeface:${Versions.materialDrawerTypeface}"
    const val sparkButton = "com.github.connyduck:sparkbutton:${Versions.sparkButton}"
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
}

object GradlePlugins {
    object Versions {
        const val gradle = "7.0.3"
        const val gradleVersions = "0.39.0"
        const val spotless = "6.0.0"
    }

    const val android = "com.android.tools.build:gradle:${Versions.gradle}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"
    const val gradleVersions =
        "com.github.ben-manes:gradle-versions-plugin:${Versions.gradleVersions}"
}
