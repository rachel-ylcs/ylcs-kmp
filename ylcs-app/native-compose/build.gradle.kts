plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
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
                    projects.nativeCompose.adaptation.samples.skikoWindow,
                    libs.skiko,
                    libs.skiko.lottie,
                )
            }
        }

        executable { entryPoint = "love.yinlin.main" }
    }
}

nativeApplication {
    skikoWindow()
}
