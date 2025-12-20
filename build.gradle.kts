plugins {
    install(
        // 这里的插件在 buildSrc 中已经注册, 在主项目模块中通过 id 引用, 而不是 alias
        listOf(
            libs.plugins.kotlinMultiplatform,
            libs.plugins.composeMultiplatform,
            libs.plugins.composeCompiler,
            libs.plugins.kotlinJvm,
            libs.plugins.kotlinAndroid,
            libs.plugins.androidApplication,
            libs.plugins.androidLibrary,
            libs.plugins.androidLibraryNew,
            libs.plugins.kotlinCocoapods,
            libs.plugins.kotlinSerialization,
        ),
        listOf(
            libs.plugins.ktor,
        )
    )
}

showtime()