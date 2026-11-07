plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
}

group = "love.yinlin.native"
version = libs.versions.rachel.get()

dependencies {
    implementation(libs.gradle.plugin.dokka)
}

kotlin {
    jvmToolchain(25)
}

gradlePlugin {
    plugins {
        create("nativeDokka") {
            id = "love.yinlin.native-dokka"
            implementationClass = "love.yinlin.dokka.NativeDokkaPlugin"
            displayName = "Rachel Native API documentation"
        }
    }
}
