import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.plugins.signing.SigningExtension

plugins {
    base
    kotlin("jvm") version embeddedKotlinVersion apply false
    alias(libs.plugins.mavenPublish) apply false
}

version = libs.versions.rachel.get()

subprojects {
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
