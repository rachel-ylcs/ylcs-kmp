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
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
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

    "ylcs-core:base", // 语言核心
    "ylcs-core:compose-base", // compose核心
    "ylcs-core:cs-base", // C/S核心

    /* -----------------  [2] 功能模块  ----------------- */

    "ylcs-module:client-engine",
    "ylcs-module:server-engine",

    /* -----------------  [3] 结构模块  ----------------- */

    "ylcs-app:mod", // MOD
    "ylcs-app:cs", // C/S共享

    /* -----------------  [4] 程序模块  ----------------- */

    "ylcs-app:app", // 跨平台APP
    "ylcs-app:server", // 服务端
    "ylcs-app:mod-manager", // MOD管理器

    /* -----------------  [5] 测试模块  ----------------- */

    "ylcs-test:nav3",
)