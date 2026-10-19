import org.jetbrains.compose.desktop.application.dsl.WindowsPlatformSettings
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val iosTarget: Boolean = false

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.resources,
                projects.ylcsModule.foundation.fts,
                projects.ylcsModule.compose.ui,
                projects.ylcsModule.compose.components.blur,
                projects.ylcsModule.compose.components.calendar,
                projects.ylcsModule.compose.components.dataTable,
                projects.ylcsModule.compose.components.hexview,
                projects.ylcsModule.compose.components.logOutput,
                projects.ylcsModule.compose.components.reordering,
                projects.ylcsModule.compose.components.richText,
            )
        }

        desktopMain.configure(commonMain)

        wasmJsMain.configure(commonMain)
    }

    override val desktopPackage = object : DesktopPackage(), DesktopPackage.Windows {
        override val packageName: String get() = uniqueSafeName
        override val mainClass: String = C.app.mainClass
        override fun onSettings(settings: WindowsPlatformSettings) { }
    }

    override fun KotlinWebpackConfig.webpack() {
        outputFileName = "$uniqueSafeModuleName.js"

        devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
            port = C.host.webServerPort
            client?.overlay = false
        }
    }

    override fun Project.actions() {
        tasks.register("galleryRun") {
            description = "运行Debug桌面版Gallery"
            dependsOn(tasks.named("run"))
        }

        tasks.register("galleryPublish") {
            description = "发布网页版Gallery"
            dependsOn(tasks.named("wasmJsBrowserDistribution"))

            doLast {
                copy {
                    from(C.root.app.gallery.originOutput)
                    into(C.root.app.gallery.output)
                }
            }
        }
    }
})
