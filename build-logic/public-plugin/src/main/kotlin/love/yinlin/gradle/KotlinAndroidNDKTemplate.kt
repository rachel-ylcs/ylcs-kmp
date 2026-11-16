package love.yinlin.gradle

import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

open class KotlinAndroidNDKTemplate : KotlinTemplate<KotlinAndroidExtension>() {
    private var sourceBlock: KotlinAndroidSourceSetsScope.() -> Unit = {}
    fun sources(block: KotlinAndroidSourceSetsScope.() -> Unit) {
        val previous = sourceBlock
        sourceBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinAndroidSourceSetsScope.source() { sourceBlock(this) }

    private var androidBlock: LibraryExtension.() -> Unit = {}
    fun android(block: LibraryExtension.() -> Unit) {
        val previous = androidBlock
        androidBlock = {
            previous()
            block()
        }
    }
    internal fun LibraryExtension.android() { androidBlock(this) }

    final override fun Project.build(extension: KotlinAndroidExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
                jvmTarget.set(JvmTarget.fromTarget(buildDefaults.jvm.androidTarget))
            }

            action()
        }

        KotlinAndroidSourceSetsScope(dependencies).source()

        extensions.configure<LibraryExtension> {
            namespace = uniqueSafeModuleName
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
                minSdk = buildDefaults.android.minSdk
                lint.targetSdk = buildDefaults.android.targetSdk

                val proguardDir = androidProguardAndroidDir.asFile
                val proguardConfigs = mutableListOf<Any>()
                if (proguardDir.isDirectory) proguardConfigs.addAll(proguardDir.listFiles { it.extension == "pro" } ?: emptyArray())
                consumerProguardFiles(*proguardConfigs.toTypedArray())

                ndk {
                    for (abi in buildDefaults.android.ndkAbi) abiFilters += abi
                }
            }

            ndkVersion = buildDefaults.android.ndkVersion

            externalNativeBuild {
                cmake {
                    path = file("src/main/cpp/CMakeLists.txt")
                }
            }

            android()
        }

        configureActions()
    }
}
