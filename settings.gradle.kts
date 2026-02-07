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
    "ylcs-module:compose:test",

    /* -----------------  库模块  ----------------- */

    "ylcs-module:compose:app",
    "ylcs-module:compose:components:pag-kmp",
    "ylcs-module:compose:components:pagination-layout",
    "ylcs-module:compose:components:url-image",
    "ylcs-module:compose:components:webview",
    "ylcs-module:compose:core",
    "ylcs-module:compose:platform-view",
    "ylcs-module:compose:plugin:animated-webp-decoder",
    "ylcs-module:compose:plugin:game",
    "ylcs-module:compose:screen",
    "ylcs-module:compose:startup:config",
    "ylcs-module:compose:theme",
    "ylcs-module:compose:ui",
    "ylcs-module:core",
    "ylcs-module:cs:core",
    "ylcs-module:cs:client-engine",
    "ylcs-module:cs:server-engine",
    "ylcs-module:foundation:context",
    "ylcs-module:foundation:startup",
    "ylcs-module:native:win32:core",
    "ylcs-module:platform:ffi:core",
    "ylcs-module:platform:ffi:posix",
    "ylcs-module:platform:ffi:win32",
    "ylcs-module:platform:native-lib-loader",
    "ylcs-module:platform:os:auto-update",
    "ylcs-module:platform:os:desktop-player",
    "ylcs-module:platform:os:single-instance",
    "ylcs-module:platform:os:window",
    "ylcs-module:startup:exception",
    "ylcs-module:startup:mmkv-kmp",
    "ylcs-module:startup:os",
    "ylcs-module:startup:picker",

    /* -----------------  应用模块  ----------------- */

    "ylcs-app:androidApp",
    "ylcs-app:cs",
    "ylcs-app:desktopApp",
    "ylcs-app:gallery",
    "ylcs-app:landpage",
    "ylcs-app:mod",
    "ylcs-app:mod-manager",
    "ylcs-app:server",
    "ylcs-app:shared",
    "ylcs-app:webApp",
)