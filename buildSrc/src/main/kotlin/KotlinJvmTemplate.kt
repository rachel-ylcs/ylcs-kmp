import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class KotlinJvmSourceSetsScope(
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(set) {
    val main: KotlinSourceSet by lazy { set.named("main").get() }
}

abstract class KotlinJvmTemplate : KotlinTemplate<KotlinJvmExtension>() {
    open fun KotlinJvmSourceSetsScope.source() { }

    open val isApplication: Boolean = false
    open val jvmName: String? = null
    open val jvmMainClass: String? = null
    open val jvmArgs: List<String> = emptyList()
    open fun JavaApplication.application() { }

    final override fun Project.build(extension: KotlinJvmExtension) {
        with(extension) {
            compilerOptions {
                freeCompilerArgs.addAll(C.features)
                jvmTarget.set(C.jvm.target)
            }

            KotlinJvmSourceSetsScope(sourceSets).source()

            action()
        }

        if (isApplication) {
            extensions.configure<JavaApplication> {
                mainClass.set(jvmMainClass)
                applicationName = jvmName ?: ""
                applicationDefaultJvmArgs = buildList {
                    addAll(jvmArgs)
                    add("--enable-native-access=ALL-UNNAMED")
                }
                application()
            }
        }

        afterEvaluate {
            actions()
        }
    }
}