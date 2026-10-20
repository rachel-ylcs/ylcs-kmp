import love.yinlin.gradle.*
import love.yinlin.repository.C

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibrary,
    )
}

rachel {
    multiplatform {
        iosFrameworkBaseName = C.app.projectName
        iosFrameworkIsStatic = true

        sources {
            commonMain {
                lib(
                    libs.compose.resources,
                    ExportLib,
                    projects.ylcsApp.app.global,
                    projects.ylcsApp.app.account,
                    projects.ylcsApp.app.activity,
                    projects.ylcsApp.app.community,
                    projects.ylcsApp.app.game,
                    projects.ylcsApp.app.gameGuessLyrics,
                    projects.ylcsApp.app.gameRhyme,
                    projects.ylcsApp.app.information,
                    projects.ylcsApp.app.music,
                    projects.ylcsApp.app.photo,
                    projects.ylcsApp.app.radio,
                    projects.ylcsApp.app.viewer,
                    projects.ylcsApp.app.world,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)
        }

        actions {
            tasks.register("appleGenVersionConfig") {
                description = "生成苹果版本号配置"
                val content = """
                    BUNDLE_VERSION=${C.version}
                    BUNDLE_SHORT_VERSION_STRING=${C.versionName}
                """.trimIndent()

                val configFile = C.root.app.iosApp.configurationFile.asFile
                outputs.file(configFile)
                outputs.upToDateWhen {
                    configFile.takeIf { it.exists() }?.readText() == content
                }
                doLast {
                    configFile.writeText(content)
                }
            }
        }
    }
}
