plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

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
