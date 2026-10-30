plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
    )
}

rachel {
    multiplatform {
        nativeDesktopTarget = true

        sources {
            posixMain(commonMain)

            jvmMain(commonMain)

            androidMain(jvmMain)

            iosMain(posixMain)

            iosMainList(iosMain)

            desktopMain(jvmMain)

            webMain(commonMain)

            jsMain(webMain)

            wasmJsMain(webMain)

            windowsMain(commonMain)

            linuxMain(posixMain)

            macosMain(posixMain)
        }
    }
}
