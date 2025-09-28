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

    "ylcs-core", // 语言核心
    "ylcs-compose-core", // compose核心
    "ylcs-cs-core", // C/S核心

    /* -----------------  [2] 功能模块  ----------------- */



    /* -----------------  [3] 结构模块  ----------------- */

    "ylcs-mod", // MOD
    "ylcs-cs", // C/S共享

    /* -----------------  [4] 程序模块  ----------------- */

    "ylcs-app", // 跨平台APP
    "ylcs-server", // 服务端
    "ylcs-mod-manager", // MOD管理器
)