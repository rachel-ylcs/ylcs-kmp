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

apply(from = "../gradle/wrapper/check-version.settings.gradle.kts")

include(
    "public-plugin",
    "repository-conventions",
    "native-bridge",
    "native-dokka",
    "native-publications"
)
