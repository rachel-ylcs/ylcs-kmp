import love.yinlin.gradle.jsBinaryDir
import love.yinlin.repository.C
import love.yinlin.gradle.task.spec.zip
import love.yinlin.gradle.wasmBinaryDir
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
    )
}

rachel {
    multiplatform {
        iosTarget = false
        desktopTarget = false

        sources {
            webMain(commonMain) {
                lib(projects.ylcsApp.app.portal)
            }

            jsMain(webMain)

            wasmJsMain(webMain)
        }

        webpack {
            outputFileName = "${C.app.projectName}.js"

            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                port = C.web.webServerPort
                client?.overlay = false
                proxy = mutableListOf(KotlinWebpackConfig.DevServer.Proxy(
                    context = mutableListOf(),
                    target = C.cs.apiUrl,
                    changeOrigin = true,
                    secure = false,
                ))
            }
        }

        actions {
            tasks.register("webJsRun") {
                description = "运行Web/Js网页版"
                dependsOn(tasks.named("jsBrowserDevelopmentRun"))
            }

            tasks.register("webRun") {
                description = "运行Web/Wasm网页版"
                dependsOn(tasks.named("wasmJsBrowserDevelopmentRun"))
            }

            val outputJsDir = C.root.outputs.dir("webJs")
            val outputWasmDir = C.root.outputs.dir("webWasm")

            tasks.register("webArtifact") {
                description = "打包网页版"
                dependsOn(tasks.named("jsBrowserDistribution"))
                dependsOn(tasks.named("wasmJsBrowserDistribution"))

                doLast {
                    copy {
                        from(jsBinaryDir)
                        into(outputJsDir)
                    }
                    delete(*outputJsDir.asFile.listFiles { it.extension == "map" || it.extension == "txt" })
                    copy {
                        from(wasmBinaryDir)
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
                    copy {
                        from(wasmBinaryDir)
                        into(outputWasmDir)
                    }
                    delete(*outputWasmDir.asFile.listFiles { it.extension == "map" || it.extension == "txt" })
                    zip(outputWasmDir, C.root.outputs.file("[WebWasm]${C.app.displayName}${C.versionName}.zip"))
                    delete(outputWasmDir)
                }
            }
        }
    }
}
