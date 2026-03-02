plugins {
    install(
        libs.plugins.kotlinAndroid,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
    )
}

template(object : KotlinAndroidNDKTemplate() {})