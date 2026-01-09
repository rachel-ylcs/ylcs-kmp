plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
    )
}

template(object : KotlinNativeLibTemplate() {
    override val windowsTarget: Boolean = true

    override fun KotlinNativeSourceSetsScope.source() {
        windowsMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.core.base,
            )
        }

        windowsTest.configure {
            lib(
                libs.test,
                libs.kotlinx.coroutines.test,
            )
        }
    }
})