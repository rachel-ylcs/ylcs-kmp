import love.yinlin.repository.C
import love.yinlin.repository.localProperties
import org.gradle.api.tasks.Copy

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
            val androidKeyName = localProperties.androidKeyName
            val androidKeyPassword = localProperties.androidKeyPassword
            val androidKey = C.root.app.config.androidKey.asFile
            if (androidKeyName.isNullOrBlank() || androidKeyPassword.isNullOrBlank() || !androidKey.isFile) {
                signingConfigs.getByName("debug")
            }
            else {
                signingConfigs {
                    register(androidKeyName) {
                        keyAlias = androidKeyName
                        keyPassword = androidKeyPassword
                        storeFile = androidKey
                        storePassword = androidKeyPassword
                    }
                }
                signingConfigs.getByName(androidKeyName)
            }
        }

        actions {
            tasks.register("androidPackage") {
                description = "安卓端Debug编译"
                dependsOn(tasks.named("assembleDebug"))
            }

            val releaseApk = provider { originOutput }
            for ((taskName, filename) in mapOf(
                "androidArtifact" to "ylcs-android.apk",
                "androidPublish" to "[Android]${C.app.displayName}${C.versionName}.apk",
            )) {
                tasks.register<Copy>(taskName) {
                    description = if (taskName == "androidArtifact") "安卓端Release打包" else "发布安卓端安装包"
                    dependsOn(tasks.named("assembleRelease"))

                    from(releaseApk)
                    into(C.root.outputs)
                    rename { filename }
                }
            }
        }
    }
}
