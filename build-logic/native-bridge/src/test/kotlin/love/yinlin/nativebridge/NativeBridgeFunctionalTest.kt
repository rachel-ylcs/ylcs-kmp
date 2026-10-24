package love.yinlin.nativebridge

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeBridgeFunctionalTest {
    private fun project(): File {
        val work = File(System.getProperty("native.tests.work")).apply { mkdirs() }
        return Files.createTempDirectory(work.toPath(), "bridge-").toFile().also {
            it.resolve("settings.gradle.kts").writeText("rootProject.name = \"consumer\"\n")
            it.resolve("gradle.properties").writeText("kotlin.stdlib.default.dependency=false\nkotlin.native.ignoreDisabledTargets=true\n")
        }
    }

    private fun runner(directory: File, vararg tasks: String) = GradleRunner.create()
        .withProjectDir(directory)
        .withPluginClasspath()
        .withArguments(*tasks, "--stacktrace", "--console=plain", "--gradle-user-home", System.getProperty("native.tests.gradleUserHome"))

    private fun module(directory: File, group: String, version: String, dependencies: String = "") {
        val module = directory.resolve("repo/${group.replace('.', '/')}/render/$version").apply { mkdirs() }
        val variants = listOf("mingw_x64", "linux_x64", "macos_arm64", "jvm", "common").joinToString(",") { target ->
            val platform = if (target in setOf("jvm", "common")) target else "native"
            val nativeAttribute = if (platform == "native") ",\"org.jetbrains.kotlin.native.target\":\"$target\"" else ""
            val usage = when (platform) {
                "jvm" -> "java-api"
                "common" -> "kotlin-metadata"
                else -> "kotlin-api"
            }
            """{"name":"$target","attributes":{"org.gradle.usage":"$usage","org.gradle.category":"library","org.jetbrains.kotlin.platform.type":"$platform"$nativeAttribute},"dependencies":[$dependencies]}"""
        }
        module.resolve("render-$version.module").writeText("""{"formatVersion":"1.1","component":{"group":"$group","module":"render","version":"$version"},"variants":[$variants]}""")
        module.resolve("render-$version.pom").writeText("""<project><modelVersion>4.0.0</modelVersion><groupId>$group</groupId><artifactId>render</artifactId><version>$version</version></project>""")
    }

    private fun consumer(directory: File, targets: String, requestedVersion: String = "1.0", extra: String = "", includePort: Boolean = true) {
        module(directory, "test.upstream", requestedVersion)
        if (includePort) module(directory, "test.port", "1.0-native")
        directory.resolve("build.gradle.kts").writeText("""
            import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("love.yinlin.native-bridge")
            }
            repositories {
                maven { url = uri("repo"); metadataSources { gradleMetadata(); mavenPom() } }
                mavenCentral()
            }
            kotlin {
                $targets
                sourceSets.commonMain.dependencies { implementation("test.upstream:render:$requestedVersion") }
            }
            nativeBridge {
                redirect("render") {
                    upstream.set("test.upstream:render")
                    upstreamVersion.set("1.0")
                    targets.set(setOf("mingw_x64"))
                    module.set("test.port:render:1.0-native")
                }
            }
            tasks.register("verifyRouting") {
                doLast {
                    kotlin.targets.filter { it.name != "metadata" }.forEach { target ->
                        val config = configurations.getByName(target.compilations.getByName("main").compileDependencyConfigurationName)
                        val ids = config.incoming.resolutionResult.allComponents.map { it.id.displayName }
                        println("ROUTE " + target.name + " = " + ids.joinToString())
                        config.incoming.resolutionResult.allDependencies.forEach {
                            if (it is org.gradle.api.artifacts.result.UnresolvedDependencyResult) throw it.failure
                        }
                    }
                    println("APPLICATION_RUNTIMES=" + configurations.count { it.name.endsWith("NativeApplicationRuntime") })
                }
            }
            $extra
        """.trimIndent())
    }

    @Test
    fun customTargetNameAndPerTargetRoutingLeaveJvmAndOtherNativeOfficial() {
        val project = project()
        consumer(project, "mingwX64(\"windows\"); linuxX64(\"linux\"); macosArm64(\"mac\"); jvm(\"desktop\")")
        val output = runner(project, "verifyRouting").build().output
        assertContains(output.lineSequence().first { it.startsWith("ROUTE windows =") }, "test.port:render:1.0-native")
        assertContains(output.lineSequence().first { it.startsWith("ROUTE linux =") }, "test.upstream:render:1.0")
        assertContains(output.lineSequence().first { it.startsWith("ROUTE mac =") }, "test.upstream:render:1.0")
        assertContains(output.lineSequence().first { it.startsWith("ROUTE desktop =") }, "test.upstream:render:1.0")
        assertContains(output, "APPLICATION_RUNTIMES=0")
    }

    @Test
    fun jvmOnlyNeverResolvesPortOrCreatesRuntimeConfiguration() {
        val project = project()
        consumer(project, "jvm(\"desktop\")", includePort = false)
        val output = runner(project, "verifyRouting").build().output
        assertContains(output, "test.upstream:render:1.0")
        assertFalse(output.contains("test.port:render:1.0-native"))
        assertContains(output, "APPLICATION_RUNTIMES=0")
    }

    @Test
    fun maintainerModeCannotFallBackToAnAvailableMavenPort() {
        val project = project()
        consumer(project, "mingwX64(\"windows\")")
        project.resolve("settings.gradle.kts").appendText("gradle.extensions.extraProperties.set(\"love.yinlin.nativeCompose.maintainer\", true)\n")
        val output = runner(project, "verifyRouting").buildAndFail().output
        assertContains(output, "maintainer mode requires a local project for render")
        assertContains(output, "Maven fallback is disabled")
    }

    @Test
    fun maintainerModeLeavesJvmOnOfficialDependencies() {
        val project = project()
        consumer(project, "jvm(\"desktop\")", includePort = false)
        project.resolve("settings.gradle.kts").appendText("gradle.extensions.extraProperties.set(\"love.yinlin.nativeCompose.maintainer\", true)\n")
        val output = runner(project, "verifyRouting").build().output
        assertContains(output, "test.upstream:render:1.0")
        assertFalse(output.contains("test.port:render:1.0-native"))
    }

    @Test
    fun untestedVersionFailsInsteadOfSilentlyDowngrading() {
        val project = project()
        consumer(project, "mingwX64(\"windows\")", "1.1")
        val output = runner(project, "verifyRouting").buildAndFail().output
        assertContains(output, "outside the tested version 1.0")
    }

    @Test
    fun oldTransitiveRequestAndNewDirectDependencySelectOnePortAndLeaveJvmOfficial() {
        val project = project()
        consumer(project, "mingwX64(\"windows\"); jvm(\"desktop\")")
        module(project, "test.upstream", "0.9")
        module(project, "test.legacy", "1.0", """{"group":"test.upstream","module":"render","version":{"requires":"0.9"}}""")
        project.resolve("build.gradle.kts").appendText("\nkotlin { sourceSets.commonMain.dependencies { implementation(\"test.legacy:render:1.0\") } }\n")
        val output = runner(project, "verifyRouting").build().output
        val native = output.lineSequence().first { it.startsWith("ROUTE windows =") }
        assertContains(native, "test.port:render:1.0-native")
        assertFalse(native.contains("test.upstream:render:"))
        assertContains(output.lineSequence().first { it.startsWith("ROUTE desktop =") }, "test.upstream:render:1.0")
    }

    @Test
    fun strictOlderVersionIsNotSilentlyReplaced() {
        val project = project()
        consumer(project, "mingwX64(\"windows\")")
        project.resolve("build.gradle.kts").appendText("\nkotlin { sourceSets.commonMain.dependencies { implementation(\"test.upstream:render\") { version { strictly(\"0.9\") } } } }\n")
        val output = runner(project, "verifyRouting").buildAndFail().output
        assertContains(output, "strict requirement 0.9")
    }

    @Test
    fun rejectedSelectedVersionIsNotSilentlyReplaced() {
        val project = project()
        consumer(project, "mingwX64(\"windows\")")
        project.resolve("build.gradle.kts").appendText("\nkotlin { sourceSets.commonMain.dependencies { implementation(\"test.upstream:render:1.0\") { version { reject(\"1.0\") } } } }\n")
        val output = runner(project, "verifyRouting").buildAndFail().output
        assertContains(output, "rejected-version constraint")
    }

    @Test
    fun maintainerUsesTheRootSelectedVersionInsteadOfThePluginDefault() {
        val project = project()
        project.resolve("settings.gradle.kts").writeText("""
            plugins { id("love.yinlin.native-bridge.settings") }
            rootProject.name = "consumer"
            gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.maintainer", true)
            gradle.extensions.extraProperties.set("love.yinlin.nativeBridge.version.skiko-core", "9.9.9")
            nativeBridgeSources.include("skiko-core", file("port"), "love.yinlin.native.skiko:skiko", "local-port")
        """.trimIndent())
        project.resolve("port").mkdirs()
        project.resolve("port/settings.gradle.kts").writeText("rootProject.name = \"skiko\"\n")
        project.resolve("port/build.gradle.kts").writeText("""
            import org.gradle.api.attributes.Attribute
            import org.gradle.api.attributes.Usage
            plugins { base }
            group = "love.yinlin.native.skiko"
            version = "9.9.9"
            configurations.create("nativeApi") {
                isCanBeConsumed = true
                isCanBeResolved = false
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
                    attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "native")
                    attribute(Attribute.of("org.jetbrains.kotlin.native.target", String::class.java), "mingw_x64")
                }
            }
        """.trimIndent())
        project.resolve("build.gradle.kts").writeText("""
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("love.yinlin.native-bridge")
            }
            repositories { mavenCentral() }
            kotlin {
                mingwX64("windows")
                sourceSets.commonMain.dependencies { implementation("org.jetbrains.skiko:skiko:0.150.1") }
            }
            tasks.register("verifyRootVersion") {
                doLast {
                    check(nativeBridge.redirects.getByName("skiko-core").upstreamVersion.get() == "9.9.9")
                    val compilation = kotlin.targets.getByName("windows").compilations.getByName("main")
                    val result = configurations.getByName(compilation.compileDependencyConfigurationName).incoming.resolutionResult
                    result.allDependencies.forEach { if (it is org.gradle.api.artifacts.result.UnresolvedDependencyResult) throw it.failure }
                    println("ROOT_VERSION_SELECTED " + result.allComponents.joinToString { it.id.displayName })
                }
            }
        """.trimIndent())
        val output = runner(project, "verifyRootVersion").build().output
        assertContains(output.lineSequence().first { it.startsWith("ROOT_VERSION_SELECTED") }, "project ':local-port'")
    }

    @Test
    fun sameBuildProjectRedirectUsesAProjectComponent() {
        val project = project()
        consumer(project, "mingwX64(\"windows\")")
        project.resolve("settings.gradle.kts").appendText("include(\":port\")\n")
        project.resolve("port").mkdirs()
        project.resolve("port/build.gradle.kts").writeText("""
            plugins { id("org.jetbrains.kotlin.multiplatform") }
            group = "test.port"
            version = "1.0-native"
            kotlin { mingwX64() }
        """.trimIndent())
        val build = project.resolve("build.gradle.kts")
        build.writeText(build.readText().replace("module.set(\"test.port:render:1.0-native\")", "projectPath.set(\":port\")"))
        val output = runner(project, "verifyRouting").build().output
        assertContains(output, "project ':port'")
        assertContains(output, "APPLICATION_RUNTIMES=0")
    }

    @Test
    fun nativeOnlyCommonMetadataUsesThePort() {
        val project = project()
        consumer(project, "mingwX64(\"windows\")", extra = commonMetadataProbe)
        val output = runner(project, "verifyCommonRoot").build().output
        assertContains(output.lineSequence().first { it.startsWith("COMMON_ROOT =") }, "test.port:render:1.0-native")
    }

    @Test
    fun macosRedirectIncludesItsCommonMetadata() {
        val project = project()
        consumer(project, "macosArm64(\"mac\")", extra = commonMetadataProbe)
        val build = project.resolve("build.gradle.kts")
        build.writeText(build.readText().replace("targets.set(setOf(\"mingw_x64\"))", "targets.set(setOf(\"macos_arm64\"))"))
        val output = runner(project, "verifyRouting", "verifyCommonRoot").build().output
        assertContains(output.lineSequence().first { it.startsWith("ROUTE mac =") }, "test.port:render:1.0-native")
        assertContains(output.lineSequence().first { it.startsWith("COMMON_ROOT =") }, "test.port:render:1.0-native")
    }

    @Test
    fun mixedJvmNativeCommonMetadataStaysOfficial() {
        val project = project()
        consumer(project, "mingwX64(\"windows\"); jvm(\"desktop\")", extra = commonMetadataProbe)
        val output = runner(project, "verifyCommonRoot").build().output
        assertContains(output.lineSequence().first { it.startsWith("COMMON_ROOT =") }, "test.upstream:render:1.0")
    }

    private val commonMetadataProbe = """
        tasks.register("verifyCommonRoot") {
            doLast {
                val metadata = kotlin.targets.getByName("metadata").compilations.getByName("main")
                val config = configurations.getByName(metadata.compileDependencyConfigurationName)
                val result = config.incoming.resolutionResult
                result.allDependencies.forEach {
                    if (it is org.gradle.api.artifacts.result.UnresolvedDependencyResult) throw it.failure
                }
                println("COMMON_ROOT = " + result.allComponents.joinToString { it.id.displayName })
            }
        }
    """.trimIndent()

    @Test
    fun includedBuildRedirectUsesTheForeignProjectSelector() {
        val project = project()
        consumer(project, "mingwX64(\"windows\")", includePort = false)
        project.resolve("settings.gradle.kts").writeText("""
            plugins { id("love.yinlin.native-bridge.settings") }
            rootProject.name = "consumer"
            gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.maintainer", true)
            nativeBridgeSources.include("render", file("port"), "test.port:render", "local-port")
        """.trimIndent())
        project.resolve("port").mkdirs()
        project.resolve("port/settings.gradle.kts").writeText("rootProject.name = \"render\"\n")
        project.resolve("port/build.gradle.kts").writeText("""
            import org.gradle.api.attributes.Attribute
            import org.gradle.api.attributes.Usage
            plugins { base }
            group = "test.port"
            version = "1.0-native"
            configurations.create("nativeApi") {
                isCanBeConsumed = true
                isCanBeResolved = false
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
                    attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "native")
                    attribute(Attribute.of("org.jetbrains.kotlin.native.target", String::class.java), "mingw_x64")
                }
            }
        """.trimIndent())
        val output = runner(project, "verifyRouting").build().output
        assertContains(output.lineSequence().first { it.startsWith("ROUTE windows =") }, "local-port")
        assertContains(output, "APPLICATION_RUNTIMES=0")
    }

    @Test
    fun oneIncludedBuildCanRedirectItsRootAndAnExtensionProject() {
        val project = project()
        consumer(project, "mingwX64(\"windows\")", includePort = false)
        project.resolve("settings.gradle.kts").writeText("""
            plugins { id("love.yinlin.native-bridge.settings") }
            rootProject.name = "consumer"
            gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.maintainer", true)
            nativeBridgeSources.include(file("port"), "local-port", mapOf(
                "render" to ("test.port:render" to ":"),
                "extension" to ("test.port:extension" to ":extension"),
            ))
        """.trimIndent())
        project.resolve("build.gradle.kts").appendText("""

            kotlin { sourceSets.commonMain.dependencies { implementation("test.upstream:extension:1.0") } }
            nativeBridge.redirect("extension") {
                upstream.set("test.upstream:extension")
                upstreamVersion.set("1.0")
                targets.set(setOf("mingw_x64"))
                module.set("test.port:extension:1.0")
            }
        """.trimIndent())
        project.resolve("port/extension").mkdirs()
        project.resolve("port/settings.gradle.kts").writeText("rootProject.name = \"render\"\ninclude(\":extension\")\n")
        project.resolve("port/build.gradle.kts").writeText("""
            import org.gradle.api.attributes.Attribute
            import org.gradle.api.attributes.Usage
            allprojects {
                group = "test.port"
                version = "1.0"
                configurations.create("nativeApi") {
                    isCanBeConsumed = true
                    isCanBeResolved = false
                    attributes {
                        attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-api"))
                        attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "native")
                        attribute(Attribute.of("org.jetbrains.kotlin.native.target", String::class.java), "mingw_x64")
                    }
                }
            }
        """.trimIndent())
        val output = runner(project, "verifyRouting").build().output
        val route = output.lineSequence().first { it.startsWith("ROUTE windows =") }
        assertContains(route, "project ':local-port'")
        assertContains(route, "project ':local-port:extension'")
        assertFalse(route.contains("test.port:render:"))
        assertFalse(route.contains("test.port:extension:"))
    }

    @Test
    fun stageAndZipRunTheSameNativeExecutableAndRuntimeFiles() {
        val project = project()
        // This compiles a tiny real native executable, independently of Skiko's C++ prerequisites.
        val windows = System.getProperty("os.name").startsWith("Windows")
        val macos = System.getProperty("os.name") == "Mac OS X"
        val target = if (windows) "mingwX64" else if (macos) "macosArm64" else "linuxX64"
        val konan = if (windows) "mingw_x64" else if (macos) "macos_arm64" else "linux_x64"
        project.resolve("gradle.properties").writeText("kotlin.native.ignoreDisabledTargets=true\n")
        project.resolve("runtime.txt").writeText("runtime fixture\n")
        project.resolve("build.gradle.kts").writeText("""
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("love.yinlin.native-bridge")
            }
            repositories { mavenCentral() }
            kotlin { $target("host") }
            nativeApplication {
                enabled.set(true)
                baseName.set("native-probe")
                runtime("$konan") { files.from("runtime.txt") }
            }
        """.trimIndent())
        val source = project.resolve("src/commonMain/kotlin/Main.kt")
        source.parentFile.mkdirs()
        source.writeText("fun main() { println(\"NATIVE_BRIDGE_EXECUTED\") }\n")
        val output = runner(project, "runHostDebugExecutable", "packageHostDebugExecutable").build().output
        assertContains(output, "NATIVE_BRIDGE_EXECUTED")
        val stage = project.resolve("build/nativeApplication/host/debugExecutable")
        assertTrue(stage.resolve("runtime.txt").isFile)
        val exe = stage.resolve(if (windows) "native-probe.exe" else "native-probe.kexe")
        assertTrue(exe.isFile)
        if (!windows) assertTrue(exe.canExecute())
        val archive = project.resolve("build/distributions").listFiles()!!.single { it.extension == "zip" }
        java.util.zip.ZipFile(archive).use { zip ->
            assertTrue(zip.getInputStream(zip.getEntry(exe.name)).readBytes().contentEquals(exe.readBytes()))
            assertTrue(zip.getInputStream(zip.getEntry("runtime.txt")).readBytes().contentEquals(stage.resolve("runtime.txt").readBytes()))
        }
    }
}
