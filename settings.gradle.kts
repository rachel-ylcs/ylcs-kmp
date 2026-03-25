pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    versionCatalogs {
        create("libs").apply {
            from(files("libs.versions.toml"))
        }
    }
}

rootProject.name = "ylcs-kmp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    /* -----------------  工具模块  ----------------- */

    "ylcs-module:build-tool",
    "ylcs-module:dokka",

    /* -----------------  库模块  ----------------- */

    "ylcs-module:compose:app",
    "ylcs-module:compose:cache",
    "ylcs-module:compose:components:drag-drop",
    "ylcs-module:compose:components:lottie",
    "ylcs-module:compose:components:media",
    "ylcs-module:compose:components:pagination-layout",
    "ylcs-module:compose:components:qrcode",
    "ylcs-module:compose:components:rich-text",
    "ylcs-module:compose:components:url-image",
    "ylcs-module:compose:components:webview",
    "ylcs-module:compose:core",
    "ylcs-module:compose:icons",
    "ylcs-module:compose:platform-view",
    "ylcs-module:compose:plugin:animated-webp-decoder",
    "ylcs-module:compose:plugin:animated-webp-decoder-ndk",
    "ylcs-module:compose:plugin:game",
    "ylcs-module:compose:screen",
    "ylcs-module:compose:startup:config",
    "ylcs-module:compose:startup:exception",
    "ylcs-module:compose:startup:mmkv-kmp",
    "ylcs-module:compose:startup:picker",
    "ylcs-module:compose:theme",
    "ylcs-module:compose:ui",
    "ylcs-module:core",
    "ylcs-module:cs:core",
    "ylcs-module:cs:client-engine",
    "ylcs-module:cs:server-engine",
    "ylcs-module:foundation:context",
    "ylcs-module:foundation:filesystem",
    "ylcs-module:foundation:network",
    "ylcs-module:foundation:startup",
    "ylcs-module:native:win32:core",
    "ylcs-module:platform:ffi:core",
    "ylcs-module:platform:ffi:posix",
    "ylcs-module:platform:ffi:win32",
    "ylcs-module:platform:native-lib-loader",
    "ylcs-module:platform:os:auto-update",
    "ylcs-module:platform:os:single-instance",
    "ylcs-module:platform:os:window",
    "ylcs-module:plugin:pag-kmp",

    /* -----------------  应用模块  ----------------- */

    "ylcs-app:app:account",
    "ylcs-app:app:community",
    "ylcs-app:app:game",
    "ylcs-app:app:game-guess-lyrics",
    "ylcs-app:app:game-rhyme",
    "ylcs-app:app:global",
    "ylcs-app:app:information",
    "ylcs-app:app:music",
    "ylcs-app:app:portal",
    "ylcs-app:app:third-party",
    "ylcs-app:app:viewer",

    "ylcs-app:android-app",
    "ylcs-app:cs",
    "ylcs-app:desktop-app",
    "ylcs-app:gallery",
    "ylcs-app:landpage",
    "ylcs-app:mod",
    "ylcs-app:mod-manager",
    "ylcs-app:server",
    "ylcs-app:web-app",

    "ylcs-app:lyrics-editor",
)