import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
	addRepos(repositories)

	dependencies {
		// Base
		classpath(GradlePlugins.android)
		classpath(GradlePlugins.kotlin)

		// Plugins
		classpath(GradlePlugins.gradleVersions)
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

	tasks.withType<DependencyUpdatesTask> {
		gradleReleaseChannel = "current"

		rejectVersionIf {
			!isNonStable(candidate.version)
		}
	}
}

tasks.register<Delete>(BuildTasks.taskTypeClean) {
	delete(rootProject.buildDir)
	delete(project.buildDir)
	delete(buildDir)
	delete("${projectDir}/buildSrc/build")
}
