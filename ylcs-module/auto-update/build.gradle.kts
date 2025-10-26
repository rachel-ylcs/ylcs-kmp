plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    C.useCompilerFeatures(this)
    C.jvmTarget(this)

    sourceSets {
        main.configure {
            useApi(
                projects.ylcsBase.core,
            )
        }
    }
}