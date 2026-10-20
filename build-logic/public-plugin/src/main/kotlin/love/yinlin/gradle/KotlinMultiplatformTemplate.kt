package love.yinlin.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.dsl.AotMode
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMImportExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.io.File

class KotlinMultiplatformSourceSetsScope(
    p: Project,
    private val extension: KotlinMultiplatformExtension,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(p, set) {
    val commonMain: KotlinSourceSet by lazy { with(extension) { set.commonMain.get() } }
    val commonTest: KotlinSourceSet by lazy { with(extension) { set.commonTest.get() } }
    val nativeMain: KotlinSourceSet by lazy { with(extension) { set.nativeMain.get() } }
    val nativeTest: KotlinSourceSet by lazy { with(extension) { set.nativeTest.get() } }
    val androidMain: KotlinSourceSet by lazy { with(extension) { set.androidMain.get() } }
    val androidTest: KotlinSourceSet by lazy { with(extension) { set.androidUnitTest.get() } }
    val appleMain: KotlinSourceSet by lazy { with(extension) { set.appleMain.get() } }
    val appleTest: KotlinSourceSet by lazy { with(extension) { set.appleTest.get() } }
    val iosMain: KotlinSourceSet by lazy { with(extension) { set.iosMain.get() } }
    val iosTest: KotlinSourceSet by lazy { with(extension) { set.iosTest.get() } }
    val iosMainList: List<KotlinSourceSet> by lazy {
        buildList {
            with(extension) {
                add(set.iosArm64Main.get())
                if (p.buildDefaults.platform == BuildPlatform.Mac) {
                    when (p.buildDefaults.architecture) {
                        BuildArchitecture.AARCH64 -> add(set.iosSimulatorArm64Main.get())
                        else -> {}
                    }
                }
            }
        }
    }
    val desktopMain: KotlinSourceSet by lazy { set.getByName("desktopMain") }
    val desktopTest: KotlinSourceSet by lazy { set.getByName("desktopTest") }
    val webMain: KotlinSourceSet by lazy { with(extension) { set.webMain.get() } }
    val webTest: KotlinSourceSet by lazy { with(extension) { set.webTest.get() } }
    val jsMain: KotlinSourceSet by lazy { with(extension) { set.jsMain.get() } }
    val jsTest: KotlinSourceSet by lazy { with(extension) { set.jsTest.get()} }
    val wasmJsMain: KotlinSourceSet by lazy { with(extension) { set.wasmJsMain.get() } }
    val wasmJsTest: KotlinSourceSet by lazy { with(extension) { set.wasmJsTest.get() } }

    val windowsMain: KotlinSourceSet by lazy { set.getByName("windowsMain") }
    val windowsTest: KotlinSourceSet by lazy { set.getByName("windowsTest") }
    val linuxMain: KotlinSourceSet by lazy { set.getByName("linuxMain") }
    val linuxTest: KotlinSourceSet by lazy { set.getByName("linuxTest") }
    val macosMain: KotlinSourceSet by lazy { set.getByName("macosMain") }
    val macosTest: KotlinSourceSet by lazy { set.getByName("macosTest") }

    val skikoMain: KotlinSourceSet by lazy { set.maybeCreate("skikoMain") }
    val jvmMain: KotlinSourceSet by lazy { set.maybeCreate("jvmMain") }
    val clientMain: KotlinSourceSet by lazy { set.maybeCreate("clientMain") }
    val posixMain: KotlinSourceSet by lazy { set.maybeCreate("posixMain") }
}

// swift 依赖包结构
sealed interface SwiftPackage {
    data class Remote(
        val url: String,
        val version: String,
        val products: List<String>
    ) : SwiftPackage

    data class Local(
        val name: String,
        val importedClangModules: List<String> = emptyList()
    ) : SwiftPackage
}

open class KotlinMultiplatformTemplate : KotlinTemplate<KotlinMultiplatformExtension>() {
    // SourceSets
    private var sourceBlock: KotlinMultiplatformSourceSetsScope.() -> Unit = {}
    fun sources(block: KotlinMultiplatformSourceSetsScope.() -> Unit) {
        val previous = sourceBlock
        sourceBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinMultiplatformSourceSetsScope.source() { sourceBlock(this) }

    // Android
    private var androidBlock: KotlinMultiplatformAndroidLibraryTarget.() -> Unit = {}
    fun android(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
        val previous = androidBlock
        androidBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinMultiplatformAndroidLibraryTarget.android() { androidBlock(this) }

    // iOS
    var iosTarget: Boolean = true
    private var iosBlock: KotlinNativeTarget.() -> Unit = {}
    fun ios(block: KotlinNativeTarget.() -> Unit) {
        val previous = iosBlock
        iosBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.ios() { iosBlock(this) }
    var iosFrameworkBaseName: String? = null
    var iosFrameworkIsStatic: Boolean? = null
    var swiftPackages: List<SwiftPackage> = emptyList()
    var swiftDiscoverClangModulesImplicitly: Boolean = true  // 是否自动发现 SwiftPM 的 Clang 模块；关闭后按声明的模块列表导入

    // Desktop
    var desktopTarget: Boolean = true
    private var desktopBlock: KotlinJvmTarget.() -> Unit = {}
    fun desktop(block: KotlinJvmTarget.() -> Unit) {
        val previous = desktopBlock
        desktopBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinJvmTarget.desktop() { desktopBlock(this) }
    var desktopPackage: DesktopPackage? = null
    fun desktopPackage(block: DesktopPackage.() -> Unit) {
        desktopPackage = DesktopPackage().apply(block)
    }

    // Web
    var webTarget: Boolean = true
    var webTestHeadless: Boolean = true
    private var wasmJsBlock: KotlinWasmJsTargetDsl.() -> Unit = {}
    fun wasmJs(block: KotlinWasmJsTargetDsl.() -> Unit) {
        val previous = wasmJsBlock
        wasmJsBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinWasmJsTargetDsl.wasmJs() { wasmJsBlock(this) }
    private var jsBlock: KotlinJsTargetDsl.() -> Unit = {}
    fun js(block: KotlinJsTargetDsl.() -> Unit) {
        val previous = jsBlock
        jsBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinJsTargetDsl.js() { jsBlock(this) }
    private var webpackBlock: KotlinWebpackConfig.() -> Unit = {}
    fun webpack(block: KotlinWebpackConfig.() -> Unit) {
        val previous = webpackBlock
        webpackBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinWebpackConfig.webpack() { webpackBlock(this) }

    // DesktopNative
    var windowsTarget: Boolean = false
    var linuxTarget: Boolean = false
    var macosTarget: Boolean = false
    private var windowsBlock: KotlinNativeTarget.() -> Unit = {}
    fun windows(block: KotlinNativeTarget.() -> Unit) {
        val previous = windowsBlock
        windowsBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.windows() { windowsBlock(this) }
    private var linuxBlock: KotlinNativeTarget.() -> Unit = {}
    fun linux(block: KotlinNativeTarget.() -> Unit) {
        val previous = linuxBlock
        linuxBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.linux() { linuxBlock(this) }
    private var macosBlock: KotlinNativeTarget.() -> Unit = {}
    fun macos(block: KotlinNativeTarget.() -> Unit) {
        val previous = macosBlock
        macosBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.macos() { macosBlock(this) }
    private var nativeBlock: KotlinNativeTarget.() -> Unit = {}
    fun native(block: KotlinNativeTarget.() -> Unit) {
        val previous = nativeBlock
        nativeBlock = {
            previous()
            block()
        }
    }
    internal fun KotlinNativeTarget.native() { nativeBlock(this) }

    final override fun Project.build(extension: KotlinMultiplatformExtension) {
        val exportResources = hasKMPResources

        with(extension) {
            compilerOptions {
                useLanguageFeature()
            }

            // Android
            extensions.findByType<KotlinMultiplatformAndroidLibraryTarget>()?.apply {
                namespace = uniqueSafeModuleName
                compileSdk {
                    val (major, minor) = buildDefaults.android.compileSdk
                    version = release(major) {
                        minorApiLevel = minor
                    }
                }
                minSdk = buildDefaults.android.minSdk
                lint.targetSdk = buildDefaults.android.targetSdk

                compilations.configureEach {
                    compileTaskProvider.configure {
                        compilerOptions {
                            jvmTarget.set(JvmTarget.fromTarget(buildDefaults.jvm.androidTarget))
                        }
                    }
                }

                withHostTest {

                }

                @Suppress("UnstableApiUsage")
                optimization {
                    val proguardDir = androidProguardKMPDir.asFile
                    if (proguardDir.isDirectory) {
                        val proguardFiles = proguardDir.listFiles { it.extension == "pro" }
                        consumerKeepRules.apply {
                            publish = true
                            files(*proguardFiles)
                        }
                    }
                }

                if (exportResources) androidResources.enable = true

                android()
            }

            // iOS
            if (iosTarget) {
                buildList {
                    add(iosArm64())
                    if (buildDefaults.platform == BuildPlatform.Mac) {
                        when (buildDefaults.architecture) {
                            // cmp 自 1.11 起已经停止对 iosX64 的支持
                            // BuildArchitecture.X86_64 -> add(iosX64())
                            BuildArchitecture.AARCH64 -> add(iosSimulatorArm64())
                            else -> { }
                        }
                    }
                }.forEach { target ->
                    target.ios()

                    // https://youtrack.jetbrains.com/issue/KT-80715
                    if (buildDefaults.platform == BuildPlatform.Mac) {
                        target.binaries.framework {
                            iosFrameworkBaseName?.let { baseName = it }
                            iosFrameworkIsStatic?.let { isStatic = it }
                            if (buildDefaults.ios.linkUIKit) linkerOpts("-framework", "UIKit")
                            @OptIn(KotlinNativeCacheApi::class)
                            disableNativeCache(
                                version = DisableCacheInKotlinVersion.`2_5_0`,
                                reason = "cache bug",
                                issueUrl = java.net.URI("https://youtrack.jetbrains.com/issue/KT-80715")
                            )
                        }
                    }
                }

                if (buildDefaults.platform == BuildPlatform.Mac) {
                    extensions.findByType<SwiftPMImportExtension>()?.apply {
                        iosMinimumDeploymentTarget.set(buildDefaults.ios.target)
                        discoverClangModulesImplicitly.set(swiftDiscoverClangModulesImplicitly)

                        val localSPMDir = iosLocalSPMDir

                        @OptIn(ExperimentalKotlinGradlePluginApi::class)
                        for (pkg in swiftPackages) {
                            when (pkg) {
                                is SwiftPackage.Remote -> swiftPackage(pkg.url, pkg.version, pkg.products)
                                is SwiftPackage.Local -> localSwiftPackage(
                                    directory = localSPMDir.dir(pkg.name),
                                    products = listOf(pkg.name),
                                    importedClangModules = pkg.importedClangModules,
                                )
                            }
                        }
                    }
                }
            }

            // Desktop
            if (desktopTarget) {
                val javaTarget = buildDefaults.jvm.target
                jvm("desktop") {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(buildDefaults.jvm.target))
                    }

                    compilations.configureEach {
                        compileJavaTaskProvider?.configure {
                            sourceCompatibility = javaTarget
                            targetCompatibility = javaTarget
                        }
                    }

                    desktop()
                }
            }

            // Web
            if (webTarget) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    compilerOptions {
                        target.set(buildDefaults.web.jsTarget)
                        useLanguageFeature()
                    }

                    browser {
                        testTask {
                            useKarma {
                                if (webTestHeadless) useChromeHeadless()
                                else useChrome()
                            }
                        }

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
                        target.set(buildDefaults.web.jsTarget)
                        useLanguageFeature()
                    }

                    browser {
                        testTask {
                            useKarma {
                                if (webTestHeadless) useChromeHeadless()
                                else useChrome()
                            }
                        }

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
                if (windowsTarget) add(mingwX64("windows") { windows() })
                if (linuxTarget) add(linuxX64("linux") { linux() })
                if (macosTarget) add(macosArm64("macos") { macos() })
            }.forEach { target ->
                target.native()
            }

            // SourceSet
            KotlinMultiplatformSourceSetsScope(this@build, extension, sourceSets).source()

            action()
        }

        extensions.findByType<ComposeCompilerGradlePluginExtension>()?.apply {
            val composeCompilerOutput = layout.buildDirectory.dir("composeStability")
            if (buildDefaults.composeReports) {
                reportsDestination.set(composeCompilerOutput)
                metricsDestination.set(composeCompilerOutput)
            }
            val stabilityFileDir = layout.projectDirectory.dir("config")
            val stabilityFiles = (stabilityFileDir.asFile.listFiles { it.extension == "conf" } ?: emptyArray<File>()).map {
                stabilityFileDir.file(it.name)
            }
            if (stabilityFiles.isNotEmpty()) stabilityConfigurationFiles.addAll(stabilityFiles)
        }

        extensions.findByType<ComposeExtension>()?.apply {
            if (exportResources) {
                this.extensions.findByType<ResourcesExtension>()?.apply {
                    publicResClass = true
                    packageOfResClass = "$uniqueSafeModuleName.resources"
                }
            }

            this.extensions.findByType<DesktopExtension>()?.apply {
                val dpkg = desktopPackage
                if (dpkg != null && dpkg.usePlatformPackage()) {
                    application {
                        mainClass = dpkg.mainClass ?: "MainKt"
                        jvmArgs += dpkg.jvmArgs
                        jvmArgs += buildDefaults.jvm.args

                        buildTypes {
                            release {
                                proguard {
                                    version.set(buildDefaults.desktop.proguardVersion)
                                    isEnabled.set(true)
                                    optimize.set(true)
                                    obfuscate.set(true)
                                    joinOutputJars.set(true)

                                    /*
                                     * Compose Desktop 似乎还不支持内嵌 Proguard 规则
                                     * 手动解析依赖
                                     */
                                    val proguardFiles = mutableListOf<File>()
                                    for (dependency in dpkg.proguard) {
                                        findProjectDirectory(dependency)?.let { directory ->
                                            directory.dir("src/desktopMain/resources/META-INF/proguard").asFile.let { subDir ->
                                                if (subDir.isDirectory) proguardFiles += subDir.listFiles { it.extension == "pro" }!!
                                            }
                                            directory.dir("src/main/resources/META-INF/proguard").asFile.let { subDir ->
                                                if (subDir.isDirectory) proguardFiles += subDir.listFiles { it.extension == "pro" }!!
                                            }
                                        }
                                    }
                                    // 再添加自身
                                    desktopProguardKMPDir.asFile.let { subDir ->
                                        if (subDir.isDirectory) proguardFiles += subDir.listFiles { it.extension == "pro" }!!
                                    }
                                    desktopProguardJVMDir.asFile.let { subDir ->
                                        if (subDir.isDirectory) proguardFiles += subDir.listFiles { it.extension == "pro" }!!
                                    }

                                    // 合并所有混淆规则
                                    configurationFiles.from(*proguardFiles.toTypedArray())
                                }

                                if (dpkg.useAOT) {
                                    aot {
                                        mode = AotMode.AotPrebuild
                                        logging = false
                                        exitAppOnAotFailure = true
                                    }
                                }
                            }
                        }

                        nativeDistributions {
                            packageName = dpkg.packageName ?: uniqueSafeName
                            packageVersion = buildDefaults.desktop.packageVersion
                            copyright = buildDefaults.copyright
                            vendor = buildDefaults.organization
                            buildDefaults.desktop.licenseFile?.let { licenseFile.set(project.file(it)) }

                            modules(*dpkg.jvmModules.toTypedArray())

                            appResourcesRootDir.set(project.packageResourcesDir)

                            val targetList = mutableListOf<TargetFormat>()

                            if (dpkg.windowsSettings != null) {
                                targetList += TargetFormat.Exe
                                windows {
                                    console = false
                                    exePackageVersion = buildDefaults.desktop.packageVersion
                                    dpkg.windowsSettings!!.invoke(this)
                                }
                            }

                            if (dpkg.linuxSettings != null) {
                                targetList += TargetFormat.Deb
                                linux {
                                    debPackageVersion = buildDefaults.desktop.packageVersion
                                    dpkg.linuxSettings!!.invoke(this)
                                }
                            }

                            if (dpkg.macosSettings != null) {
                                targetList += TargetFormat.Pkg
                                macOS {
                                    pkgPackageVersion = buildDefaults.desktop.packageVersion
                                    dpkg.macosSettings!!.invoke(this)
                                }
                            }

                            targetFormats(*targetList.toTypedArray())

                            with(dpkg) { onPackage() }
                        }
                    }
                }
            }
        }

        configureActions()
        registerDesktopNative(desktopNativeKMPSourceDir, "desktopJar")
    }
}
