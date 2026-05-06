plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.ktor,
    )
}

template(object : KotlinNativeExecutableTemplate() {
    override val windowsTarget: Boolean = true
    override val linuxTarget: Boolean = true
    override val macosTarget: Boolean = true

    override fun KotlinNativeSourceSetsScope.source() {
        nativeMain.configure(commonMain) {
            lib(projects.ylcsModule.foundation.filesystem)
        }

        windowsMain.configure(nativeMain)
        linuxMain.configure(nativeMain)
        macosMain.configure(nativeMain)
    }
})