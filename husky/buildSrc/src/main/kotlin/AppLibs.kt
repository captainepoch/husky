object ApplicationLibs {

	private object Versions {
		const val appcompat = "1.2.0"
		const val constraintlayout = "2.1.0"
		const val coreKtx = "1.3.2"
		const val dagger = "2.30.1"
		const val glide = "4.11.0"
		const val lifecycle = "2.2.0"
		const val materialDesign = "1.4.0"
		const val materialDrawer = "8.2.0"
		const val okhttpVersion = "4.9.0"
		const val retrofit = "2.9.0"
		const val room = "2.2.5"
		const val simplestack = "2.6.2"
		const val simplestackExt = "2.2.2"
		const val timber = "4.7.1"
	}

	object AndroidX {
		const val appCompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
		const val browser = "androidx.browser:browser:1.3.0"
		const val cardView = "androidx.cardview:cardview:1.0.0"
		const val constraintLayout =
			"androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}"
		const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
		const val exifInterface = "androidx.exifinterface:exifinterface:1.3.2"
		const val emoji = "androidx.emoji:emoji:1.1.0"
		const val emojiAppCompat = "androidx.emoji:emoji-appcompat:1.1.0"
		const val emojiBundled = "androidx.emoji:emoji-bundled:1.1.0"
		const val fragmentKtx = "androidx.fragment:fragment-ktx:1.2.5"
		const val pagingRuntimeKtx = "androidx.paging:paging-runtime-ktx:2.1.2"
		const val preferenceKtx = "androidx.preference:preference-ktx:1.1.1"
		const val recyclerView = "androidx.recyclerview:recyclerview:1.1.0"
		const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
		const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
		const val roomRxJava = "androidx.room:room-rxjava2:${Versions.room}"
		const val shareTarget = "androidx.sharetarget:sharetarget:1.0.0"
		const val swipeRefreshLayout = "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"
		const val viewpager2 = "androidx.viewpager2:viewpager2:1.0.0"
		const val workRuntime = "androidx.work:work-runtime:2.4.0"

		object Lifecycle {
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
		const val flexBox = "com.google.android:flexbox:2.0.1"
		const val materialDesign = "com.google.android.material:material:${Versions.materialDesign}"
	}

	object Kotlin {
		const val reflect = "org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}"
		const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion}"
		const val stdlibJdk = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}"
	}

	object RxJava {
		const val rxAndroid = "io.reactivex.rxjava2:rxandroid:2.1.1"
		const val rxJava = "io.reactivex.rxjava2:rxjava:2.2.20"
		const val rxKotlin = "io.reactivex.rxjava2:rxkotlin:2.4.0"
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

	const val androidImageCropper = "com.theartofdev.edmodo:android-image-cropper:2.8.0"
	const val autodispose = "com.uber.autodispose:autodispose:1.4.0"
	const val autodisposeAndroidArchComp =
		"com.uber.autodispose:autodispose-android-archcomponents:1.4.0"
	const val bigImageViewer = "com.github.piasy:BigImageViewer:1.7.0"
	const val conscryptAndroid = "org.conscrypt:conscrypt-android:2.5.1"
	const val filemojiCompat = "de.c1710:filemojicompat:1.0.17"
	const val glideImage = "com.github.piasy:GlideImageLoader:1.8.0"
	const val glideImageViewFactory = "com.github.piasy:GlideImageViewFactory:1.8.0"
	const val markdownEdit = "com.github.Tunous:MarkdownEdit:1.0.0"
	const val materialDrawer = "com.mikepenz:materialdrawer:${Versions.materialDrawer}"
	const val materialDrawerIconics =
		"com.mikepenz:materialdrawer-iconics:${Versions.materialDrawer}"
	const val materialDrawerTypeface =
		"com.mikepenz:google-material-typeface:3.0.1.4.original-kotlin@aar"
	const val sparkButton = "com.github.connyduck:sparkbutton:4.1.0"
	const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
}
