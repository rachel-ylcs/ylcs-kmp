import love.yinlin.repository.BuildEnvironment

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("ylcs.repository")
}

repository {
    name = "ylcs-kmp"
    typeSafeProjectAccessors = true

    shared {
        namespace = "love.yinlin"
        homepage = "https://github.com/rachel-ylcs/ylcs-kmp"
    }

    rachel {
        stripFirstPathSegment = true
        languageFeatures = listOf(
            "-Xexpect-actual-classes",
            "-Xreturn-value-checker=check",
            "-Xcollection-literals",
        )
        resourceExcludes = listOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin")
        resourceClassPublic = true
        composeReports = true
        stabilityDirectory = "config"
        jsTarget = "es2020"

        jvm {
            version = 25
            androidVersion = 25
            args = listOf("--enable-native-access=ALL-UNNAMED", "-XX:+UseCompactObjectHeaders")
        }
        android {
            minSdk = 26
            compileSdk(37, 1)
            targetSdk = 37
            ndkAbi = listOf("arm64-v8a")
            ndkVersion = "29.0.14206865"
            proguardRule = "proguard-android-optimize.txt"
        }
        ios {
            target = "26.0"
            linkUIKit = true
            disableKotlin250NativeCache = true
        }
        desktop {
            proguardVersion = "7.10.0"
        }
        native {
            outputDirectory = "artifacts/desktopNative"
            jniHeadersDirectory = "artifacts/include"
            buildType = "Release"
            parallelism = 4
        }
        publishing {
            central = true
            automaticRelease = true
            validateDeployment = false
            sign = true
            inceptionYear = "2025"
            licenseName = "MIT License"
            licenseUrl = "https://opensource.org/license/MIT"
        }
    }

    ylcs {
        app {
            name = "ylcs"
            projectName = "ylcs-app"
            displayName = "银临茶舍"
            description = "银临茶舍KMP跨平台APP"
            vendor = "银临茶舍"
            copyright = "© 2024-2026 银临茶舍 版权所有"
        }
        desktopModules = listOf("java.instrument", "java.net.http", "java.management", "jdk.unsupported")
        modManager {
            name = "ModManager"
            displayName = "银临茶舍Mod管理器"
            mainClass = "love.yinlin.ModManagerKt"
        }
        host {
            environment = BuildEnvironment.Prod
            mainHost = "yinlin.love"
            mainPort = 1211
            webServerPort = 8000
        }
    }

    modules {
        module("ylcs-module") {
            /* -----------------  工具模块  ----------------- */

            module("build-tool")
            module("dokka")

            /* -----------------  库模块  ----------------- */

            module("compose") {
                module("app")
                module("cache")
                module("components") {
                    module("blur")
                    module("calendar")
                    module("data-table")
                    module("drag-drop")
                    module("hexview")
                    module("log-output")
                    module("lottie")
                    module("media")
                    module("pagination-layout")
                    module("qrcode")
                    module("reordering")
                    module("rich-text")
                    module("url-image")
                    module("webview")
                }
                module("core")
                module("game") {
                    module("core")
                }
                module("icons")
                module("platform-view")
                module("screen")
                module("startup") {
                    module("config")
                    module("exception")
                    module("mmkv-kmp")
                    module("picker")
                }
                module("theme")
                module("ui")
            }
            module("core")
            module("cs") {
                module("core")
                module("client-engine")
                module("server-engine")
                module("server-service-mysql")
                module("server-service-redis")
            }
            module("foundation") {
                module("compression")
                module("context")
                module("cryptography")
                module("filesystem")
                module("fts")
                module("network")
                module("parser") {
                    module("arguments")
                    module("lrc")
                    module("markdown")
                    module("toml")
                    module("yaml")
                }
                module("startup")
            }
            module("native") {
                module("win32") {
                    module("core")
                }
            }
            module("platform") {
                module("ffi") {
                    module("core")
                    module("posix")
                    module("win32")
                }
                module("native-lib-loader")
                module("os") {
                    module("single-instance")
                    module("window")
                }
            }
        }

        /* -----------------  应用模块  ----------------- */

        module("ylcs-app") {
            module("app") {
                module("global")
                module("portal")
                module("account")
                module("activity")
                module("community")
                module("game")
                module("game-guess-lyrics")
                module("game-rhyme")
                module("information")
                module("music")
                module("photo")
                module("radio")
                module("viewer")
                module("world")
            }
            module("android-app")
            module("cs")
            module("desktop-app")
            module("gallery")
            module("landpage")
            module("mod")
            module("mod-manager")
            module("modern-screen")
            module("server")
            module("third-party")
            module("web-app")
        }
    }
}
