plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    C.useCompilerFeatures(this)
    C.jvmTarget(this)

    sourceSets {
        main.configure {
            useLib(
                libs.kotlinx.coroutines
            )
        }
    }
}