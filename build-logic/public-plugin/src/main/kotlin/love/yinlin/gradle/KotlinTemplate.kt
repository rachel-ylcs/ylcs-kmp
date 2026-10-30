package love.yinlin.gradle

import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import love.yinlin.gradle.task.GenerateCodeTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

// 在 ExportLib 后面的库将会被导出给其他模块可见
data object ExportLib

// Npm
class NpmInfo internal constructor(val name: String, val version: String)

class BackendInfo(
    namespace: String,
    selector: String,
    path: String,
    stripFirstPathSegment: Boolean = false,
    rootProjectName: String = "root",
) {
    val uniqueSelector: String = selector
    val uniqueName: String = selector.removeSuffix(":").substringAfterLast(':').ifEmpty { rootProjectName }
    val uniqueSafeName: String = uniqueName.replace('-', '_')
    private val modulePath = selector.removePrefix(":").removeSuffix(":").let {
        if (it.isEmpty()) uniqueName
        else if (stripFirstPathSegment) it.substringAfter(':')
        else it
    }.replace(':', '.')
    val uniqueModuleName: String = "$namespace.$modulePath"
    val uniqueSafeModuleName: String = uniqueModuleName.replace('-', '_')
    val uniqueGroupName: String = uniqueModuleName.removeSuffix(".$uniqueName")
    val uniquePath: String = path

    override fun toString(): String = "Template(name=$uniqueName, module=$uniqueModuleName, path=$uniquePath)"
}

abstract class KotlinSourceSetsScope(
    private val p: Project,
    private val set: NamedDomainObjectContainer<KotlinSourceSet>
) {
    fun find(
        name: String,
        vararg parents: KotlinSourceSet,
        block: KotlinSourceSet.() -> Unit = {}
    ) : KotlinSourceSet = set.getByName(name) {
        for (parent in parents) dependsOn(parent)
        block()
    }

    fun create(
        name: String,
        vararg parents: KotlinSourceSet,
        block: KotlinSourceSet.() -> Unit = {}
    ) : KotlinSourceSet = set.create(name) {
        for (parent in parents) dependsOn(parent)
        block()
    }

    operator fun KotlinSourceSet.invoke(
        vararg parents: KotlinSourceSet,
        block: KotlinSourceSet.() -> Unit = {}
    ) {
        for (parent in parents) dependsOn(parent)
        block()
    }

    operator fun List<KotlinSourceSet>.invoke(
        vararg parents: KotlinSourceSet,
        block: KotlinSourceSet.() -> Unit = {}
    ) {
        for (item in this) {
            for (parent in parents) item.dependsOn(parent)
            item.block()
        }
    }
}

open class KotlinTemplate<T : KotlinBaseExtension> {
    private var actionBlock: T.() -> Unit = {}
    fun kotlin(block: T.() -> Unit) { actionBlock = block }
    internal fun T.action() { actionBlock(this) }

    private var projectActionsBlock: (Project.() -> Unit)? = null
    fun actions(block: Project.() -> Unit) { projectActionsBlock = block }
    internal fun Project.configureActions() {
        val block = projectActionsBlock ?: return
        afterEvaluate { block(this) }
    }

    internal lateinit var owner: Project
    protected val defaults: BuildDefaults get() = owner.buildDefaults

    internal var desktopCppSources: List<String> = emptyList()

    internal lateinit var internalBackendInfo: BackendInfo

    // 下面的值只能在 Template 的非 init 中使用
    // :ylcs-module:compose:platform-view
    val uniqueSelector: String get() = internalBackendInfo.uniqueSelector
    // platform-view
    val uniqueName: String get() = internalBackendInfo.uniqueName
    // platform_view
    val uniqueSafeName: String get() = internalBackendInfo.uniqueSafeName
    // love.yinlin.compose.platform-view
    val uniqueModuleName: String get() = internalBackendInfo.uniqueModuleName
    // love.yinlin.compose.platform_view
    val uniqueSafeModuleName: String get() = internalBackendInfo.uniqueSafeModuleName
    // love.yinlin.compose
    val uniqueGroupName: String get() = internalBackendInfo.uniqueGroupName
    // /path/to/ylcs-kmp/ylcs-module/compose/platform-view
    val uniquePath: String get() = internalBackendInfo.uniquePath

    open fun Project.build(extension: T) { extension.action() }

    // 引入依赖
    fun npm(name: String, version: Provider<String>): NpmInfo = NpmInfo(name, version.get())
    fun npm(name: String, version: String): NpmInfo = NpmInfo(name, version)

