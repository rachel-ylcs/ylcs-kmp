import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("build/generated/kotlin")
            dependencies {
                implementation(libs.compose.runtime)

                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.json)
            }
        }

        val nonAndroidMain by creating {
            dependsOn(commonMain)
            dependencies {

            }
        }

        val nonWasmJsMain by creating {
            dependsOn(commonMain)
            dependencies {

            }
        }

        androidMain.get().apply {
            dependsOn(nonWasmJsMain)
            dependencies {

            }
        }

        val iosMain = iosMain.get().apply {
            dependsOn(nonAndroidMain)
            dependsOn(nonWasmJsMain)
            dependencies {

            }
        }

        listOf(
            iosX64Main,
            iosArm64Main,
            iosSimulatorArm64Main
        ).forEach {
            it.get().apply {
                dependsOn(iosMain)
                dependencies {

                }
            }
        }

        val desktopMain by getting {
            dependsOn(nonAndroidMain)
            dependsOn(nonWasmJsMain)
            dependencies {

            }
        }

        wasmJsMain.get().apply {
            dependsOn(nonAndroidMain)
            dependencies {

            }
        }
    }
}

android {
    namespace = "${rootProject.extra["appPackageName"]}.shared"
    compileSdk = rootProject.extra["androidBuildSDK"] as Int

    defaultConfig {
        minSdk = rootProject.extra["androidMinSDK"] as Int
        lint.targetSdk = rootProject.extra["androidBuildSDK"] as Int
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

afterEvaluate {
    val generateConstants by tasks.registering {
        val constantsFile = file("build/generated/kotlin/love/yinlin/Constants.kt")
        outputs.file(constantsFile)
        val content = """
            package love.yinlin
            import love.yinlin.platform.Platform
            import love.yinlin.platform.platform
            
            // 由构建脚本自动生成，请勿手动修改
            object Local {
                const val DEVELOPMENT: Boolean = ${rootProject.extra["environment"] == "Dev"}
                
                const val APP_NAME: String = "${rootProject.extra["appName"]}"
                const val NAME: String = "${rootProject.extra["appDisplayName"]}"
                const val VERSION: Int = ${rootProject.extra["appVersion"]}
                const val VERSION_NAME: String = "${rootProject.extra["appVersionName"]}"
                
                const val LOCAL_HOST: String = "localhost"
                const val MAIN_HOST: String = "${rootProject.extra["mainHost"]}"
                @Suppress("HttpUrlsUsage")
                val API_BASE_URL: String = run {
                    if (platform == Platform.WebWasm && ${rootProject.extra["webUseProxy"]}) {
                        "http://${'$'}LOCAL_HOST:${rootProject.extra["webServerPort"]}"
                    } else {
                        "${rootProject.extra["apiBaseUrl"]}"
                    }
                }
            }
        """.trimIndent()

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