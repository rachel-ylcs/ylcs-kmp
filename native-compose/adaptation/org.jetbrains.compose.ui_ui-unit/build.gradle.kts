plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
}

kotlin.sourceSets {
    commonMain.dependencies {
        api(libs.androidx.annotation)
        api(project(":ui-geometry"))
        implementation(libs.androidx.collection)
        implementation(libs.compose.runtime)
        implementation(project(":ui-util"))
    }
    getByName("nativeMain").dependencies {
        implementation(libs.kotlinx.atomicfu)
        implementation(libs.compose.annotation.internal)
        implementation(libs.compose.collection.internal)
    }
}
