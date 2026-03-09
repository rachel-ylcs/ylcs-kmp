import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    install(
        libs.plugins.androidApplication,
        libs.plugins.composeCompiler,
        libs.plugins.kotlinSerialization,
    )
}

template(object : KotlinAndroidTemplate() {
    override val packageName: String = C.app.packageName
    override val packageVersion: Int = C.app.version
    override val packageVersionName: String = C.app.versionName

    override fun KotlinAndroidSourceSetsScope.source() {
        lib(projects.ylcsApp.app.portal)
    }

    override fun ApplicationExtension.sign(): ApkSigningConfig? = try {
        val localProperties = Properties().also { p ->
            C.root.localProperties.asFile.inputStream().use { p.load(it) }
        }
        val androidKeyName = localProperties.getProperty("androidKeyName")
        val androidKeyPassword = localProperties.getProperty("androidKeyPassword")
        signingConfigs {
            register(androidKeyName) {
                keyAlias = androidKeyName
                keyPassword = androidKeyPassword
                storeFile = C.root.config.androidKey.asFile
                storePassword = androidKeyPassword
            }
        }
        signingConfigs.getByName(androidKeyName)
    } catch (e: Throwable) {
        println("[AndroidSigning] can't load android signing config, error: ${e.message}")
        null
    }

    override fun Project.actions() {
        // 安卓打包
        val androidPackage by tasks.registering {
            dependsOn(tasks.named("assembleDebug"))
        }

        val androidArtifact by tasks.registering {
            dependsOn(tasks.named("assembleRelease"))

            doLast {
                copy {
                    from(originOutput)
                    into(C.root.outputs)
                    rename { _ -> "ylcs-android.apk" }
                }
            }
        }

        // 发布安卓安装包
        val androidPublish by tasks.registering {
            dependsOn(tasks.named("assembleRelease"))

            doLast {
                copy {
                    from(originOutput)
                    into(C.root.outputs)
                    rename { _ -> "[Android]${C.app.displayName}${C.app.versionName}.APK" }
                }
            }
        }
    }
})