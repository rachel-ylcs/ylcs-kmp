import love.yinlin.gradle.*

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
        windowsTarget = true
        linuxTarget = true
        macosTarget = true

        sources {
            val posixMain = createPosix(commonMain)

            val jvmMain = createJvm(commonMain)

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
