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

abstract class KotlinSourceSetsScope(private val set: NamedDomainObjectContainer<KotlinSourceSet>) {
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
    with(t) {
        extensions.configure<T> {
            build(this)
        }
    }
}