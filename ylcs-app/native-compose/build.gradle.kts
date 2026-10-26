plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.composeMultiplatform,
        libs.plugins.nativeBridge
    )
}

rachel {
    multiplatform {
        iosTarget = false
        webTarget = false
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        sources {
            commonMain {
                lib(libs.compose.foundation, libs.compose.resources, libs.androidx.navigation3, libs.androidx.navigation3.viewmodel)
            }
            nativeMain(commonMain)
            windowsMain(nativeMain)
            linuxMain(nativeMain)
            macosMain(nativeMain)
        }

        desktopPackage {
            mainClass = "love.yinlin.DesktopMainKt"
            packageName = "native_compose_jvm"
            windows { iconFile.set(rootProject.file("ylcs-app/config/icon.ico")) }
            linux { iconFile.set(rootProject.file("ylcs-app/config/icon.png")) }
            macos { iconFile.set(rootProject.file("ylcs-app/config/icon.icns")) }
        }

        composeDesktopPackage {
            entryPoint = "love.yinlin.main"
            packageName = "native_compose"
            displayName = "Rachel Native Compose"
            packageVersion = libs.versions.rachel.get()
            description = "Rachel Desktop Native Compose"
            windows { iconFile = rootProject.file("ylcs-app/config/icon.ico") }
            linux { iconFile = rootProject.file("ylcs-app/config/icon.png") }
            macos { iconFile = rootProject.file("ylcs-app/config/icon.icns") }
        }
    }
}
