import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import java.io.File
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

class KotlinAndroidSourceSetsScope(private val handler: DependencyHandler) {
    private fun implementation(dependencyNotation: Any) = handler.add("implementation", dependencyNotation)
    private fun api(dependencyNotation: Any) = handler.add("api", dependencyNotation)

    fun lib(vararg libs: Any) {
        var isExport = false
        for (item in libs) {
            when {
                item is ExportLib -> isExport = true
                isExport -> api(item)
                else -> implementation(item)
            }
        }
    }
}

abstract class KotlinAndroidTemplate : KotlinTemplate<KotlinAndroidExtension>() {
    open fun KotlinAndroidSourceSetsScope.source() { }

    open val packageName: String get() = uniqueSafeModuleName
    abstract val packageVersion: Int
    abstract val packageVersionName: String
    open fun ApplicationExtension.sign(): ApkSigningConfig? = null
    open fun ApplicationExtension.android() { }

    val Project.originOutput: File get() {
        val outputDir = layout.buildDirectory.get().dir("outputs").dir("apk").dir("release").asFile
        val outputList = outputDir.listFiles { file ->
            file.isFile && file.extension.equals("apk", true)
        }?.sortedBy { it.name }.orEmpty()
        return when {
            outputList.size == 1 -> outputList.first()
            else -> outputList.firstOrNull { it.name == "${project.name}-release.apk" }
                ?: outputList.firstOrNull()
                ?: outputDir.resolve("${project.name}-release.apk")
        }
    }

    final override fun Project.build(extension: KotlinAndroidExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
                jvmTarget.set(C.jvm.androidTarget)
            }

            action()
        }

        KotlinAndroidSourceSetsScope(dependencies).source()

        extensions.configure<ApplicationExtension> {
            namespace = packageName
            compileSdk = C.android.compileSdk

            compileOptions {
                sourceCompatibility = C.jvm.compatibility
                targetCompatibility = C.jvm.compatibility
            }

            defaultConfig {
                applicationId = packageName
                minSdk = C.android.minSdk
                targetSdk = C.android.targetSdk
                versionCode = packageVersion
                versionName = packageVersionName

                ndk {
                    for (abi in C.android.ndkAbi) abiFilters += abi
                }
            }

            val androidSigningConfig = sign()

            buildTypes {
                debug {
                    isMinifyEnabled = false
                    isShrinkResources = false
                    isDebuggable = true
                    signingConfig = androidSigningConfig
                }

                release {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    isDebuggable = false

                    val proguardDir = androidProguardAndroidDir.asFile
                    val proguardConfigs = mutableListOf<Any>(getDefaultProguardFile(C.proguard.defaultRule))
                    if (proguardDir.isDirectory) proguardConfigs.addAll(proguardDir.listFiles { it.extension == "pro" })
                    proguardFiles(*proguardConfigs.toTypedArray())

                    signingConfig = androidSigningConfig
                }
            }

            packaging {
                resources {
                    excludes += C.excludes
                }

                dex {
                    useLegacyPackaging = true
                }

                jniLibs {
                    useLegacyPackaging = true
                }
            }

            android()
        }

        afterEvaluate {
            actions()
        }
    }
}