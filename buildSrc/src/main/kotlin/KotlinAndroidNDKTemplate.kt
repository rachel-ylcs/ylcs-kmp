import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

abstract class KotlinAndroidNDKTemplate : KotlinTemplate<KotlinAndroidExtension>() {
    open fun KotlinAndroidSourceSetsScope.source() { }

    open fun LibraryExtension.android() { }

    final override fun Project.build(extension: KotlinAndroidExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
                jvmTarget.set(C.jvm.androidTarget)
            }

            action()
        }

        KotlinAndroidSourceSetsScope(dependencies).source()

        extensions.configure<LibraryExtension> {
            namespace = uniqueSafeModuleName
            compileSdk = C.android.compileSdk

            compileOptions {
                sourceCompatibility = C.jvm.compatibility
                targetCompatibility = C.jvm.compatibility
            }

            defaultConfig {
                minSdk = C.android.minSdk
                lint.targetSdk = C.android.targetSdk

                val proguardDir = androidProguardAndroidDir.asFile
                val proguardConfigs = mutableListOf<Any>()
                if (proguardDir.isDirectory) proguardConfigs.addAll(proguardDir.listFiles { it.extension == "pro" })
                consumerProguardFiles(*proguardConfigs.toTypedArray())

                ndk {
                    for (abi in C.android.ndkAbi) abiFilters += abi
                }
            }

            ndkVersion = C.android.ndkVersion

            externalNativeBuild {
                cmake {
                    path = file("src/main/cpp/CMakeLists.txt")
                }
            }

            android()
        }

        afterEvaluate {
            actions()
        }
    }
}