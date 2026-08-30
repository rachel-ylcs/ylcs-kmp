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
                projects.ylcsModule.compose.ui,
                projects.ylcsModule.compose.components.richText,
                libs.sketch,
                libs.sketch.http,
            )
        }

        desktopMain.configure(commonMain)

        wasmJsMain.configure(commonMain)
    }

    override val desktopPackageName: String get() = uniqueSafeName
    override val desktopMainClass: String = C.app.mainClass
    override val windowsDistributions: (WindowsPlatformSettings.() -> Unit) = { }

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