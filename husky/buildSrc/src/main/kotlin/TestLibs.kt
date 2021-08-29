object TestLibs {

	private object Versions {
		const val espresso = "3.4.0"
		const val extJunit = "1.1.3"
		const val junit = "4.13.2"
		const val mockitoInline = "3.6.28"
		const val mockitoKotlin = "2.2.0"
		const val roboelectric = "4.4"
		const val roomTesting = "2.2.5"
	}

	const val extJunit = "androidx.test.ext:junit:${Versions.extJunit}"
	const val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
	const val junit = "junit:junit:${Versions.junit}"
	const val mockitoInline = "org.mockito:mockito-inline:${Versions.mockitoInline}"
	const val mockitoKotlin = "com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}"
	const val roboelectric = "org.robolectric:robolectric:${Versions.roboelectric}"
	const val roomTesting = "androidx.room:room-testing:${Versions.roomTesting}"
}
