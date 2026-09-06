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
                libs.ktor.json,
                libs.ktor.server.cio,
                libs.ktor.server.negotiation,
                libs.ktor.server.websockets,
                ExportLib,
                libs.ktor.server,
                projects.ylcsModule.foundation.filesystem,
                projects.ylcsModule.cs.core,
            )
        }

        windowsMain.configure(nativeMain)
        linuxMain.configure(nativeMain)
        macosMain.configure(nativeMain)
    }
})