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
    override val linuxTarget: Boolean = true
    override val macosTarget: Boolean = true

    override fun KotlinNativeSourceSetsScope.source() {
        nativeMain.configure(commonMain) {
            lib(
                libs.rethis,
                projects.ylcsModule.cs.serverEngine,
            )
        }

        windowsMain.configure(nativeMain)
        linuxMain.configure(nativeMain)
        macosMain.configure(nativeMain)
    }
})