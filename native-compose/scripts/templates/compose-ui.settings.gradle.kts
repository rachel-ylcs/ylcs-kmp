pluginManagement {
    includeBuild("../../../../../build-logic")
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
    versionCatalogs {
        create("libs") { from(files("../../../../../libs.versions.toml")) }
    }
}

rootProject.name = "native-compose-ui"
val repositoryDirectory = settingsDir.resolve("../../../../..").canonicalFile

__PROJECTS__