    fun HasKotlinDependencies.lib(vararg libs: Any) {
        dependencies {
            var isExport = false
            for (item in libs) {
                when {
                    item is ExportLib -> isExport = true
                    isExport -> {
                        if (item is NpmInfo) api(npm(item.name, item.version))
                        else api(item)
                    }
                    else -> {
                        if (item is NpmInfo) implementation(npm(item.name, item.version))
                        else implementation(item)
                    }
                }
            }
        }
    }

    /** 在默认 JVM C++ 目录之外跟踪共享源码，并通过 RACHEL_CPP_SOURCE_DIRS 提供给 CMake。 */
    var KotlinJvmTarget.cpp: List<String>
        get() = desktopCppSources
        set(value) { desktopCppSources = value }

    /** 为纯 JVM 模板配置附加 C++ 源码目录。 */
    var KotlinJvmExtension.cpp: List<String>
        get() = desktopCppSources
        set(value) { desktopCppSources = value }

    /**
     * 为当前 Native target 的 main compilation 注册 CInterop，沿用 src/nativeInterop/cinterop/<name>.def。
     * 共享和平台配置中的声明会合并，同名条目只注册一次；赋值不会删除已有 CInterop。
     */
    var KotlinNativeTarget.cpp: List<String>
        get() = compilations.getByName("main").cinterops.names.toList()
        set(value) {
            val interops = compilations.getByName("main").cinterops
            for (name in value) interops.maybeCreate(name)
        }

    // 启用编译器实验性特性
    fun KotlinCommonCompilerOptions.useLanguageFeature(vararg features: String) {
        freeCompilerArgs.addAll(defaults.languageFeatures + features)
    }

    fun KotlinSourceSet.generateCode(taskName: String, block: GenerateCodeTask.() -> Unit): TaskProvider<GenerateCodeTask> {
        val generateTask = owner.setupGenerateCode(taskName, block)
        kotlin.srcDir(generateTask.flatMap { it.outputDirectory })
        return generateTask
    }

    fun Project.setupGenerateCode(taskName: String, block: GenerateCodeTask.() -> Unit): TaskProvider<GenerateCodeTask> {
        val generateTask = tasks.register(taskName, GenerateCodeTask::class, configurationAction = block)
        GeneratedBuildSources.publish(this, generateTask.flatMap { it.outputFile })
        tasks.withType<AbstractKotlinCompileTool<*>>().configureEach {
            dependsOn(generateTask)
        }
        return generateTask
    }
}

internal inline fun <reified T : KotlinBaseExtension> Project.configureTemplate(t: KotlinTemplate<T>) {
    group = t.uniqueGroupName
    buildDefaults.versionCode?.let { version = it }

    with(t) {
        extensions.configure<T> { build(this) }

        if (providers.environmentVariable("GITHUB_ACTIONS").orNull == "true" && providers.environmentVariable("RUNNER_OS").orNull == "macOS") {
            extensions.findByType<KotlinMultiplatformExtension>()?.targets?.withType<KotlinNativeTarget>()?.configureEach {
                if (konanTarget.family.isAppleFamily) {
                    // CI tests keep Debug behavior but do not need Apple's DWARF debug map.
                    binaries.withType<TestExecutable>().configureEach { linkerOpts("-S") }
                }
            }
        }

        val publication = buildDefaults.publishing
        extensions.findByType<DokkaExtension>()?.apply {
            dokkaPublications.named("html") {
                moduleName.set(t.uniqueModuleName)
                moduleVersion.set(publication.version ?: project.version.toString())
                failOnWarning.set(false)
            }
        }

        extensions.findByType<MavenPublishBaseExtension>()?.apply {
            if (publication.central) {
                if (publication.validateDeployment) publishToMavenCentral(automaticRelease = publication.automaticRelease)
                else publishToMavenCentral(automaticRelease = publication.automaticRelease, validateDeployment = DeploymentValidation.NONE)
            }
            if (publication.sign) signAllPublications()
            coordinates(uniqueGroupName, uniqueName, publication.version ?: project.version.toString())

            pom {
                name.set(uniqueName)
                description.set(uniqueModuleName)
                publication.inceptionYear?.let { inceptionYear.set(it) }
                publication.homepage?.let { url.set(it) }
                if (publication.licenseName != null) licenses {
                    license {
                        name.set(publication.licenseName)
                        url.set(publication.licenseUrl)
                        distribution.set(publication.licenseUrl)
                    }
                }
                if (publication.developerId != null) developers {
                    developer {
                        id.set(publication.developerId)
                        name.set(publication.developerName)
                        url.set(publication.homepage)
                    }
                }
                if (publication.homepage != null) scm {
                    url.set(publication.homepage)
                    connection.set(publication.scmConnection)
                    developerConnection.set(publication.scmConnection)
                }
            }
        }
    }
}
