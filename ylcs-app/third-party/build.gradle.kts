plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                projects.ylcsModule.foundation.network,

                libs.compose.runtime,
            )
        }

        desktopTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test,
            )
        }
    }
})
