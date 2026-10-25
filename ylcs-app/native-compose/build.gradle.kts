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
                lib(libs.compose.foundation)
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
