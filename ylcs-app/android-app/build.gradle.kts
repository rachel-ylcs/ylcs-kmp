import love.yinlin.repository.C
import love.yinlin.repository.localProperties

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinSerialization,
        libs.plugins.androidApplication,
        libs.plugins.composeCompiler,
    )
}

rachel {
    androidApplication {
        packageName = C.namespace
        packageVersion = C.version
        packageVersionName = C.versionName

        sources {
            lib(projects.ylcsApp.app.portal)
        }

        android {
            androidResources {
                @Suppress("UnstableApiUsage")
                localeFilters += listOf("zh-rCN")
            }
        }

        sign {
            val androidKeyName = localProperties.androidKeyName ?: "default"
            val androidKeyPassword = localProperties.androidKeyPassword
            signingConfigs {
                register(androidKeyName) {
                    keyAlias = androidKeyName
                    keyPassword = androidKeyPassword
                    storeFile = C.root.app.config.androidKey.asFile.let {
                        if (it.exists()) it else null
                    }
                    storePassword = androidKeyPassword
                }
            }
            signingConfigs.getByName(androidKeyName)
        }

        actions {
            tasks.register("androidPackage") {
                description = "安卓端Debug编译"
                dependsOn(tasks.named("assembleDebug"))
            }

            tasks.register("androidArtifact") {
                description = "安卓端Release打包"
                dependsOn(tasks.named("assembleRelease"))

                doLast {
                    copy {
                        from(originOutput)
                        into(C.root.outputs)
                        rename { _ -> "ylcs-android.apk" }
                    }
                }
            }

            tasks.register("androidPublish") {
                description = "发布安卓端安装包"
                dependsOn(tasks.named("assembleRelease"))

                doLast {
                    copy {
                        from(originOutput)
                        into(C.root.outputs)
                        rename { _ -> "[Android]${C.app.displayName}${C.versionName}.apk" }
                    }
                }
            }
        }
    }
}
