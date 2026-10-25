plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.nativeBridge
    )
}

rachel {
    nativeExecutable {
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        sources {
            commonMain {
                lib(
                    projects.ylcsModule.core,
                    libs.skiko,
                    libs.skiko.lottie,
                    libs.compose.ui.util,
                    libs.compose.ui.geometry,
                    libs.compose.ui.unit,
                    libs.compose.ui.graphics,
                    libs.compose.ui.text,
                    libs.compose.ui.skiko,
                    libs.compose.ui.asProvider(),
                )
            }
        }

        executable {
            entryPoint = "love.yinlin.main"
        }
    }
}

nativeApplication {
    skikoWindow()
}
