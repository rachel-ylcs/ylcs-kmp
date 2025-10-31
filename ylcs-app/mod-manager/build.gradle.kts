import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    C.useCompilerFeatures(this)

    jvm {
        C.jvmTarget(this)
    }
    
    sourceSets {
        jvmMain.configure {
            useLib(
                projects.ylcsApp.mod,
                projects.ylcsModule.app,
                projects.ylcsModule.compose.component.urlImage,
                libs.compose.components.resources,
            )
        }
    }
}

compose.desktop {
    application {
        mainClass = C.modManager.mainClass

        buildTypes.release.proguard {
            version = C.proguard.version
            isEnabled = true
            optimize = true
            obfuscate = true
            configurationFiles.from(C.root.app.commonR8Rule, C.root.app.desktopR8Rule)
        }

        nativeDistributions {
            packageName = C.modManager.name
            packageVersion = C.app.versionName
            description = C.app.description
            copyright = C.app.copyright
            vendor = C.app.vendor

            targetFormats(TargetFormat.Exe)

            modules(*C.desktop.modules)

            windows {
                console = false
            }
        }
    }
}

afterEvaluate {
    val run = tasks.named("run")
    val createReleaseDistributable = tasks.named("createReleaseDistributable")

    // 运行 桌面程序 Debug
    val modManagerRunDebug by tasks.registering {
        dependsOn(run)
    }

    val modManagerCopyDir by tasks.registering {
        mustRunAfter(createReleaseDistributable)
        doLast {
            copy {
                from(C.root.modManager.desktopOriginOutput)
                into(C.root.outputs)
            }
        }
    }

    val modManagerPublish by tasks.registering {
        dependsOn(createReleaseDistributable)
        dependsOn(modManagerCopyDir)
    }
}