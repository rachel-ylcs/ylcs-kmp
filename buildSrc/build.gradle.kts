plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.android)
    implementation(libs.gradle.plugin.compose)
    implementation(libs.gradle.plugin.composeCompiler)
    implementation(libs.gradle.plugin.serialization)
    implementation(libs.gradle.plugin.mavenPublish)
    implementation(libs.gradle.plugin.dokka)
}

kotlin {
    jvmToolchain(25)
}