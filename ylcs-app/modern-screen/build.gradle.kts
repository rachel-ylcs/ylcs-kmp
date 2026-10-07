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
            lib(projects.ylcsModule.compose.screen)
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
        tasks.register("modernScreenRun") {
            description = "运行Debug桌面版ModernScreen"
            dependsOn(tasks.named("run"))
        }

        tasks.register("modernScreenPublish") {
            description = "发布网页版ModernScreen"
            dependsOn(tasks.named("wasmJsBrowserDistribution"))

            doLast {
                copy {
                    from(C.root.app.modernScreen.originOutput)
                    into(C.root.app.modernScreen.output)
                }
            }
        }
    }
})