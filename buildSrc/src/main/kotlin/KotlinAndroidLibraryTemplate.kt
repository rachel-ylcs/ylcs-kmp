import com.android.build.gradle.LibraryExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.collections.plusAssign

class KotlinAndroidLibrarySourceSetsScope(
    p: Project,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(p, set) {
    val main: KotlinSourceSet by lazy { set.named("main").get() }
    val test: KotlinSourceSet by lazy { set.named("test").get() }
}

abstract class KotlinAndroidLibraryTemplate : KotlinTemplate<KotlinAndroidExtension>() {
    open fun KotlinAndroidLibrarySourceSetsScope.source() { }

    open fun LibraryExtension.android() { }

    final override fun Project.build(extension: KotlinAndroidExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
                jvmTarget.set(C.jvm.androidTarget)
            }

            KotlinAndroidLibrarySourceSetsScope(this@build, sourceSets).source()

            action()
        }

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

                ndk {
                    for (abi in C.android.ndkAbi) abiFilters += abi
                }

                val proguardDir = androidProguardAndroidDir.asFile
                val proguardConfigs = mutableListOf<Any>()
                if (proguardDir.isDirectory) proguardConfigs.addAll(proguardDir.listFiles { it.extension == "pro" })
                consumerProguardFiles(*proguardConfigs.toTypedArray())
            }

            android()
        }

        afterEvaluate {
            actions()
        }
    }
}