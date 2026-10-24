import love.yinlin.repository.tip.showtime

plugins {
    idea
    install(
        libs.plugins.rachel,
        libs.plugins.nativeBridge,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.kotlinJvm,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidApplication,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
        apply = false
    )
}

idea.module {
    excludeDirs.add(file("native-compose"))
}

showtime()
