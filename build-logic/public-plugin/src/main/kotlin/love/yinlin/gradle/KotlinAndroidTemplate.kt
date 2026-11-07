package love.yinlin.gradle

import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.File
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
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

open class KotlinAndroidTemplate : KotlinTemplate<KotlinAndroidExtension>() {
    private var sourceBlock: KotlinAndroidSourceSetsScope.() -> Unit = {}
    fun sources(block: KotlinAndroidSourceSetsScope.() -> Unit) {
        val previous = sourceBlock
        sourceBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinAndroidSourceSetsScope.source() { sourceBlock(this) }

    var packageName: String = ""
    var packageVersion: Int = 1
    var packageVersionName: String = "1.0"
    private var signBlock: ApplicationExtension.() -> ApkSigningConfig? = { null }
    fun sign(block: ApplicationExtension.() -> ApkSigningConfig?) { signBlock = block }
    internal fun ApplicationExtension.sign(): ApkSigningConfig? = signBlock(this)
    private var androidBlock: ApplicationExtension.() -> Unit = {}
    fun android(block: ApplicationExtension.() -> Unit) {
        val previous = androidBlock
        androidBlock = {
            previous()
            block()
        }
    }
    internal fun ApplicationExtension.android() { androidBlock(this) }

    private val releaseApkDirectories = mutableMapOf<String, Provider<Directory>>()

    val androidReleaseApk: Provider<RegularFile>
        get() {
            val directory = releaseApkDirectories["release"] ?: releaseApkDirectories.values.singleOrNull()
                ?: error("Expected one Release APK variant for ${owner.path}, found ${releaseApkDirectories.keys}")
            val projectName = owner.name
            return directory.map { it.file(selectReleaseApk(it.asFile, projectName).name) }
        }

    val Project.originOutput: File get() =
        selectReleaseApk(layout.buildDirectory.get().dir("outputs/apk/release").asFile, name)

    final override fun Project.build(extension: KotlinAndroidExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
                jvmTarget.set(JvmTarget.fromTarget(buildDefaults.jvm.androidTarget))
            }

            action()
        }

        KotlinAndroidSourceSetsScope(dependencies).source()

        extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
            onVariants(selector().withBuildType("release")) { variant ->
                releaseApkDirectories[variant.name] = variant.artifacts.get(SingleArtifact.APK)
            }
        }

        extensions.configure<ApplicationExtension> {
            namespace = packageName.ifEmpty { uniqueSafeModuleName }
            compileSdk {
                val (major, minor) = buildDefaults.android.compileSdk
                version = release(major) {
                    minorApiLevel = minor
                }
            }

            compileOptions {
                sourceCompatibility = buildDefaults.jvm.androidCompatibility
                targetCompatibility = buildDefaults.jvm.androidCompatibility
            }

            defaultConfig {
                applicationId = packageName.ifEmpty { uniqueSafeModuleName }
                minSdk = buildDefaults.android.minSdk
                targetSdk = buildDefaults.android.targetSdk
                versionCode = packageVersion
                versionName = packageVersionName

                ndk {
                    for (abi in buildDefaults.android.ndkAbi) abiFilters += abi
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
                    val proguardConfigs = mutableListOf<Any>(getDefaultProguardFile(buildDefaults.android.proguardRule))
                    if (proguardDir.isDirectory) proguardConfigs.addAll(proguardDir.listFiles { it.extension == "pro" } ?: emptyArray())
                    proguardFiles(*proguardConfigs.toTypedArray())

                    signingConfig = androidSigningConfig
                }
            }

            packaging {
                resources {
                    excludes += buildDefaults.resourceExcludes
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

        configureActions()
    }
}

private fun selectReleaseApk(outputDir: File, projectName: String): File {
    val outputList = outputDir.listFiles { file ->
        file.isFile && file.extension.equals("apk", true)
    }?.sortedBy { it.name }.orEmpty()
    return when {
        outputList.size == 1 -> outputList.first()
        else -> outputList.firstOrNull { it.name == "$projectName-release.apk" }
            ?: outputList.firstOrNull()
            ?: outputDir.resolve("$projectName-release.apk")
    }
}
