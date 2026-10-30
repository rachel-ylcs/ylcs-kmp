import love.yinlin.gradle.ExportLib

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.nativeBridge,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        desktop {
            cpp = listOf(rootProject.file("native-compose/vendor/sdl-source-${libs.versions.sdl.get()}").path)
        }

        native {
            cpp = listOf("sdl3")
            compilations.getByName("main").cinterops.getByName("sdl3") {
                // 独立生成声明，避免 Skiko 的同名头文件使共享 metadata 丢失 SDL API。
                dependencyFiles = files()
                includeDirs(rootProject.file("native-compose/vendor/sdl-source-${libs.versions.sdl.get()}/include"))
            }
        }

        sources {
            commonMain {
                lib(
                    ExportLib,
                    projects.ylcsModule.foundation.filesystem,
                )
            }

            iosMain(commonMain)

            iosMainList(iosMain)

            desktopMain(commonMain) {
                lib(projects.ylcsModule.platform.nativeLibLoader)
            }

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)

            nativeMain(commonMain) {
                lib(libs.skiko)
            }

            windowsMain(nativeMain)

            linuxMain(nativeMain)

            macosMain(nativeMain)
        }
    }
}
