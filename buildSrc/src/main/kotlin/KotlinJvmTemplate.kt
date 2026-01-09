import com.android.build.gradle.internal.tasks.factory.dependsOn
import love.yinlin.task.BuildDesktopNativeTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

class KotlinJvmSourceSetsScope(
    p: Project,
    set: NamedDomainObjectContainer<KotlinSourceSet>
) : KotlinSourceSetsScope(p, set) {
    val main: KotlinSourceSet by lazy { set.named("main").get() }
    val test: KotlinSourceSet by lazy { set.named("test").get() }
}

abstract class KotlinJvmTemplate : KotlinTemplate<KotlinJvmExtension>() {
    open fun KotlinJvmSourceSetsScope.source() { }

    open val jvmName: String? = null
    open val jvmMainClass: String? = null
    open val jvmArgs: List<String> = emptyList()
    open fun JavaApplication.application() { }

    final override fun Project.build(extension: KotlinJvmExtension) {
        with(extension) {
            compilerOptions {
                useLanguageFeature()
                jvmTarget.set(C.jvm.target)
            }

            KotlinJvmSourceSetsScope(this@build, sourceSets).source()

            action()
        }

        extensions.findByType<JavaApplication>()?.apply {
            mainClass.set(jvmMainClass)
            applicationName = jvmName ?: ""
            applicationDefaultJvmArgs = buildList {
                addAll(jvmArgs)
                add("--enable-native-access=ALL-UNNAMED")
            }
            application()
        }

        afterEvaluate {
            actions()

            // 检查是否需要编译 Desktop Native
            val libName = System.mapLibraryName(name.replace('-', '_'))
            val moduleDir = layout.projectDirectory.asFile
            val sourceDir = moduleDir.resolve("src/main/cpp")
            val nativeBuildTmpDir = layout.buildDirectory.dir("desktopNative").get()
            if (sourceDir.exists()) {
                val buildNativeTask = tasks.register("buildDesktopNative", BuildDesktopNativeTask::class) {
                    inputDir.set(sourceDir)
                    outputFile.set(C.root.artifacts.desktopNative.file(libName))
                    platform.set(C.platform)
                    nativeBuildDir.set(nativeBuildTmpDir.asFile)
                    nativeJniDir.set(C.root.artifacts.include.asFile)
                }
                tasks.named("jar").dependsOn(buildNativeTask)
            }
        }
    }
}