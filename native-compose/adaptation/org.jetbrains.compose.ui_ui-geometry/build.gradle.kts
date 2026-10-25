plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
}

kotlin.sourceSets.commonMain.dependencies {
    implementation(libs.compose.runtime)
    implementation(project(":ui-util"))
}
