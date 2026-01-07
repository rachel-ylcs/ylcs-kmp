plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.android)
    implementation(libs.gradle.plugin.compose)
    implementation(libs.gradle.plugin.composeCompiler)
    implementation(libs.gradle.plugin.serialization)
}

kotlin {
    jvmToolchain(25)
}