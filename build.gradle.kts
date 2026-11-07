import love.yinlin.repository.tip.showtime
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    idea
    install(
        libs.plugins.rachel,
        libs.plugins.nativeBridge,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.kotlinSerialization,
        libs.plugins.kotlinJvm,
        libs.plugins.composeMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.androidApplication,
        libs.plugins.androidLibrary,
        libs.plugins.mavenPublish,
        libs.plugins.dokka,
        apply = false
    )
}

idea.module {
    excludeDirs.add(file("native-compose/vendor"))
}

val kotlinLanguage = KotlinVersion.fromVersion(libs.versions.kotlin.get().split('.').take(2).joinToString("."))
allprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.languageVersion.set(kotlinLanguage)
        compilerOptions.apiVersion.set(kotlinLanguage)
    }
}

showtime()
