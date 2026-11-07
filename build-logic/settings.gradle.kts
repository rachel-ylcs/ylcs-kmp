pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
    }
}

rootProject.name = "rachel-build-logic"

val catalog = settingsDir.resolve("../libs.versions.toml")
val expectedGradle = Regex("(?m)^gradle\\s*=\\s*\"([^\"]+)\"").find(catalog.readText())?.groupValues?.get(1)
check(gradle.gradleVersion == expectedGradle) {
    "Use the repository Gradle Wrapper. The catalog requires Gradle $expectedGradle, running ${gradle.gradleVersion}."
}

include(
    "public-plugin",
    "repository-conventions",
    "native-bridge",
    "native-dokka",
    "native-publications"
)
