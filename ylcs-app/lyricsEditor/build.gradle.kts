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
    override val packageName: String = "love.yinlin.lyricseditor"
    override val packageVersion: Int = 100
    override val packageVersionName: String = "1.0.0"

    override fun KotlinAndroidSourceSetsScope.source() {
        main.configure {
            lib(projects.ylcsModule.compose.app)
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
        val androidPublish by tasks.registering {
            dependsOn(tasks.named("assembleRelease"))

            doLast {
                copy {
                    from(originOutput)
                    into(C.root.outputs)
                    rename { _ -> "RachelModLyricsEditor.APK" }
                }
            }
        }
    }
})