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
        wasmJsMain.configure {
            lib(projects.ylcsApp.shared)
        }
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
        val wasmJsBrowserDevelopmentRun = tasks.named("wasmJsBrowserDevelopmentRun")
        val wasmJsBrowserDistribution = tasks.named("wasmJsBrowserDistribution")

        val webCopyDir by tasks.registering {
            mustRunAfter(wasmJsBrowserDistribution)
            doLast {
                copy {
                    from(C.root.webApp.originOutput)
                    into(C.root.webApp.output)
                }
                delete {
                    delete(*C.root.webApp.output.asFile.listFiles {
                        it.extension == "map" || it.extension == "txt"
                    }.map { C.root.webApp.output.file(it.name) }.toTypedArray())
                }
                zip {
                    from(C.root.webApp.output)
                    to(C.root.outputs.file("[Web]${C.app.displayName}${C.app.versionName}.zip"))
                }
                delete {
                    delete(C.root.webApp.output)
                }
            }
        }

        // 运行 Web 应用程序
        val webRun by tasks.registering {
            dependsOn(wasmJsBrowserDevelopmentRun)
        }

        // 发布 Web 应用程序
        val webPublish by tasks.registering {
            dependsOn(wasmJsBrowserDistribution)
            dependsOn(webCopyDir)
        }
    }
})