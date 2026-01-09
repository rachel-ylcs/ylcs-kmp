import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.dsl.SigningConfig
import java.util.Properties

plugins {
    install(
        libs.plugins.kotlinAndroid,
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
        main.configure {
            lib(projects.ylcsApp.shared)
        }
    }

    override fun BaseAppModuleExtension.sign(): SigningConfig? = try {
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

        // 发布安卓安装包
        val androidPublish by tasks.registering {
            dependsOn(tasks.named("assembleRelease"))

            doLast {
                copy {
                    from(C.root.androidApp.originOutput)
                    into(C.root.outputs)
                    rename { _ -> "[Android]${C.app.displayName}${C.app.versionName}.APK" }
                }
            }
        }
    }
})