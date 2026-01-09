import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
    override val exportResource: Boolean = true

    override fun KotlinNativeTarget.ios() {
        if (C.platform == BuildPlatform.Mac) {
            compilations.getByName("main") {
                val nskeyvalueobserving by cinterops.creating
            }
        }
    }

    override val cocoapodsList: List<Pod> = listOf(
        Pod("YLCSCore", moduleName = "YLCSCore", source = C.root.iosApp.core.asFile),
        Pod("MMKV", version = libs.versions.mmkv),
        Pod("MobileVLCKit", version = libs.versions.vlcKit),
        Pod("SGQRCode", version = libs.versions.sgQrcode),
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
                projects.ylcsApp.mod,
                projects.ylcsModule.foundation.net.clientEngine,
                projects.ylcsModule.foundation.service.exception,
                projects.ylcsModule.foundation.service.mmkvKmp,
                projects.ylcsModule.compose.platformView,
                projects.ylcsModule.compose.component.paginationLayout,
                projects.ylcsModule.compose.component.urlImage,
                projects.ylcsModule.compose.component.webview,
                projects.ylcsModule.compose.plugin.game,
                libs.compose.components.resources,
                libs.lottie,
                libs.lottie.network,
                libs.tool.html,
                libs.tool.blur,
                libs.tool.reorder,
                libs.tool.qrcode,
                ExportLib,
                projects.ylcsApp.cs,
                projects.ylcsModule.foundation.service.os,
                projects.ylcsModule.foundation.service.picker,
                projects.ylcsModule.compose.app,
                projects.ylcsModule.compose.screen,
                projects.ylcsModule.compose.service.config,
            )
        }

        val jvmMain by create(commonMain) {
            lib(
                fileTree(mapOf("dir" to "libs/jvm", "include" to listOf("*.jar")))
            )
        }

        androidMain.configure(jvmMain) {
            lib(
                libs.scan.android,
                libs.scan.camera.android,
                fileTree(mapOf("dir" to "libs/android", "include" to listOf("*.aar", "*.jar"))),
                ExportLib,
                libs.media3.ui,
                libs.media3.session,
                libs.media3.player,
            )
        }

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(jvmMain) {
            lib(
                projects.ylcsModule.foundation.os.window,
                projects.ylcsModule.foundation.os.desktopPlayer,
                fileTree(mapOf("dir" to "libs/desktop", "include" to listOf("*.jar")))
            )
        }

        wasmJsMain.configure(commonMain)
    }

    override fun ComposeCompilerGradlePluginExtension.composeCompiler() {
        stabilityConfigurationFiles.add(C.root.config.stability)
        reportsDestination = C.root.shared.composeCompilerReport
        metricsDestination = C.root.shared.composeCompilerReport
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