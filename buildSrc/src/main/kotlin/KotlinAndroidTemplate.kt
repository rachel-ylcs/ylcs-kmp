import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class KotlinAndroidSourceSetsScope(
    p: Project,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(p, set) {
    val main: KotlinSourceSet by lazy { set.named("main").get() }
    val test: KotlinSourceSet by lazy { set.named("test").get() }
}

abstract class KotlinAndroidTemplate : KotlinTemplate<KotlinAndroidExtension>() {
    open fun KotlinAndroidSourceSetsScope.source() { }

    open val packageName: String get() = uniqueSafeModuleName
    abstract val packageVersion: Int
    abstract val packageVersionName: String
    open fun BaseAppModuleExtension.sign(): SigningConfig? = null
    open fun BaseAppModuleExtension.android() { }

    final override fun Project.build(extension: KotlinAndroidExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
                jvmTarget.set(C.jvm.androidTarget)
            }

            KotlinAndroidSourceSetsScope(this@build, sourceSets).source()

            action()
        }

        extensions.configure<BaseAppModuleExtension> {
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
                    proguardFiles(
                        getDefaultProguardFile(C.proguard.defaultRule),
                        //C.root.shared.commonR8Rule, C.root.shared.androidR8Rule
                    )
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