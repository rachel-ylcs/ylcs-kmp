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
    nativeExecutable {
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        sources {
            commonMain {
                lib(libs.compose.foundation, libs.compose.resources, libs.androidx.navigation3, libs.androidx.navigation3.viewmodel)
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
