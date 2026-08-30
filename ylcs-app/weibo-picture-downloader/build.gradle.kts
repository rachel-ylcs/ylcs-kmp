import love.yinlin.task.spec.zip

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinNativeExecutableTemplate() {
    override val windowsTarget: Boolean = true

    override fun KotlinNativeSourceSetsScope.source() {
        windowsMain.configure {
            lib(
                ExportLib,
                projects.ylcsModule.foundation.filesystem,
                projects.ylcsModule.foundation.network,
            )
        }
    }

    override fun Project.actions() {
        tasks.register("weiboPictureDownloaderRun") {
            description = "运行微博图片下载器Windows版"
            dependsOn(tasks.named("linkDebugExecutableWindows"))
        }

        tasks.register("weiboPictureDownloaderPublish") {
            description = "发布微博图片下载器Windows版"
            dependsOn(tasks.named("linkReleaseExecutableWindows"))

            doLast {
                val outputAppDir = C.root.outputs.dir("weiboPictureDownloader")
                delete(outputAppDir)
                copy {
                    from(C.root.app.weiboPictureDownloader.originOutput)
                    into(outputAppDir)
                }
                copy {
                    from(C.root.artifacts.tool.file("cwebp.exe"))
                    into(outputAppDir)
                }
                zip(outputAppDir, C.root.outputs.file("weiboPictureDownloader.zip"))
                delete(outputAppDir)
            }
        }
    }
})