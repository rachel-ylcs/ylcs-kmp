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
    "ylcs-module:compose:component:all",
    "ylcs-module:compose:component:pagination-layout",
    "ylcs-module:compose:component:url-image",
    "ylcs-module:compose:component:webview",
    "ylcs-module:compose:device",
    "ylcs-module:compose:platform-view",
    "ylcs-module:compose:plugin:animated-webp-decoder",
    "ylcs-module:compose:plugin:game",
    "ylcs-module:compose:screen",
    "ylcs-module:compose:service:all",
    "ylcs-module:compose:service:config",
    "ylcs-module:compose:theme",
    "ylcs-module:compose:ui",
    "ylcs-module:core:base",
    "ylcs-module:core:compose",
    "ylcs-module:core:cs",
    "ylcs-module:foundation:framework:context",
    "ylcs-module:foundation:framework:startup",
    "ylcs-module:foundation:net:client-engine",
    "ylcs-module:foundation:net:server-engine",
    "ylcs-module:foundation:os:auto-update",
    "ylcs-module:foundation:os:single-instance",
    "ylcs-module:foundation:service:all",
    "ylcs-module:foundation:service:exception",
    "ylcs-module:foundation:service:mmkv-kmp",
    "ylcs-module:foundation:service:os",
    "ylcs-module:foundation:service:picker",
    "ylcs-module:native:platform-lib",
    "ylcs-module:platform:kotlin-ffi",

    /* -----------------  应用模块  ----------------- */

    "ylcs-app:mod",
    "ylcs-app:cs",
    "ylcs-app:shared",
    "ylcs-app:androidApp",
    "ylcs-app:desktopApp",
    "ylcs-app:webApp",
    "ylcs-app:server",
    "ylcs-app:mod-manager",
)