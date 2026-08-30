import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    install(
        libs.plugins.kotlinSerialization,
        libs.plugins.androidApplication,
        libs.plugins.composeCompiler,
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
                storeFile = C.root.app.config.androidKey.asFile
                storePassword = androidKeyPassword
            }
        }
        signingConfigs.getByName(androidKeyName)
    } catch (e: Throwable) {
        println("[AndroidSigning] can't load android signing config, error: ${e.message}")
        null
    }

    override fun Project.actions() {
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
                    rename { _ -> "[Android]${C.app.displayName}${C.app.versionName}.apk" }
                }
            }
        }
    }
})