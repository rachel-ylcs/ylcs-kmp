import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinCocoapods,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override val cocoapodsList: List<Pod> = listOf(
        pod("YLCSCore", moduleName = "YLCSCore", source = C.root.iosApp.core.asFile),
        pod("MobileVLCKit", libs.versions.vlcKit),
    )

    override fun CocoapodsExtension.cocoapods() {
        name = C.app.projectName
        summary = C.app.description
        homepage = C.app.homepage

        framework {
            baseName = C.app.projectName
            isStatic = true
        }
    }

    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            lib(
                libs.compose.resources,
                ExportLib,
                projects.ylcsApp.app.account,
                projects.ylcsApp.app.community,
                projects.ylcsApp.app.game,
                projects.ylcsApp.app.gameGuessLyrics,
                projects.ylcsApp.app.gameRhyme,
                projects.ylcsApp.app.global,
                projects.ylcsApp.app.information,
                projects.ylcsApp.app.music,
                projects.ylcsApp.app.thirdParty,
                projects.ylcsApp.app.viewer,
            )
        }
    }

    override fun Project.actions() {
        // 生成苹果版本号配置
        val appleGenVersionConfig by tasks.registering {
            val content = """
                BUNDLE_VERSION=${C.app.version}
                BUNDLE_SHORT_VERSION_STRING=${C.app.versionName}
            """.trimIndent()

            val configFile = C.root.iosApp.configurationFile.asFile
            outputs.file(configFile)
            outputs.upToDateWhen {
                configFile.takeIf { it.exists() }?.readText() == content
            }
            doLast {
                configFile.writeText(content)
            }
        }
    }
})