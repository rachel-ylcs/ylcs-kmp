plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    C.useCompilerFeatures(this)
    C.jvmTarget(this)

    sourceSets {
        main.configure {
            useLib(
                libs.kotlinx.io,
                libs.kotlinx.json,
                libs.kotlinx.coroutines,
            )
        }
    }
}