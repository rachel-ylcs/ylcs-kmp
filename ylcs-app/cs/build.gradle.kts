import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    C.useCompilerFeatures(this)

    androidTarget {
        C.jvmTarget(this)
        publishLibraryVariants("release")
    }

    iosArm64()
    if (C.platform == BuildPlatform.Mac) {
        when (C.architecture) {
            BuildArchitecture.AARCH64 -> iosSimulatorArm64()
            BuildArchitecture.X86_64 -> iosX64()
            else -> {}
        }
    }

    jvm("desktop") {
        C.jvmTarget(this)
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                enabled = false
            }
        }
        binaries.executable()
        binaries.library()
    }

    sourceSets {
        commonMain.configure {
            kotlin.srcDir(C.root.cs.srcGenerated)
            useApi(
                projects.ylcsBase.csCore,
            )
            useLib(
                libs.compose.runtime,
            )
        }
    }
}

android {
    namespace = "${C.app.packageName}.base.cs"
    compileSdk = C.android.compileSdk

    defaultConfig {
        minSdk = C.android.minSdk
        lint.targetSdk = C.android.targetSdk
    }

    compileOptions {
        sourceCompatibility = C.jvm.compatibility
        targetCompatibility = C.jvm.compatibility
    }
}

afterEvaluate {
    val generateConstants by tasks.registering {
        val content = """
            package love.yinlin
            
            import love.yinlin.data.AppInfo
            import love.yinlin.platform.Platform
            import love.yinlin.platform.platform
            
            // 由构建脚本自动生成，请勿手动修改
            object Local {
                const val DEVELOPMENT: Boolean = ${C.environment == BuildEnvironment.Dev}
                
                val info = AppInfo(
                    appName = "${C.app.name}",
                    name = "${C.app.displayName}",
                    version = ${C.app.version},
                    versionName = "${C.app.versionName}",
                    packageName = "${C.app.packageName}",
                )
                
                const val MAIN_HOST: String = "${C.host.mainHost}"
                const val API_HOST: String = "${C.host.apiHost}"
                
                val API_BASE_URL: String = run {
                    if (platform == Platform.WebWasm && ${C.host.webUseProxy}) "${C.host.webServerUrl}" else "${C.host.apiUrl}"
                }
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