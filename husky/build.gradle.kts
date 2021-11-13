import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
	addRepos(repositories)

	dependencies {
		// Base
		classpath(GradlePlugins.android)
		classpath(GradlePlugins.kotlin)

		// Plugins
		classpath(GradlePlugins.gradleVersions)
		classpath(GradlePlugins.spotless)
	}
}

allprojects {
	addRepos(repositories)

	tasks.withType<JavaCompile> {
		options.encoding = DefaultConfig.encoding
		options.compilerArgs.addAll(
			listOf(
				"-Xlint:all",
				"-Xlint:unchecked",
				"-Xlint:-deprecation"
			)
		)
	}

	apply(plugin = AppPlugins.manesVersions)

	tasks.withType<DependencyUpdatesTask> {
		gradleReleaseChannel = "current"

		rejectVersionIf {
			!isNonStable(candidate.version)
		}
	}
}

tasks.register<Delete>(BuildTasks.taskTypeClean) {
	delete(buildDir)
	delete("${projectDir}/buildSrc/build")
}
