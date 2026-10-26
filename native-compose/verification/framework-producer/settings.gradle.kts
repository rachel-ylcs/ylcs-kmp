pluginManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("native.plugin.repository").getOrElse("../../../build-logic/build/native-bridge-repository")) }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("native.repository").getOrElse("../../vendor/build/repository")) }
        mavenCentral()
        google()
    }
    versionCatalogs { create("libs") { from(files("../../../libs.versions.toml")) } }
}

rootProject.name = "framework"
