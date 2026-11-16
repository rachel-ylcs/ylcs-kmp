plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
}

group = "love.yinlin.native"
version = libs.versions.rachel.get()

dependencies {
    compileOnly(libs.gradle.plugin.kotlin.compiler.embeddable)
    compileOnly(libs.gradle.plugin.kotlin.api)
}

kotlin {
    jvmToolchain(25)
}

gradlePlugin {
    plugins {
        create("nativeVendorDiagnostics") {
            id = "love.yinlin.native-vendor-diagnostics"
            implementationClass = "love.yinlin.vendor.NativeVendorDiagnosticsPlugin"
        }
    }
}
