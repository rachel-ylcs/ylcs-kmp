plugins {
    install(
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
    )
}

// https://github.com/Kotlin/dokka/issues/2956
tasks.matching { it.name.contains("javaDocReleaseGeneration", ignoreCase = true) }.configureEach { enabled = false }

template(object : KotlinAndroidNDKTemplate() {})