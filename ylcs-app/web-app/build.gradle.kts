import love.yinlin.gradle.BrowserArtifactTarget
import love.yinlin.repository.C

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

        webDevProxy(port = C.web.webServerPort, outputFileName = "${C.app.projectName}.js")

        artifacts {
            val wasm = browserDistribution(BrowserArtifactTarget.Wasm)
            val excludes = listOf("*.map", "*.txt")
            zip(
                "webArtifact", wasm, C.root.outputs, "ylcs-web.zip", "打包Web网页版", "Web", excludes,
                replaceUnpackedOutput = true,
                omitEmptyComposeResourceDirectories = true,
            )
            zip(
                "webPublish", wasm, C.root.outputs, "[Web]${C.app.displayName}${C.versionName}.zip", "发布Web网页版", "Web", excludes,
                replaceUnpackedOutput = true,
                omitEmptyComposeResourceDirectories = true,
            )
        }

        runs {
            browser("webJsRun", BrowserArtifactTarget.Js, "运行Web/Js网页版")
            browser("webRun", BrowserArtifactTarget.Wasm, "运行Web/Wasm网页版")
        }
    }
}
