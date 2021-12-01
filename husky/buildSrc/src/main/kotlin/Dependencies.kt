import java.util.Locale
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

const val kotlinVersion = "1.5.32"

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
    const val versionCodeRel = 170
    const val versionNameRel = "1.0.2"

    val javaVersion = JavaVersion.VERSION_11
    val jvmTarget = javaVersion.toString()

    const val instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    const val encoding = "UTF-8"
}

object BetaConfig {
    const val betaSuffix = "beta"
    const val betaSuffixVersion = "2"
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

// Function to add repositories to the project.
fun addRepos(handler: RepositoryHandler) {
    handler.google()
    handler.gradlePluginPortal()
    handler.maven(url = "https://jitpack.io")
    handler.maven(url = "https://plugins.gradle.org/m2/")
}

// Function to check stable versions
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("a", "alpha", "beta", "final", "ga", "m", "release", "rc")
        .any { version.toLowerCase(Locale.ROOT).contains(it) }
    return stableKeyword.not()
}
