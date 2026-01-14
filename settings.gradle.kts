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
    /* -----------------  库模块  ----------------- */

    "ylcs-module:compose:app",
    "ylcs-module:compose:core",
    "ylcs-module:compose:device",
    "ylcs-module:compose:plugin:animated-webp-decoder",
    "ylcs-module:compose:plugin:game",
    "ylcs-module:compose:screen",
    "ylcs-module:compose:startup:config",
    "ylcs-module:compose:theme",
    "ylcs-module:compose:ui:core",
    "ylcs-module:compose:ui:pag-kmp",
    "ylcs-module:compose:ui:pagination-layout",
    "ylcs-module:compose:ui:platform-view",
    "ylcs-module:compose:ui:rachel",
    "ylcs-module:compose:ui:url-image",
    "ylcs-module:compose:ui:webview",
    "ylcs-module:core",
    "ylcs-module:dokka",
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

    "ylcs-app:landpage",
    "ylcs-app:mod",
    "ylcs-app:cs",
    "ylcs-app:shared",
    "ylcs-app:androidApp",
    "ylcs-app:desktopApp",
    "ylcs-app:webApp",
    "ylcs-app:server",
    "ylcs-app:mod-manager",
)