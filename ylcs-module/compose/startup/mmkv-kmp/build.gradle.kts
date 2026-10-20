import love.yinlin.gradle.ExportLib
import love.yinlin.gradle.SwiftPackage

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.filesystem,
                    projects.ylcsModule.foundation.startup,
                )
            }

            androidMain(commonMain) {
                lib(libs.mmkv.android)
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(projects.ylcsModule.platform.nativeLibLoader)
            }

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)
        }

        // 仅导入 MMKV 的公开 API，避免扫描 MMKVCoreOpenSSL 中需要宏定义的内部模板头文件。
        swiftDiscoverClangModulesImplicitly = false

        swiftPackages = listOf(
            SwiftPackage.Remote("https://github.com/Tencent/MMKV.git", libs.versions.mmkv.get(), listOf("MMKV")),
            SwiftPackage.Local("Dummy"),
        )
    }
}
