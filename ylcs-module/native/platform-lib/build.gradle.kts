plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinNativeLibTemplate() {
    override val libName: String = "platform_lib"
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