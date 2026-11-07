import love.yinlin.gradle.BrowserArtifactTarget
import love.yinlin.repository.C
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

        artifacts {
            val js = browserDistribution(BrowserArtifactTarget.Js)
            val wasm = browserDistribution(BrowserArtifactTarget.Wasm)
            val excludes = listOf("*.map", "*.txt")
            val jsArtifact = zip("webJsArtifact", js, C.root.outputs, "ylcs-js.zip", "打包Web/Js网页版", "webJs", excludes, replaceUnpackedOutput = true)
            val wasmArtifact = zip("webWasmArtifact", wasm, C.root.outputs, "ylcs-wasm.zip", "打包Web/Wasm网页版", "webWasm", excludes, replaceUnpackedOutput = true)
            zip("webJsPublish", js, C.root.outputs, "[WebJs]${C.app.displayName}${C.versionName}.zip", "发布Web/Js网页版", "webJs", excludes, replaceUnpackedOutput = true)
            zip("webPublish", wasm, C.root.outputs, "[WebWasm]${C.app.displayName}${C.versionName}.zip", "发布网页版", "webWasm", excludes, replaceUnpackedOutput = true)
            aggregate("webArtifact", "打包网页版", jsArtifact, wasmArtifact)
        }

        runs {
            browser("webJsRun", BrowserArtifactTarget.Js, "运行Web/Js网页版")
            browser("webRun", BrowserArtifactTarget.Wasm, "运行Web/Wasm网页版")
        }
    }
}
