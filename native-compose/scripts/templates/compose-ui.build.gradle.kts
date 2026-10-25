import groovy.json.JsonSlurper
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

val repositoryDirectory = rootDir.resolve("../../../../..").canonicalFile
val nativeDirectory = repositoryDirectory.resolve("native-compose")
val sourceLock = JsonSlurper().parse(nativeDirectory.resolve("upstream/compose.json")) as Map<*, *>
val modules = sourceLock["modules"] as Map<*, *>
val hostTarget = when (HostManager.host.name) {
    "mingw_x64" -> "windows"
    "linux_x64" -> "linux"
    "macos_arm64" -> "macos"
    else -> error("Unsupported Native UI build host: ${HostManager.host}")
}
layout.buildDirectory.set(nativeDirectory.resolve("vendor/build/compose-ui/root"))

subprojects {
    group = sourceLock["group"] as String
    version = libs.versions.cmp.get()
    layout.buildDirectory.set(nativeDirectory.resolve("vendor/build/compose-ui/$name"))

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(__JAVA_VERSION__)
            compilerOptions {
                freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcollection-literals")
                optIn.addAll("androidx.compose.ui.ExperimentalComposeUiApi", "androidx.compose.ui.unit.ExperimentalUnitApi")
            }
            mingwX64("windows")
            linuxX64("linux")
            macosArm64("macos")
            sourceSets {
                commonTest.dependencies { implementation(kotlin("test")) }
                val nativeMain = create("nativeMain") { dependsOn(commonMain.get()) }
                val nativeTest = create("nativeTest") { dependsOn(commonTest.get()) }
                for (target in listOf("windows", "linux", "macos")) {
                    getByName("${target}Main").dependsOn(nativeMain)
                    getByName("${target}Test").dependsOn(nativeTest)
                }
                val module = modules[project.name] as? Map<*, *>
                val selectedSources = module?.get("sourceSets") as? Map<*, *>
                if (selectedSources != null) {
                    val adaptation = nativeDirectory.resolve("adaptation/org.jetbrains.compose.ui_${project.name}")
                    configureEach {
                        val upstream = selectedSources[name] as? List<*> ?: emptyList<String>()
                        kotlin.setSrcDirs(upstream.map { project.file(it as String) })
                        kotlin.srcDir(adaptation.resolve("src/$name/kotlin"))
                    }
                }
            }
        }
    }

    plugins.withId("maven-publish") {
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "NativeVerification"
                    url = uri(nativeDirectory.resolve("vendor/build/compose-ui/repository"))
                }
            }
            publications.withType<MavenPublication>().configureEach {
                val target = kotlin.targets.findByName(name) as? KotlinNativeTarget
                artifactId = if (target == null) project.name else "${project.name}-${target.konanTarget.name.replace("_", "")}"
                pom {
                    name.set("Rachel Native ${project.name}")
                    description.set("Native desktop adaptation of Compose ${project.name}; modified by Rachel")
                    url.set("https://github.com/rachel-ylcs/ylcs-kmp")
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm { url.set("https://github.com/JetBrains/compose-multiplatform-core") }
                }
            }
        }
        tasks.withType<PublishToMavenRepository>().configureEach {
            doFirst {
                check(repository.url.scheme == "file") { "Native UI remote Maven publishing is disabled" }
                check(publication.name in setOf("kotlinMultiplatform", hostTarget)) {
                    "Publish ${publication.name} on its matching Native host"
                }
            }
        }
        tasks.withType<Jar>().matching { it.name.endsWith("SourcesJar") || it.name == "sourcesJar" }.configureEach {
            from(rootDir.resolve("../LICENSE.txt")) { into("META-INF/licenses") }
            from(nativeDirectory.resolve("upstream/compose.json"), nativeDirectory.resolve("upstream/compose-origins.json")) {
                into("META-INF/provenance")
            }
        }
    }
}

tasks.register("publishHostToVerificationRepository") {
    group = "publishing"
    for (module in modules.keys) {
        dependsOn(":$module:publishKotlinMultiplatformPublicationToNativeVerificationRepository")
        dependsOn(":$module:publish${hostTarget.replaceFirstChar(Char::uppercase)}PublicationToNativeVerificationRepository")
    }
}
