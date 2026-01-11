import love.yinlin.task.spec.zip
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
    override val desktopTarget: Boolean = false

    override fun KotlinMultiplatformSourceSetsScope.source() {
        webMain.configure(commonMain) {
            lib(projects.ylcsApp.shared)
        }

        jsMain.configure(webMain)

        wasmJsMain.configure(webMain)
    }

    override fun KotlinWebpackConfig.webpack() {
        outputFileName = "${C.app.projectName}.js"

        devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
            port = C.host.webServerPort
            client?.overlay = false
            proxy = mutableListOf(KotlinWebpackConfig.DevServer.Proxy(
                context = mutableListOf(),
                target = C.host.apiUrl,
                changeOrigin = true,
                secure = false,
            ))
        }
    }

    override fun Project.actions() {
        // 运行 Web 应用程序
        val webRun by tasks.registering {
            dependsOn(tasks.named("wasmJsBrowserDevelopmentRun"))
        }

        // 发布 Web 应用程序
        val webPublish by tasks.registering {
            dependsOn(tasks.named("wasmJsBrowserDistribution"))

            doLast {
                copy {
                    from(C.root.webApp.originOutput)
                    into(C.root.webApp.output)
                }
                delete(*C.root.webApp.output.asFile.listFiles { it.extension == "map" || it.extension == "txt" })
                zip(C.root.webApp.output, C.root.outputs.file("[Web]${C.app.displayName}${C.app.versionName}.zip"))
                delete(C.root.webApp.output)
            }
        }
    }
})