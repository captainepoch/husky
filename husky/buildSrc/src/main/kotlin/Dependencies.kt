import java.util.Locale
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

const val kotlinVersion = "1.5.31"

object AndroidSDK {
    const val compileSdk = 30
    const val buildTools = "30.0.3"
}

object CustomHuskyBuild {
    const val applicationName = "Husky"
    const val customLogo = ""
    const val customInstance = ""
    const val supportAccountUrl = "https://huskyapp.dev/users/husky"
}

object DefaultConfig {
    const val applicationID = "su.xash.husky"
    const val minSdk = 23
    const val targetSdk = 30
    const val versionCodeRel = 169
    const val versionNameRel = "1.0.2"

    val javaVersion = JavaVersion.VERSION_1_8
    val jvmTarget = javaVersion.toString()

    const val instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    const val encoding = "UTF-8"
}

object BetaConfig {
    const val betaSufix = "beta"
    const val betaSufixVersion = "1"
    const val versionCodeBeta = 1
    const val versionNameBeta = "1.0.2"
}

object BuildTypes {
    const val debug = "debug"
    const val release = "release"
}

object Flavors {
    object Dimensions {
        const val husky = "husky"
        const val release = "release"
    }

    const val husky = "husky"
    const val beta = "beta"
    const val stable = "stable"
}

object ProguardFile {
    const val defaultFile = "proguard-android-optimize.txt"
    const val defaultRules = "proguard-rules.pro"
}

object BuildTasks {
    const val taskTypeClean = "clean"
}

object GradlePlugins {
    object Versions {
        const val gradle = "7.0.2"
        const val gradleVersions = "0.38.0"
    }

    const val android = "com.android.tools.build:gradle:${Versions.gradle}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"
    const val gradleVersions =
        "com.github.ben-manes:gradle-versions-plugin:${Versions.gradleVersions}"
}

// Function to add repositories to the project.
fun addRepos(handler: RepositoryHandler) {
    handler.google()
    handler.maven(url = "https://jitpack.io")
    handler.gradlePluginPortal()
}

// Function to check stable versions
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("alpha", "beta", "final", "ga", "m", "release", "rc")
        .any { version.toLowerCase(Locale.ROOT).contains(it) }
    return stableKeyword.not()
}
