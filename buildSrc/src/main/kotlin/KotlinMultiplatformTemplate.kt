import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import love.yinlin.task.BuildDesktopNativeTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
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
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.io.File

class Pod internal constructor(
    val name: String,
    val version: String? = null,
    val moduleName: String? = null,
    val extraOpts: List<String> = listOf("-compiler-option", "-fmodules"),
    val source: File? = null,
)

class KotlinMultiplatformSourceSetsScope(
    p: Project,
    private val extension: KotlinMultiplatformExtension,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(p, set) {
    val commonMain: KotlinSourceSet by lazy { with(extension) { set.commonMain.get() } }
    val commonTest: KotlinSourceSet by lazy { with(extension) { set.commonTest.get() } }
    val nativeMain: KotlinSourceSet by lazy { with(extension) { set.nativeMain.get() } }
    val androidMain: KotlinSourceSet by lazy { with(extension) { set.androidMain.get() } }
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
    val webMain: KotlinSourceSet by lazy { with(extension) { set.webMain.get() } }
    val jsMain: KotlinSourceSet by lazy { with(extension) { set.jsMain.get() } }
    val wasmJsMain: KotlinSourceSet by lazy { with(extension) { set.wasmJsMain.get() } }

    val androidNativeMain: KotlinSourceSet by lazy { set.getByName("androidNativeMain") }
    val androidNativeTest: KotlinSourceSet by lazy { set.getByName("androidNativeTest") }
    val windowsMain: KotlinSourceSet by lazy { set.getByName("windowsMain") }
    val windowsTest: KotlinSourceSet by lazy { set.getByName("windowsTest") }
    val linuxMain: KotlinSourceSet by lazy { set.getByName("linuxMain") }
    val linuxTest: KotlinSourceSet by lazy { set.getByName("linuxTest") }
    val macosMain: KotlinSourceSet by lazy { set.getByName("macosMain") }
    val macosTest: KotlinSourceSet by lazy { set.getByName("macosTest") }

    val composeOSLib: String get() = extension.extensions.getByType<ComposePlugin.Dependencies>().desktop.currentOs
}

abstract class KotlinMultiplatformTemplate : KotlinTemplate<KotlinMultiplatformExtension>() {
    // SourceSets
    open fun KotlinMultiplatformSourceSetsScope.source() { }

    // Compose
    open val exportResource: Boolean = false
    open fun ComposeCompilerGradlePluginExtension.composeCompiler() { }

    // Android
    open fun KotlinMultiplatformAndroidLibraryTarget.android() { }
    open fun LibraryExtension.android() { }
    open val buildNDK: Boolean = false

    // iOS
    open val iosTarget: Boolean = true
    open fun KotlinNativeTarget.ios() { }
    open val cocoapodsList: List<Pod> = emptyList()
    open fun CocoapodsExtension.cocoapods() { }

    fun pod(
        name: String,
        version: String? = null,
        moduleName: String? = null,
        extraOpts: List<String> = listOf("-compiler-option", "-fmodules"),
        source: File? = null,
    ): Pod = Pod(name, version, moduleName, extraOpts, source)

    fun pod(
        name: String,
        version: Provider<String>,
        moduleName: String? = null,
        extraOpts: List<String> = listOf("-compiler-option", "-fmodules"),
        source: File? = null,
    ): Pod = Pod(name, version.get(), moduleName, extraOpts, source)

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
    open fun KotlinJsTargetDsl.js() { }
    open fun KotlinWebpackConfig.webpack() { }

    // DesktopNative
    open val androidNativeTarget: Boolean = false
    open val windowsTarget: Boolean = false
    open val linuxTarget: Boolean = false
    open val macosTarget: Boolean = false
    open fun KotlinNativeTarget.native() { }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
            }

            // Android
            val oldAndroidPlugin = this@build.extensions.findByType<LibraryExtension>()
            val newAndroidPlugin = extensions.findByType<KotlinMultiplatformAndroidLibraryTarget>()
            if (oldAndroidPlugin != null || newAndroidPlugin != null) {
                oldAndroidPlugin?.apply {
                    @Suppress("Deprecation")
                    androidTarget {
                        compilerOptions {
                            jvmTarget.set(C.jvm.androidTarget)
                        }
                    }

                    namespace = uniqueSafeModuleName
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
                    namespace = uniqueSafeModuleName
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

                    if (exportResource) {
                        @Suppress("UnstableApiUsage")
                        androidResources.enable = true
                    }

                    android()
                }
            }

            // iOS
            if (iosTarget) {
                buildList {
                    add(iosArm64())
                    if (C.platform == BuildPlatform.Mac) {
                        when (C.architecture) {
                            BuildArchitecture.AARCH64 -> add(iosSimulatorArm64())
                            // https://blog.jetbrains.com/kotlin/2023/02/update-regarding-kotlin-native-targets/
                            BuildArchitecture.X86_64 -> @Suppress("Deprecation") add(iosX64())
                            else -> { }
                        }
                    }
                }.forEach { target ->
                    target.ios()

                    // https://youtrack.jetbrains.com/issue/KT-80715
                    if (C.platform == BuildPlatform.Mac) {
                        target.binaries.framework {
                            @OptIn(KotlinNativeCacheApi::class)
                            disableNativeCache(
                                version = DisableCacheInKotlinVersion.`2_3_20`,
                                reason = "cache bug",
                                issueUrl = java.net.URI("https://youtrack.jetbrains.com/issue/KT-80715")
                            )
                        }
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
                                if (item.version != null) version = item.version
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
                    compilerOptions {
                        target.set("es2015")
                        useLanguageFeature(
                            "-Xes-long-as-bigint"
                        )
                    }

                    browser {
                        commonWebpackConfig {
                            cssSupport {
                                enabled.set(true)
                            }

                            webpack()
                        }
                    }
                    binaries.executable()

                    js()
                    wasmJs()
                }

                js {
                    compilerOptions {
                        target.set("es2015")
                        useLanguageFeature(
                            "-Xes-long-as-bigint"
                        )
                    }

                    browser {
                        commonWebpackConfig {
                            cssSupport {
                                enabled.set(true)
                            }

                            webpack()
                        }
                    }
                    binaries.executable()

                    js()
                }
            }

            // DesktopNative
            buildList {
                if (androidNativeTarget) add(androidNativeArm64("androidNative"))
                if (windowsTarget) add(mingwX64("windows"))
                if (linuxTarget) add(linuxX64("linux"))
                if (macosTarget) add(macosArm64("macos"))
            }.forEach { target ->
                target.native()
            }

            // SourceSet
            KotlinMultiplatformSourceSetsScope(this@build, extension, sourceSets).source()

            action()
        }

        extensions.findByType<ComposeCompilerGradlePluginExtension>()?.apply {
            composeCompiler()
        }

        if (exportResource) {
            extensions.configure<ComposeExtension> {
                this.configure<ResourcesExtension> {
                    publicResClass = true
                    packageOfResClass = "$uniqueSafeModuleName.resources"
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
                            isEnabled.set(false)
                            optimize.set(false)
                            obfuscate.set(false)
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

                            appResourcesRootDir.set(project.packageResourcesDir)

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

            // 检查是否需要编译 Desktop Native
            val sourceDir = layout.projectDirectory.asFile.resolve("src/desktopMain/cpp")
            if (sourceDir.exists()) {
                val buildNativeTask = tasks.register("buildDesktopNative", BuildDesktopNativeTask::class) {
                    inputDir.set(sourceDir)
                }

                tasks.named("desktopJar") {
                    dependsOn(buildNativeTask)
                }
            }
        }
    }
}