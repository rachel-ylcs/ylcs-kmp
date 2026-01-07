plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        commonMain.configure {
            kotlin.srcDir(C.root.cs.srcGenerated)
            lib(
                ExportLib,
                projects.ylcsModule.core.cs,
                libs.compose.runtime,
            )
        }
    }

    override fun Project.actions() {
        val generateConstants by tasks.registering {
            val content = """
            package love.yinlin
            
            import love.yinlin.data.AppInfo
            
            // 由构建脚本自动生成，请勿手动修改
            object Local {
                val info = AppInfo(
                    appName = "${C.app.name}",
                    name = "${C.app.displayName}",
                    version = ${C.app.version},
                    versionName = "${C.app.versionName}",
                    minVersion = ${C.app.minVersion},
                    minVersionName = "${C.app.minVersionName}",
                    packageName = "${C.app.packageName}",
                )
                
                const val MAIN_HOST: String = "${C.host.mainHost}"
                const val API_HOST: String = "${C.host.apiHost}"
                const val API_BASE_URL: String = "${C.host.apiUrl}"
            }
        """.trimIndent()
            val constantsFile = C.root.cs.generatedLocalFile.let {
                outputs.file(it)
                it.asFile
            }
            outputs.upToDateWhen {
                constantsFile.takeIf { it.exists() }?.readText() == content
            }
            doLast {
                constantsFile.parentFile.mkdirs()
                constantsFile.writeText(content, Charsets.UTF_8)
            }
        }

        rootProject.tasks.named("prepareKotlinBuildScriptModel").configure {
            dependsOn(generateConstants)
        }

        tasks.matching { it.name.startsWith("compile") }.configureEach {
            dependsOn(generateConstants)
        }
    }
})