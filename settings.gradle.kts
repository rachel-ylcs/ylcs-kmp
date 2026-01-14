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
    "ylcs-module:compose:component:pagination-layout",
    "ylcs-module:compose:component:url-image",
    "ylcs-module:compose:component:webview",
    "ylcs-module:compose:device",
    "ylcs-module:compose:platform-view",
    "ylcs-module:compose:plugin:animated-webp-decoder",
    "ylcs-module:compose:plugin:game",
    "ylcs-module:compose:screen",
    "ylcs-module:compose:service:config",
    "ylcs-module:compose:theme",
    "ylcs-module:compose:ui",
    "ylcs-module:core:base",
    "ylcs-module:core:compose",
    "ylcs-module:core:cs",
    "ylcs-module:dokka",
    "ylcs-module:foundation:context",
    "ylcs-module:foundation:cs:client-engine",
    "ylcs-module:foundation:cs:server-engine",
    "ylcs-module:foundation:os:auto-update",
    "ylcs-module:foundation:os:desktop-player",
    "ylcs-module:foundation:os:single-instance",
    "ylcs-module:foundation:os:window",
    "ylcs-module:foundation:service:exception",
    "ylcs-module:foundation:service:mmkv-kmp",
    "ylcs-module:foundation:service:os",
    "ylcs-module:foundation:service:picker",
    "ylcs-module:foundation:startup",
    "ylcs-module:native:win32:core",
    "ylcs-module:platform:ffi:core",
    "ylcs-module:platform:ffi:posix",
    "ylcs-module:platform:ffi:win32",
    "ylcs-module:platform:native-lib-loader",

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