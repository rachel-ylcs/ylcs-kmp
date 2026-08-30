import love.yinlin.task.spec.zip
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
    override val desktopTarget: Boolean = false

    override fun KotlinMultiplatformSourceSetsScope.source() {
        webMain.configure(commonMain) {
            lib(projects.ylcsApp.app.portal)
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
        tasks.register("webJsRun") {
            description = "运行Web/Js网页版"
            dependsOn(tasks.named("jsBrowserDevelopmentRun"))
        }

        tasks.register("webRun") {
            description = "运行Web/Wasm网页版"
            dependsOn(tasks.named("wasmJsBrowserDevelopmentRun"))
        }

        tasks.register("webArtifact") {
            description = "打包网页版"
            dependsOn(tasks.named("jsBrowserDistribution"))
            dependsOn(tasks.named("wasmJsBrowserDistribution"))

            doLast {
                val outputJsDir = C.root.app.webApp.jsOutput
                val outputWasmDir = C.root.app.webApp.wasmOutput
                copy {
                    from(C.root.app.webApp.originJsOutput)
                    into(outputJsDir)
                }
                delete(*outputJsDir.asFile.listFiles { it.extension == "map" || it.extension == "txt" })
                copy {
                    from(C.root.app.webApp.originWasmOutput)
                    into(outputWasmDir)
                }
                delete(*outputWasmDir.asFile.listFiles { it.extension == "map" || it.extension == "txt" })
                zip(outputJsDir, C.root.outputs.file("ylcs-js.zip"))
                zip(outputWasmDir, C.root.outputs.file("ylcs-wasm.zip"))
            }
        }

        tasks.register("webPublish") {
            description = "发布网页版"
            dependsOn(tasks.named("wasmJsBrowserDistribution"))

            doLast {
                val outputAppDir = C.root.app.webApp.wasmOutput
                copy {
                    from(C.root.app.webApp.originWasmOutput)
                    into(outputAppDir)
                }
                delete(*outputAppDir.asFile.listFiles { it.extension == "map" || it.extension == "txt" })
                zip(outputAppDir, C.root.outputs.file("[WebWasm]${C.app.displayName}${C.app.versionName}.zip"))
                delete(outputAppDir)
            }
        }
    }
})