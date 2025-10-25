pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.aliyun.com/repository/gradle-plugin")
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        google()
        maven("https://jitpack.io")
    }

    versionCatalogs {
        create("libs") {
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

    "ylcs-module:service:startup",
    "ylcs-module:service:all",
    "ylcs-module:service:context",
    "ylcs-module:service:os",
    "ylcs-module:compose:device",
    "ylcs-module:compose:theme",
    "ylcs-module:compose:screen",
    "ylcs-module:compose:ui",
    "ylcs-module:compose:app",
    "ylcs-module:compose:component:all",
    "ylcs-module:compose:component:url-image",
    "ylcs-module:os",
    "ylcs-module:client-engine",
    "ylcs-module:server-engine",

    /* -----------------  [3] 结构模块  ----------------- */

    "ylcs-app:mod",
    "ylcs-app:cs",

    /* -----------------  [4] 程序模块  ----------------- */

    "ylcs-app:app",
    "ylcs-app:server",
    "ylcs-app:mod-manager",

    /* -----------------  [5] 测试模块  ----------------- */
)