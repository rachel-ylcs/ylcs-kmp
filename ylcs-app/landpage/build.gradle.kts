import love.yinlin.gradle.BrowserArtifactTarget
import love.yinlin.repository.C
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
    )
}

rachel {
    js {
        sources {
            jsMain(commonMain) {
                lib(
                    libs.kotlinx.html,
                    projects.ylcsModule.core,
                    projects.ylcsApp.cs,
                )
            }
        }

        webpack {
            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                port = C.web.webServerPort
                client?.overlay = false
                open = false
            }
        }

        artifacts {
            zip(
                "landpagePublish", jsDistribution, C.root.outputs, "landpage.zip",
                description = "发布落地页",
                archiveRoot = "landpage",
                excludes = listOf("*.map", "*.txt"),
                replaceUnpackedOutput = true,
            )
        }

        runs {
            browser("landpageRun", BrowserArtifactTarget.Js, "运行落地页")
        }
    }
}
