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
    // 其中核心模块和功能模块可发布 maven

    /* -----------------  [1] 核心模块  ----------------- */

    "ylcs-base:core",
    "ylcs-base:compose-core",
    "ylcs-base:cs-core",

    /* -----------------  [2] 功能模块  ----------------- */

    "ylcs-module:compose:app",
    "ylcs-module:compose:context",
    "ylcs-module:compose:device",
    "ylcs-module:compose:theme",
    "ylcs-module:compose:startup",
    "ylcs-module:compose:platform-view",
    "ylcs-module:compose:ui",
    "ylcs-module:compose:screen",
    "ylcs-module:compose:game",
    "ylcs-module:compose:animated-webp-decoder",

    "ylcs-module:compose:service:all",
    "ylcs-module:compose:service:exception",
    "ylcs-module:compose:service:os",
    "ylcs-module:compose:service:mmkv-kmp",
    "ylcs-module:compose:service:config",
    "ylcs-module:compose:service:picker",

    "ylcs-module:compose:component:all",
    "ylcs-module:compose:component:pagination-layout",
    "ylcs-module:compose:component:url-image",
    "ylcs-module:compose:component:webview",

    "ylcs-module:client-engine",
    "ylcs-module:server-engine",

    "ylcs-module:kotlin-ffi",

    "ylcs-module:auto-update",
    "ylcs-module:single_instance",

    "ylcs-module:native:platform-lib",

    /* -----------------  [3] 结构模块  ----------------- */

    "ylcs-app:mod",
    "ylcs-app:cs",

    /* -----------------  [4] 程序模块  ----------------- */

    "ylcs-app:shared",

    "ylcs-app:androidApp",
    "ylcs-app:desktopApp",
    "ylcs-app:webApp",

    "ylcs-app:server",
    "ylcs-app:mod-manager",
)