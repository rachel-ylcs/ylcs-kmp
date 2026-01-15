import com.vanniktech.maven.publish.MavenPublishBaseExtension
import love.yinlin.task.GenerateCodeTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.NamedDomainObjectCollectionDelegateProvider
import org.gradle.kotlin.dsl.NamedDomainObjectContainerCreatingDelegateProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool

// 在 ExportLib 后面的库将会被导出给其他模块可见
data object ExportLib

// Npm
class NpmInfo internal constructor(val name: String, val version: String)

class BackendInfo(namespace: String, selector: String, path: String) {
    val uniqueSelector: String = selector
    val uniqueName: String = selector.removeSuffix(":").substringAfterLast(':')
    val uniqueSafeName: String = uniqueName.replace('-', '_')
    val uniqueModuleName: String = "$namespace.${selector.removePrefix(":").removeSuffix(":").substringAfter(':').replace(':', '.')}"
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
        vararg parents: KotlinSourceSet,
        block: KotlinSourceSet.() -> Unit = {}
    ) : NamedDomainObjectCollectionDelegateProvider<KotlinSourceSet> = set.getting {
        for (parent in parents) dependsOn(parent)
        block()
    }

    fun create(
        vararg parents: KotlinSourceSet,
        block: KotlinSourceSet.() -> Unit = {}
    ) : NamedDomainObjectContainerCreatingDelegateProvider<KotlinSourceSet> = set.creating {
        for (parent in parents) dependsOn(parent)
        block()
    }

    fun KotlinSourceSet.configure(
        vararg parents: KotlinSourceSet,
        block: KotlinSourceSet.() -> Unit = {}
    ) {
        for (parent in parents) dependsOn(parent)
        block()
    }

    fun List<KotlinSourceSet>.configure(
        vararg parents: KotlinSourceSet,
        block: KotlinSourceSet.() -> Unit = {}
    ) {
        for (item in this) {
            for (parent in parents) item.dependsOn(parent)
            item.block()
        }
    }
}

abstract class KotlinTemplate<T : KotlinBaseExtension> {
    protected open fun T.action() { }
    protected open fun Project.actions() { }

    var internalBackendInfo: BackendInfo? = null

    // 下面的值只能在 Template 的非 init 中使用
    // :ylcs-module:compose:platform-view
    val uniqueSelector: String get() = internalBackendInfo!!.uniqueSelector
    // platform-view
    val uniqueName: String get() = internalBackendInfo!!.uniqueName
    // platform_view
    val uniqueSafeName: String get() = internalBackendInfo!!.uniqueSafeName
    // love.yinlin.compose.platform-view
    val uniqueModuleName: String get() = internalBackendInfo!!.uniqueModuleName
    // love.yinlin.compose.platform_view
    val uniqueSafeModuleName: String get() = internalBackendInfo!!.uniqueSafeModuleName
    // love.yinlin.compose
    val uniqueGroupName: String get() = internalBackendInfo!!.uniqueGroupName
    // /path/to/ylcs-kmp/ylcs-module/compose/platform-view
    val uniquePath: String get() = internalBackendInfo!!.uniquePath

    open fun Project.build(extension: T) { extension.action() }

    // 引入依赖
    fun npm(name: String, version: Provider<String>): NpmInfo = NpmInfo(name, version.get())

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

    // 启用编译器实验性特性
    fun KotlinCommonCompilerOptions.useLanguageFeature(vararg features: String) {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xreturn-value-checker=check",
            *features
        )
    }

    // 需要生成代码
    fun Project.setupGenerateCode(taskName: String, block: GenerateCodeTask.() -> Unit) {
        val generateTask = tasks.register(taskName, GenerateCodeTask::class, configurationAction = block)
        rootProject.tasks.findByName("prepareKotlinBuildScriptModel")?.apply {
            dependsOn(generateTask)
        }
        tasks.withType<AbstractKotlinCompileTool<*>>().configureEach {
            dependsOn(generateTask)
        }
    }
}

inline fun <reified T : KotlinBaseExtension> Project.template(t: KotlinTemplate<T>) {
    t.internalBackendInfo = BackendInfo(C.namespace, this.path, this.layout.projectDirectory.asFile.absolutePath).also {
        group = it.uniqueGroupName
        version = C.app.version
    }

    with(t) {
        extensions.configure<T> {
            build(this)
        }

        // Dokka
        extensions.findByType<DokkaExtension>()?.apply {
            dokkaPublications.named("html") {
                moduleName.set(t.uniqueModuleName)
                moduleVersion.set(C.app.versionName)
                failOnWarning.set(false)
            }
        }

        // Maven
        extensions.findByType<MavenPublishBaseExtension>()?.apply {
            publishToMavenCentral()
            signAllPublications()
            coordinates(uniqueGroupName, uniqueName, C.app.versionName)

            pom {
                name.set(uniqueName)
                description.set(uniqueModuleName)
                inceptionYear.set("2025")
                url.set(C.app.homepage)
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/MIT")
                        distribution.set("https://opensource.org/license/MIT")
                    }
                }
                developers {
                    developer {
                        id.set(C.app.name)
                        name.set(C.app.displayName)
                        url.set(C.app.homepage)
                    }
                }
                scm {
                    url.set(C.app.homepage)
                    connection.set("scm:git:git${C.app.homepage.removePrefix("https")}.git")
                    developerConnection.set("scm:git:git${C.app.homepage.removePrefix("https")}.git")
                }
            }
        }
    }
}