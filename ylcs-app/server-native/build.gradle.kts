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

    override val args: List<String> = buildList {
        if ("serverPublish" !in currentTaskName) add("--cd=${C.root.work.server.asFile}")
    }

    override fun KotlinNativeSourceSetsScope.source() {
        nativeMain.configure(commonMain) {
            lib(
                libs.cryptography,
                libs.cryptography.provider,
                projects.ylcsApp.cs,
                projects.ylcsModule.cs.serverEngineNative,
                projects.ylcsModule.cs.serverServiceMysql,
                projects.ylcsModule.cs.serverServiceRedis,
            )
        }

        windowsMain.configure(nativeMain)
        linuxMain.configure(nativeMain)
        macosMain.configure(nativeMain)
    }
})