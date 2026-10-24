pluginManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("native.plugin.repository").getOrElse("../../../build-logic/build/native-bridge-repository")) }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri(providers.gradleProperty("native.repository").getOrElse("../../build/repository"))
        }
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../../../libs.versions.toml")) }
    }
}

rootProject.name = "native-maven-consumer"
