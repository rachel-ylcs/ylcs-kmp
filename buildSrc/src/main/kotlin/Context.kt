import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

val Project.currentTaskName: String get() = this.gradle.startParameter.taskNames.firstOrNull() ?: "sync"

// 快速引入

fun HasKotlinDependencies.useLib(vararg libs: Any) {
    dependencies {
        for (lib in libs) implementation(lib)
    }
}

fun HasKotlinDependencies.useApi(vararg libs: Any) {
    dependencies {
        for (lib in libs) api(lib)
    }
}

fun KotlinSourceSet.useSourceSet(vararg parents: KotlinSourceSet) {
    for (parent in parents) dependsOn(parent)
}

// 编译配置

fun Constants.useCompilerFeatures(context: HasConfigurableKotlinCompilerOptions<out KotlinCommonCompilerOptions>) {
    context.compilerOptions {
        freeCompilerArgs.addAll(features)
    }
}

fun Constants.jvmTarget(context: HasConfigurableKotlinCompilerOptions<out KotlinJvmCompilerOptions>) {
    context.compilerOptions {
        jvmTarget.set(jvm.target)
    }
}