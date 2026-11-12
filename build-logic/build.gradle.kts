import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.assignment.plugin.gradle.AssignmentExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension
import java.util.Properties

plugins {
    base
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSamWithReceiver) apply false
    alias(libs.plugins.kotlinAssignment) apply false
    alias(libs.plugins.mavenPublish) apply false
}

version = libs.versions.rachel.get()

val kotlinLanguage = KotlinVersion.fromVersion(libs.versions.kotlin.get().split('.').take(2).joinToString("."))
val kotlinProfile = Properties().apply { rootDir.resolve("../gradle.properties").inputStream().use(::load) }

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        pluginManager.apply("org.jetbrains.kotlin.plugin.sam.with.receiver")
        pluginManager.apply("org.jetbrains.kotlin.plugin.assignment")
        extensions.configure<SamWithReceiverExtension> { annotation("org.gradle.api.HasImplicitReceiver") }
        extensions.configure<AssignmentExtension> { annotation("org.gradle.api.SupportsKotlinAssignmentOverloading") }
        dependencies.add("compileOnly", gradleKotlinDsl())
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                languageVersion.set(kotlinLanguage)
                apiVersion.set(kotlinLanguage)
                freeCompilerArgs.addAll(kotlinProfile.getProperty("rachel.kotlin.compilerArgs").split(','))
                // Gradle's embedded script compiler consumes these plugin binaries.
                freeCompilerArgs.addAll("-Xabi-stability=stable", "-java-parameters", "-jvm-default=enable", "-Xsam-conversions=class")
            }
        }
    }
    if (name in setOf("public-plugin", "native-bridge", "native-dokka", "native-publications")) {
        pluginManager.apply(if (name == "native-publications") "com.vanniktech.maven.publish.base" else "com.vanniktech.maven.publish")
        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral(automaticRelease = false)
            signAllPublications()
            pom {
                name.set(project.name)
                description.set("Rachel Kotlin Multiplatform build tools")
                url.set("https://github.com/rachel-ylcs/ylcs-kmp")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("rachel-ylcs")
                        name.set("Rachel contributors")
                        url.set("https://github.com/rachel-ylcs")
                    }
                }
                scm {
                    url.set("https://github.com/rachel-ylcs/ylcs-kmp")
                    connection.set("scm:git:https://github.com/rachel-ylcs/ylcs-kmp.git")
                    developerConnection.set("scm:git:ssh://git@github.com/rachel-ylcs/ylcs-kmp.git")
                }
            }
        }
        extensions.configure<SigningExtension> {
            setRequired(provider {
                gradle.taskGraph.allTasks.any { it is PublishToMavenRepository && it.repository.name == "mavenCentral" }
            })
        }
    }
}
