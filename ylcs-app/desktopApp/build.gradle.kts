import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    C.useCompilerFeatures(this)

    jvm("desktop") {
        C.jvmTarget(this)
    }

    sourceSets {
        val desktopMain by getting {
            useLib(
                projects.ylcsApp.shared,
                libs.compose.components.resources,
                projects.ylcsModule.autoUpdate,
                projects.ylcsModule.singleInstance,
            )
        }
    }
}

compose.desktop {
    application {
        mainClass = C.app.mainClass

        if ("desktopRun" in currentTaskName) {
            val desktopWorkSpace = C.root.work.desktop.asFile
            desktopWorkSpace.mkdirs()
            jvmArgs += arrayOf(
                "-Duser.dir=$desktopWorkSpace",
                "-Djava.library.path=${C.root.native.libs}",
            )
        }
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        buildTypes.release.proguard {
            version = C.proguard.version
            isEnabled = true
            optimize = true
            obfuscate = true
            joinOutputJars = true
            configurationFiles.from(C.root.shared.commonR8Rule, C.root.shared.desktopR8Rule)
        }

        nativeDistributions {
            packageName = C.app.name
            packageVersion = C.app.versionName
            description = C.app.description
            copyright = C.app.copyright
            vendor = C.app.vendor
            licenseFile.set(C.root.license)

            targetFormats(TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Pkg)

            modules(*C.desktop.modules)

            windows {
                console = false
                exePackageVersion = C.app.versionName
                iconFile.set(C.root.config.icon)
            }

            linux {
                debPackageVersion = C.app.versionName
                iconFile.set(C.root.config.icon)
            }

            macOS {
                pkgPackageVersion = C.app.versionName
                iconFile.set(C.root.config.icon)
            }
        }
    }
}

afterEvaluate {
    val run = tasks.named("run")
    val runRelease = tasks.named("runRelease")
    val createReleaseDistributable = tasks.named("createReleaseDistributable")
    val suggestRuntimeModules = tasks.named("suggestRuntimeModules")

    // 运行 桌面程序 Debug
    val desktopRunDebug by tasks.registering {
        dependsOn(run)
    }

    // 运行 桌面程序 Release
    val desktopRunRelease by tasks.registering {
        dependsOn(runRelease)
    }

    // 检查桌面模块完整性
    val desktopCheckModules by tasks.registering {
        dependsOn(suggestRuntimeModules)
    }

    val desktopCopyDir by tasks.registering {
        mustRunAfter(createReleaseDistributable)
        doLast {
            copy {
                from(C.root.desktopApp.originOutput)
                into(C.root.outputs)
            }
            copy {
                from(C.root.native.libs)
                into(C.root.desktopApp.libOutput)
            }
            copy {
                from(C.root.config.currentPackages)
                into(C.root.desktopApp.packagesOutput)
            }
            val platformName = when (C.platform) {
                BuildPlatform.Windows -> "[Windows]"
                BuildPlatform.Linux -> "[Linux]"
                BuildPlatform.Mac -> "[MacOS]"
            }
            zip {
                from(C.root.outputs.dir(C.app.name).dir("app"))
                to(C.root.outputs.file("$platformName${C.app.displayName}${C.app.versionName}升级包.zip"))
            }
            zip {
                from(C.root.outputs.dir(C.app.name))
                to(C.root.outputs.file("$platformName${C.app.displayName}${C.app.versionName}.zip"))
            }
            delete {
                delete(C.root.outputs.dir(C.app.name))
            }
        }
    }

    // 发布桌面应用程序
    val desktopPublish by tasks.registering {
        dependsOn(createReleaseDistributable)
        dependsOn(desktopCopyDir)
    }
}