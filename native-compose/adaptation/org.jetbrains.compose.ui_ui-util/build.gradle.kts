plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
}

kotlin.sourceSets {
    commonMain.dependencies {
        implementation(libs.androidx.collection)
        implementation(libs.compose.runtime)
    }
    getByName("nativeMain").dependencies {
        implementation(libs.compose.collection.internal)
    }
}
