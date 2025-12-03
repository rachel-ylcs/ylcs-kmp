import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "${C.app.projectName}.js"
                cssSupport {
                    enabled = true
                }
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
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.configure {
            useLib(
                projects.ylcsApp.shared
            )
        }
    }
}

afterEvaluate {
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