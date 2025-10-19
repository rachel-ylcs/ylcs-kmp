plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    C.useCompilerFeatures(this)

    jvm("desktop") {
        C.jvmTarget(this)
    }

    sourceSets {
        val commonMain by getting {
            useApi(
                projects.ylcsCore.composeBase,
                libs.compose.navigation3,
                libs.compose.navigation.event,

                compose.desktop.currentOs,
            )
        }
    }
}