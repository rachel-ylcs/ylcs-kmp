import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.NamedDomainObjectCollectionDelegateProvider
import org.gradle.kotlin.dsl.NamedDomainObjectContainerCreatingDelegateProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getting
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

// 在 ExportLib 后面的库将会被导出给其他模块可见
data object ExportLib

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

data class Maven(
    val description: String,
    val inceptionYear: Int,
    val authors: List<Pair<String, String>> = emptyList()
)

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

    open val maven: Maven? = null

    open fun Project.build(extension: T) { extension.action() }

    fun HasKotlinDependencies.lib(vararg libs: Any) {
        dependencies {
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

    fun KotlinCommonCompilerOptions.useLanguageFeature(vararg features: String) {
        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xreturn-value-checker=check",
            *features
        )
    }
}

inline fun <reified T : KotlinBaseExtension> Project.template(t: KotlinTemplate<T>) {
    t.internalBackendInfo = BackendInfo(C.namespace, this.path, this.layout.projectDirectory.asFile.absolutePath)
    with(t) {
        extensions.configure<T> {
            build(this)
        }
    }
}