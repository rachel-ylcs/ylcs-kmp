plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

template(object : KotlinNativeLibTemplate() {
    override val windowsTarget: Boolean = true

    override fun KotlinNativeSourceSetsScope.source() {
        windowsMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.core,
            )
        }
    }
})