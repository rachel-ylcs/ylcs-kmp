pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("framework.repository").getOrElse("../../vendor/build/framework-repository")) }
        maven { url = uri(providers.gradleProperty("native.repository").getOrElse("../../vendor/build/repository")) }
        mavenCentral()
        google()
    }
    versionCatalogs { create("libs") { from(files("../../../libs.versions.toml")) } }
}

rootProject.name = "framework-consumer"
