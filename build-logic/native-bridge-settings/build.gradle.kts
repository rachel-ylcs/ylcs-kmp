plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "love.yinlin.native"
version = libs.versions.rachel.get()
kotlin { jvmToolchain(25) }

gradlePlugin {
    plugins {
        create("nativeBridgeSettings") {
            id = libs.plugins.nativeBridgeSettings.get().pluginId
            implementationClass = "love.yinlin.nativebridge.NativeBridgeSettingsPlugin"
            displayName = "Rachel Native Bridge Sources"
            description = "Connect local Native port builds without chained module substitutions"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "LocalVerification"
            url = uri(rootProject.layout.buildDirectory.dir("native-bridge-repository"))
        }
    }
}
