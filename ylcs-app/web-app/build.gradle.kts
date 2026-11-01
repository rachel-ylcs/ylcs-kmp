import love.yinlin.gradle.jsBinaryDir
import love.yinlin.repository.C
import love.yinlin.gradle.wasmBinaryDir
import org.gradle.api.tasks.bundling.Zip
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

            for ((taskName, filename) in mapOf(
                "webJsArtifact" to "ylcs-js.zip",
                "webWasmArtifact" to "ylcs-wasm.zip",
                "webPublish" to "[WebWasm]${C.app.displayName}${C.versionName}.zip",
            )) {
                val isJs = taskName == "webJsArtifact"
                val binaryDirectory = if (isJs) jsBinaryDir else wasmBinaryDir
                val outputDirectory = if (isJs) "webJs" else "webWasm"
                tasks.register<Zip>(taskName) {
                    description = when (taskName) {
                        "webJsArtifact" -> "打包Web/Js网页版"
                        "webWasmArtifact" -> "打包Web/Wasm网页版"
                        else -> "发布网页版"
                    }
                    dependsOn(tasks.named(if (isJs) "jsBrowserDistribution" else "wasmJsBrowserDistribution"))

                    from(binaryDirectory) {
                        into(outputDirectory)
                        exclude("*.map", "*.txt")
                    }
                    destinationDirectory.set(C.root.outputs)
                    archiveFileName.set(filename)
                    doFirst { delete(C.root.outputs.dir(outputDirectory)) }
                }
            }

            tasks.register("webArtifact") {
                description = "打包网页版"
                dependsOn(tasks.named("webJsArtifact"), tasks.named("webWasmArtifact"))
            }
        }
    }
}
