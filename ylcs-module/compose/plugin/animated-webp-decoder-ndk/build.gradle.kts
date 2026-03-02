plugins {
    install(
        libs.plugins.kotlinAndroid,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinAndroidNDKTemplate() {})