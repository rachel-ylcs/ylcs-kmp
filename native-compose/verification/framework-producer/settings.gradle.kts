pluginManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("native.plugin.repository").getOrElse("../../../build-logic/build/native-bridge-repository")) }
        mavenCentral()
        gradlePluginPortal()
    }
}

apply(from = "../../gradle/verification.settings.gradle.kts")
val generation = gradle.extensions.extraProperties.get("love.yinlin.nativeCompose.verificationGeneration") as File

dependencyResolutionManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("native.repository").getOrElse(generation.resolve("build/repository").path)) }
        mavenCentral()
        google()
    }
    versionCatalogs { create("libs") { from(files("../../../libs.versions.toml")) } }
}

rootProject.name = "framework"
