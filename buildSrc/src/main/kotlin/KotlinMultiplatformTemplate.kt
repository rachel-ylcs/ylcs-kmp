import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationDistributions
import org.jetbrains.compose.desktop.application.dsl.JvmMacOSPlatformSettings
import org.jetbrains.compose.desktop.application.dsl.LinuxPlatformSettings
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.dsl.WindowsPlatformSettings
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.io.File

class KotlinMultiplatformSourceSetsScope(
    private val p: Project,
    private val extension: KotlinMultiplatformExtension,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(set) {
    val commonMain: KotlinSourceSet by lazy { with(extension) { set.commonMain.get() } }
    val commonTest: KotlinSourceSet by lazy { with(extension) { set.commonTest.get() } }
    val nativeMain: KotlinSourceSet by lazy { with(extension) { set.nativeMain.get() } }
    val androidMain: KotlinSourceSet by lazy { with(extension) { set.androidMain.get() } }
    val androidNativeMain: KotlinSourceSet by lazy { with(extension) { set.androidNativeArm64Main.get() } }
    val appleMain: KotlinSourceSet by lazy { with(extension) { set.appleMain.get() } }
    val iosMain: KotlinSourceSet by lazy { with(extension) { set.iosMain.get() } }
    val iosMainList: List<KotlinSourceSet> by lazy {
        buildList {
            with(extension) {
                add(set.iosArm64Main.get())
                if (p.C.platform == BuildPlatform.Mac) {
                    when (p.C.architecture) {
                        BuildArchitecture.AARCH64 -> add(set.iosSimulatorArm64Main.get())
                        BuildArchitecture.X86_64 -> add(set.iosX64Main.get())
                        else -> {}
                    }
                }
            }
        }
    }
    val desktopMain: KotlinSourceSet by lazy { set.getByName("desktopMain") }
    val wasmJsMain: KotlinSourceSet by lazy { with(extension) { set.wasmJsMain.get() } }
    val windowsMain: KotlinSourceSet by lazy { set.getByName("windowsMain") }
    val windowsTest: KotlinSourceSet by lazy { set.getByName("windowsTest") }
    val linuxMain: KotlinSourceSet by lazy { set.getByName("linuxMain") }
    val linuxTest: KotlinSourceSet by lazy { set.getByName("linuxTest") }
    val macosMain: KotlinSourceSet by lazy { set.getByName("macosMain") }
    val macosTest: KotlinSourceSet by lazy { set.getByName("macosTest") }

    val composeOSLib: String get() = extension.extensions.getByType<ComposePlugin.Dependencies>().desktop.currentOs
}

data class Pod(
    val name: String,
    val moduleName: String? = null,
    val version: Provider<String>? = null,
    val extraOpts: List<String> = listOf("-compiler-option", "-fmodules"),
    val source: File? = null,
)

abstract class KotlinMultiplatformTemplate : KotlinTemplate<KotlinMultiplatformExtension>() {
    // SourceSets
    open fun KotlinMultiplatformSourceSetsScope.source() { }

    // Compose
    open val resourceName: String? = null
    open fun ComposeCompilerGradlePluginExtension.composeCompiler() { }

    // Android
    open val namespace: String? = null
    open fun KotlinMultiplatformAndroidLibraryTarget.android() { }
    open fun LibraryExtension.android() { }
    open val buildNDK: Boolean = false

    // iOS
    open val iosTarget: Boolean = true
    open fun KotlinNativeTarget.ios() { }
    open val cocoapodsList: List<Pod> = emptyList()
    open fun CocoapodsExtension.cocoapods() { }

    // Desktop
    open val desktopTarget: Boolean = true
    open fun KotlinJvmTarget.desktop() { }
    open val desktopPackageName: String? = null
    open val desktopMainClass: String? = null
    open val desktopJvmArgs: List<String> = emptyList()
    open val desktopModules: List<String> = emptyList()
    open val windowsDistributions: (WindowsPlatformSettings.() -> Unit)? = null
    open val linuxDistributions: (LinuxPlatformSettings.() -> Unit)? = null
    open val macOSDistributions: (JvmMacOSPlatformSettings.() -> Unit)? = null
    open fun JvmApplicationDistributions.desktopPackage() { }

    // Web
    open val webTarget: Boolean = true
    open fun KotlinWasmJsTargetDsl.wasmJs() { }
    open fun KotlinWebpackConfig.webpack() { }

    // DesktopNative
    open val windowsTarget: Boolean = false
    open val linuxTarget: Boolean = false
    open val macosTarget: Boolean = false
    open fun KotlinNativeTarget.desktopNative() { }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        with(extension) {
            // 开启 kotlin 编译器特性
            compilerOptions {
                freeCompilerArgs.addAll(C.features)
            }

            // Android
            val oldAndroidPlugin = this@build.extensions.findByType<LibraryExtension>()
            val newAndroidPlugin = extensions.findByType<KotlinMultiplatformAndroidLibraryTarget>()
            if (oldAndroidPlugin != null || newAndroidPlugin != null) {
                val androidNamespace = this@KotlinMultiplatformTemplate.namespace?.let { "${C.app.packageName}.$it" } ?: C.app.packageName

                oldAndroidPlugin?.apply {
                    @Suppress("Deprecation")
                    androidTarget {
                        compilerOptions {
                            jvmTarget.set(C.jvm.androidTarget)
                        }
                    }

                    this.namespace = androidNamespace
                    compileSdk = C.android.compileSdk

                    defaultConfig {
                        minSdk = C.android.minSdk
                        lint.targetSdk = C.android.targetSdk

                        ndk {
                            for (abi in C.android.ndkAbi) abiFilters += abi
                        }
                    }

                    if (buildNDK) {
                        ndkVersion = C.android.ndkVersion

                        externalNativeBuild {
                            cmake {
                                path = file("src/androidMain/cpp/CMakeLists.txt")
                            }
                        }
                    }

                    android()
                }

                newAndroidPlugin?.apply {
                    this.namespace = androidNamespace
                    compileSdk = C.android.compileSdk
                    minSdk = C.android.minSdk
                    lint.targetSdk = C.android.targetSdk

                    compilations.configureEach {
                        compileTaskProvider.configure {
                            compilerOptions {
                                jvmTarget.set(C.jvm.androidTarget)
                            }
                        }
                    }

                    if (resourceName != null) {
                        @Suppress("UnstableApiUsage")
                        androidResources.enable = true
                    }

                    android()
                }
            }

            // iOS
            if (iosTarget) {
                iosArm64 { ios() }
                if (C.platform == BuildPlatform.Mac) {
                    when (C.architecture) {
                        BuildArchitecture.AARCH64 -> iosSimulatorArm64 { ios() }
                        BuildArchitecture.X86_64 -> iosX64 { ios() }
                        else -> { }
                    }
                }

                // Use Cocoapods
                extensions.findByType<CocoapodsExtension>()?.apply {
                    version = C.app.versionName
                    ios.deploymentTarget = C.ios.target
                    podfile = C.root.iosApp.podfile.asFile

                    xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
                    xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE

                    cocoapods()

                    if (C.platform == BuildPlatform.Mac) {
                        for (item in cocoapodsList) {
                            pod(item.name) {
                                if (item.moduleName != null) moduleName = item.moduleName
                                if (item.version != null) version = item.version.get()
                                extraOpts += item.extraOpts
                                if (item.source != null) source = path(item.source)
                            }
                        }
                    }
                }
            }

            // Desktop
            if (desktopTarget) {
                jvm("desktop") {
                    compilerOptions {
                        jvmTarget.set(C.jvm.target)
                    }

                    desktop()
                }
            }

            // Web
            if (webTarget) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser {
                        testTask {
                            enabled = false
                        }
                        commonWebpackConfig {
                            cssSupport {
                                enabled.set(true)
                            }

                            webpack()
                        }
                    }
                    binaries.executable()

                    wasmJs()
                }
            }

            // DesktopNative
            if (windowsTarget) mingwX64("windows") { desktopNative() }
            if (linuxTarget) linuxX64("linux") { desktopNative() }
            if (macosTarget) macosArm64("macos") { desktopNative() }

            // SourceSet
            KotlinMultiplatformSourceSetsScope(this@build, extension, sourceSets).source()

            action()
        }

        extensions.findByType<ComposeCompilerGradlePluginExtension>()?.apply {
            composeCompiler()
        }

        resourceName?.let { name ->
            extensions.configure<ComposeExtension> {
                this.configure<ResourcesExtension> {
                    publicResClass = true
                    packageOfResClass = "${C.app.packageName}.$name.resources"
                }
            }
        }

        if (windowsDistributions != null || linuxDistributions != null || macOSDistributions != null) {
            extensions.configure<ComposeExtension> {
                this.configure<DesktopExtension> {
                    application {
                        mainClass = desktopMainClass

                        jvmArgs += desktopJvmArgs
                        jvmArgs += "--enable-native-access=ALL-UNNAMED"

                        buildTypes.release.proguard {
                            version.set(C.proguard.version)
                            isEnabled.set(true)
                            optimize.set(true)
                            obfuscate.set(true)
                            joinOutputJars.set(true)
                            configurationFiles.from(C.root.shared.commonR8Rule, C.root.shared.desktopR8Rule)
                        }

                        nativeDistributions {
                            packageName = desktopPackageName
                            packageVersion = C.app.versionName
                            description = C.app.description
                            copyright = C.app.copyright
                            vendor = C.app.vendor
                            licenseFile.set(C.root.license)

                            modules(*desktopModules.toTypedArray())

                            val targetList = mutableListOf<TargetFormat>()

                            windowsDistributions?.let { settings ->
                                targetList += TargetFormat.Exe
                                windows {
                                    console = false
                                    exePackageVersion = C.app.versionName
                                    settings()
                                }
                            }

                            linuxDistributions?.let { settings ->
                                targetList += TargetFormat.Deb
                                linux {
                                    debPackageVersion = C.app.versionName
                                    settings()
                                }
                            }

                            macOSDistributions?.let { settings ->
                                targetList += TargetFormat.Pkg
                                macOS {
                                    pkgPackageVersion = C.app.versionName
                                    settings()
                                }
                            }

                            targetFormats(*targetList.toTypedArray())

                            desktopPackage()
                        }
                    }
                }
            }
        }

        afterEvaluate {
            actions()
        }
    }
}