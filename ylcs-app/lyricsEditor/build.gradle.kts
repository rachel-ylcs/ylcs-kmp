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
    override val packageName: String = "${C.app.packageName}.lyricseditor"
    override val packageVersion: Int = 100
    override val packageVersionName: String = "1.0.0"

    override fun KotlinAndroidSourceSetsScope.source() {
        lib(
            projects.ylcsModule.compose.app,
            projects.ylcsModule.compose.screen,

            projects.ylcsApp.mod,
        )
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