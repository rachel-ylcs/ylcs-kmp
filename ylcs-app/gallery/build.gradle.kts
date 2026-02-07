import org.jetbrains.compose.desktop.application.dsl.WindowsPlatformSettings
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val iosTarget: Boolean = false

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.resources,
                projects.ylcsModule.compose.ui
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
        // 运行 Gallery 桌面版
        val galleryRun by tasks.registering {
            dependsOn(tasks.named("run"))
        }

        // 发布 Gallery 网页版
        val galleryPublish by tasks.registering {
            dependsOn(tasks.named("wasmJsBrowserDistribution"))

            doLast {
                copy {
                    from(C.root.gallery.originOutput)
                    into(C.root.gallery.output)
                }
            }
        }
    }
})