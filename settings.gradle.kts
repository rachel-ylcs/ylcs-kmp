import love.yinlin.repository.nativeCompose

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("ylcs.repository")
}

nativeCompose()

repository {
    name = "ylcs-kmp"
    namespace = "love.yinlin"
    homepage = "https://github.com/rachel-ylcs/ylcs-kmp"
    organization = "银临茶舍"
    copyright = "© 2024-2026 银临茶舍 版权所有"

    rachel {
        stripFirstPathSegment = true

        languageFeatures = listOf(
            "-Xexpect-actual-classes",
            "-Xcollection-literals",
        )

        resourceExcludes = listOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "DebugProbesKt.bin"
        )

        composeReports = true

        jvm {
            version = 25
            androidVersion = 25
            args = listOf(
                "--enable-native-access=ALL-UNNAMED",
                "-XX:+UseCompactObjectHeaders"
            )
        }

        android {
            minSdk = 28
            compileSdk = 37 to 1
            targetSdk = 37
            ndkAbi = listOf("arm64-v8a")
            ndkVersion = "29.0.14206865"
        }

        ios {
            target = "26.0"
            linkUIKit = true
        }

        desktop {
            proguardVersion = "7.10.0"
        }

        web {
            webServerPort = 8000
            jsTarget = "es2020"
        }

        native {
            outputDirectory = "artifacts/desktopNative"
            jniHeadersDirectory = "artifacts/include"
            buildType = "Release"
            parallelism = 8
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
        }

        cs {
            apiUrl = "https://api.yinlin.love"
        }
    }

    modules {
        module("ylcs-module") {
            /* -----------------  工具模块  ----------------- */

            module("build-tool", "dokka")

            /* -----------------  库模块  ----------------- */

            module("core")

            module("foundation") {
                module("context")
                module("filesystem", "network", "startup")
                module("compression", "cryptography")
                module("fts")
                module("parser") {
                    module("arguments", "lrc", "markdown", "toml", "yaml")
                }
            }

            module("compose") {
                module("core")
                module("theme", "ui", "icons", "platform-view", "screen", "cache", "app")
                module("game") {
                    module("core")
                }
                module("startup") {
                    module("config", "exception", "mmkv-kmp", "picker")
                }
                module("components") {
                    module(
                        "image",
                        "blur",
                        "calendar",
                        "data-table",
                        "drag-drop",
                        "hexview",
                        "log-output",
                        "lottie",
                        "media",
                        "pagination-layout",
                        "qrcode",
                        "reordering",
                        "rich-text",
                        "webview"
                    )
                }
            }

            module("cs") {
                module("core")
                module("client-engine")
                module("server-engine")
                module("server-service-mysql", "server-service-redis")
            }

            module("native") {
                module("win32") {
                    module("core")
                }
            }

            module("platform") {
                module("native-lib-loader")
                module("ffi") {
                    module("core")
                    module("posix", "win32")
                }
                module("os") {
                    module("single-instance")
                    module("window")
                }
            }
        }

        /* -----------------  应用模块  ----------------- */

        module("ylcs-app") {
            module("native-compose")
            module("app") {
                module("global", "portal")
                module("viewer")
                module("information", "activity", "community", "account", "music", "photo", "radio", "world")
                module("game", "game-guess-lyrics", "game-rhyme")
            }

            module("cs", "mod", "third-party")
            module("gallery", "modern-screen")
            module("mod-manager", "landpage")
            module("android-app", "desktop-app", "web-app", "server")
        }
    }
}
