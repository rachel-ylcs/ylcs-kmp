package love.yinlin.nativebridge

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.file.attribute.PosixFilePermissions
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeBridgeFunctionalTest {
    @Test
    fun nativeUiTemplateKeepsKonanCoordinatesAfterKgpCreatesPublications() {
        val repository = project()
        repository.resolve("libs.versions.toml").writeText(File(System.getProperty("native.tests.versionCatalog")).readText())
        val native = repository.resolve("native-compose")
        native.resolve("upstream").mkdirs()
        native.resolve("upstream/compose.json").writeText("""
            {"group":"test.native.ui","ciModules":["ui-util"],"modules":{"ui-util":{"sourceSets":{"commonMain":["src/commonMain/kotlin"]}}}}
        """.trimIndent())
        native.resolve("upstream/compose-origins.json").writeText("""{"files":{}}""")
        val build = native.resolve("vendor/compose/workspace/native-desktop").apply { mkdirs() }
        build.parentFile.resolve("LICENSE.txt").writeText("fixture source license\n")
        build.resolve("gradle.properties").writeText("kotlin.stdlib.default.dependency=false\nkotlin.native.ignoreDisabledTargets=true\nkotlin.mpp.applyDefaultHierarchyTemplate=false\n")
        build.resolve("settings.gradle.kts").writeText("""
            pluginManagement {
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                versionCatalogs { create("libs") { from(files("../../../../../libs.versions.toml")) } }
            }
            rootProject.name = "native-ui-publication-fixture"
            include(":ui-util")
        """.trimIndent())
        build.resolve("ui-util").mkdirs()
        build.resolve("ui-util/src/commonMain/kotlin").mkdirs()
        build.resolve("ui-util/src/commonMain/kotlin/Source.kt").writeText("package fixture\nclass Source\n")
        build.resolve("ui-util/build.gradle.kts").writeText("""
            plugins {
                kotlin("multiplatform")
                `maven-publish`
            }
        """.trimIndent())
        val template = File(System.getProperty("native.tests.composeUiTemplate")).readText().replace("__JAVA_VERSION__", "25")
        build.resolve("build.gradle.kts").writeText(template + "\n" + """
            tasks.register("verifyNativePublicationCoordinates") {
                dependsOn(":ui-util:" + hostTarget + "SourcesJar")
                doLast {
                    val library = project(":ui-util")
                    check(library.group == "org.jetbrains.compose.ui")
                    val publications = library.extensions.getByType<PublishingExtension>().publications
                    val expected = mapOf("kotlinMultiplatform" to "ui-util", "windows" to "ui-util-mingwx64", "linux" to "ui-util-linuxx64", "macos" to "ui-util-macosarm64")
                    for ((name, artifact) in expected) {
                        val publication = publications.getByName(name) as MavenPublication
                        check(publication.groupId == "test.native.ui")
                        check(publication.artifactId == artifact) { name + " still uses " + publication.artifactId }
                        println("NATIVE_UI_PUBLICATION=" + artifact)
                    }
                    library.extensions.getByType<KotlinMultiplatformExtension>().targets.withType<KotlinNativeTarget>().forEach { target ->
                        check(target.compilations.getByName("main").compileTaskProvider.get().compilerOptions.moduleName.get() == "org.jetbrains.compose.ui:ui-util")
                    }
                    val sources = library.tasks.getByName(hostTarget + "SourcesJar") as Jar
                    java.util.zip.ZipFile(sources.archiveFile.get().asFile).use { archive ->
                        check(archive.getEntry("META-INF/licenses/LICENSE.txt") != null)
                        check(archive.getEntry("META-INF/provenance/compose.json") != null)
                        check(archive.getEntry("META-INF/provenance/compose-origins.json") != null)
                    }
                    println("NATIVE_UI_SOURCE_NOTICES=VERIFIED")
                }
            }
        """.trimIndent())
        val result = runner(build, "verifyNativePublicationCoordinates").build()
        for (artifact in listOf("ui-util", "ui-util-mingwx64", "ui-util-linuxx64", "ui-util-macosarm64")) assertContains(result.output, "NATIVE_UI_PUBLICATION=$artifact")
        assertContains(result.output, "NATIVE_UI_SOURCE_NOTICES=VERIFIED")
        assertTrue(result.tasks.none { it.path.contains("compileKotlin") || it.path.contains("linkDebug") || it.path.contains("linkRelease") })
    }

    @Test
    fun maintainedLibraryTestScriptConfiguresWithoutExecutingNativeCompilations() {
        val root = project()
        val native = root.resolve("native-compose")
        native.resolve("upstream").mkdirs()
        native.resolve("upstream/lock.json").writeText("""{"skia":"fixture"}""")
        val library = native.resolve("adaptation/test.library").apply { mkdirs() }
        library.resolve("settings.gradle.kts").writeText("rootProject.name = \"library-test-configuration\"\n")
        library.resolve("gradle.properties").writeText("kotlin.native.ignoreDisabledTargets=true\n")
        library.resolve("buildSrc").mkdirs()
        library.resolve("buildSrc/build.gradle.kts").writeText("""
            plugins { `java-library` }
            repositories { mavenCentral() }
            dependencies { implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${System.getProperty("native.tests.kotlinVersion")}") }
        """.trimIndent())
        val script = File(System.getProperty("native.tests.skikoLibraryScript")).invariantSeparatorsPath
        library.resolve("build.gradle.kts").writeText("""
            import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
            import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
            import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
            plugins { id("org.jetbrains.kotlin.multiplatform") }
            repositories { mavenCentral() }
            kotlin {
                mingwX64()
                linuxX64()
                macosArm64()
            }
            tasks.register("unzipSkiaReleaseWindowsX64")
            apply(from = "$script")
            tasks.register("verifyNativeTestConfiguration") {
                doLast {
                    kotlin.targets.withType<KotlinNativeTargetWithHostTests>().forEach { target ->
                        val run = target.testRuns.getByName("regression")
                        check(run.executionSource.compilation.name == "regression")
                        check(target.binaries.getTest("regression", NativeBuildType.DEBUG).compilation == target.compilations.getByName("regression"))
                        check(tasks.getByName(target.name + "RegressionTest") is KotlinNativeTest)
                        check(kotlin.sourceSets.getByName("nativeRegressionTest").dependsOn.contains(kotlin.sourceSets.getByName("commonRegression")))
                        println("CONFIGURED_LIBRARY_TEST=" + target.name)
                    }
                }
            }
        """.trimIndent())
        val output = runner(library, "verifyNativeTestConfiguration").build().output
        for (target in listOf("mingwX64", "linuxX64", "macosArm64")) assertContains(output, "CONFIGURED_LIBRARY_TEST=$target")
        assertFalse(output.contains("Missing 'dependsOn' in Source Sets"))
        assertFalse(output.lineSequence().any { it.startsWith("> Task :compileKotlin") || it.startsWith("> Task :link") })
    }

    private fun project(): File {
        val work = File(System.getProperty("native.tests.work")).apply { mkdirs() }
        return Files.createTempDirectory(work.toPath(), "bridge-").toFile().also {
            it.resolve("settings.gradle.kts").writeText("rootProject.name = \"consumer\"\n")
            it.resolve("gradle.properties").writeText("kotlin.stdlib.default.dependency=false\nkotlin.native.ignoreDisabledTargets=true\nkotlin.mpp.applyDefaultHierarchyTemplate=false\n")
        }
    }

    private fun runner(directory: File, vararg tasks: String) = GradleRunner.create()
        .withProjectDir(directory)
        .withPluginClasspath()
        .withArguments(*tasks, "--stacktrace", "--console=plain", "--gradle-user-home", System.getProperty("native.tests.gradleUserHome"))

    private fun module(directory: File, group: String, version: String, dependencies: String = "", artifact: String = "render") {
        val module = directory.resolve("repo/${group.replace('.', '/')}/$artifact/$version").apply { mkdirs() }
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
        module.resolve("$artifact-$version.module").writeText("""{"formatVersion":"1.1","component":{"group":"$group","module":"$artifact","version":"$version"},"variants":[$variants]}""")
        module.resolve("$artifact-$version.pom").writeText("""<project><modelVersion>4.0.0</modelVersion><groupId>$group</groupId><artifactId>$artifact</artifactId><version>$version</version></project>""")
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
    fun builtInComposeRulesKeepTheNativeBackendGraphTogetherAndJvmOfficial() {
        val project = project()
        val compatibility = Properties().apply {
            checkNotNull(NativeBridgePlugin::class.java.classLoader.getResourceAsStream("rachel-native-bridge.properties")).use { load(it) }
        }
        val version = compatibility.getProperty("compose.upstream.version")
        val artifacts = listOf("ui-util", "ui-geometry", "ui-unit", "ui-graphics", "ui-text", "ui-skiko", "ui-backhandler", "ui")
        for (artifact in artifacts) {
            module(project, "org.jetbrains.compose.ui", version, artifact = artifact)
            val children = when (artifact) {
                "ui" -> listOf("ui-geometry", "ui-graphics", "ui-text", "ui-unit", "ui-util", "ui-skiko", "ui-backhandler")
                "ui-skiko" -> listOf("ui-graphics", "ui-text")
                "ui-text" -> listOf("ui-graphics", "ui-unit", "ui-util")
                "ui-graphics" -> listOf("ui-unit")
                "ui-unit" -> listOf("ui-geometry")
                else -> emptyList()
            }
            val dependencies = children.joinToString(",") { """{"group":"love.yinlin.native.compose.ui","module":"$it","version":{"requires":"$version"}}""" }
            module(project, "love.yinlin.native.compose.ui", version, dependencies = dependencies, artifact = artifact)
        }
        val dependencies = artifacts.joinToString("\n") { "implementation(\"org.jetbrains.compose.ui:$it:$version\")" }
        project.resolve("build.gradle.kts").writeText("""
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("love.yinlin.native-bridge")
            }
            repositories {
                maven { url = uri("repo"); metadataSources { gradleMetadata(); mavenPom() } }
            }
            kotlin {
                mingwX64("windows")
                linuxX64("linux")
                macosArm64("macos")
                jvm("desktop")
                sourceSets.commonMain.dependencies {
                    $dependencies
                }
            }
            tasks.register("verifyComposeRouting") {
                doLast {
                    kotlin.targets.filter { it.name != "metadata" }.forEach { target ->
                        val config = configurations.getByName(target.compilations.getByName("main").compileDependencyConfigurationName)
                        val result = config.incoming.resolutionResult
                        result.allDependencies.filterIsInstance<org.gradle.api.artifacts.result.UnresolvedDependencyResult>().forEach { throw it.failure }
                        println("COMPOSE_ROUTE " + target.name + " = " + result.allComponents.joinToString { it.id.displayName })
                    }
                }
            }
        """.trimIndent())
        val output = runner(project, "verifyComposeRouting").build().output
        for (target in listOf("windows", "linux", "macos", "desktop")) {
            val line = output.lineSequence().first { it.startsWith("COMPOSE_ROUTE $target =") }
            val group = if (target == "desktop") "org.jetbrains.compose.ui" else "love.yinlin.native.compose.ui"
            for (artifact in artifacts) assertContains(line, "$group:$artifact:$version")
            if (target == "desktop") assertFalse(line.contains("love.yinlin.native.compose.ui"))
            else assertFalse(line.contains("org.jetbrains.compose.ui"))
        }
    }

    @Test
    fun jvmOnlyNeverResolvesPortOrCreatesRuntimeConfiguration() {
        val project = project()
        consumer(project, "jvm(\"desktop\")", includePort = false, extra = """
            nativeBridge { skikoTests() }
            tasks.named("verifyRouting") {
                doLast { check(configurations.findByName("skikoNativeTestRuntime") == null) }
            }
        """.trimIndent())
        val output = runner(project, "verifyRouting").build().output
        assertContains(output, "test.upstream:render:1.0")
        assertFalse(output.contains("test.port:render:1.0-native"))
        assertContains(output, "APPLICATION_RUNTIMES=0")
    }

    @Test
    fun skikoTestsStageOnlyHostDataAndKeepLibraryBinariesUntouched() {
        val project = project()
        val compatibility = Properties().apply {
            checkNotNull(NativeBridgePlugin::class.java.classLoader.getResourceAsStream("rachel-native-bridge.properties")).use { load(it) }
        }
        val coordinate = compatibility.getProperty("skiko.port.module").split(':')
        val artifact = coordinate[1] + "-mingwx64"
        module(project, coordinate[0], coordinate[2], artifact = artifact)
        val runtime = project.resolve("repo/${coordinate[0].replace('.', '/')}/$artifact/${coordinate[2]}")
        runtime.resolve("$artifact-${coordinate[2]}-icudtl.dat").writeText("fixture ICU data")
        project.resolve("fixture-test").mkdirs()
        project.resolve("fixture-test/unused.exe").writeText("configuration fixture; never executed")
        consumer(project, "mingwX64(\"windows\"); linuxX64(\"linux\"); macosArm64(\"macos\")", extra = """
            nativeBridge { skikoTests() }
            tasks.named<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>("windowsTest") {
                executable = file("fixture-test/unused.exe")
            }
            tasks.register("verifySkikoTestRuntime") {
                if (System.getProperty("os.name").startsWith("Windows")) dependsOn("stageWindowsTestSkikoRuntime")
                doLast {
                    kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().forEach { target ->
                        check(target.binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Executable>().isEmpty())
                    }
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        check(file("fixture-test/icudtl.dat").readText() == "fixture ICU data")
                    }
                    else check(configurations.findByName("skikoNativeTestRuntime") == null)
                    println("SKIKO_TEST_RUNTIME_VERIFIED")
                }
            }
        """.trimIndent())
        val output = runner(project, "verifySkikoTestRuntime").build().output
        assertContains(output, "SKIKO_TEST_RUNTIME_VERIFIED")
        assertFalse(output.lineSequence().any { it.startsWith("> Task :compileKotlin") || it.startsWith("> Task :link") })
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
        consumer(project, "mingwX64(\"windows\"); jvm(\"desktop\")", includePort = false)
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
            group = "test.upstream"
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
        val desktop = output.lineSequence().first { it.startsWith("ROUTE desktop =") }
        assertContains(desktop, "test.upstream:render:1.0")
        assertFalse(desktop.contains("local-port"))
        assertContains(output, "APPLICATION_RUNTIMES=0")
    }

    @Test
    fun duplicateSourceBuildRegistrationKeepsIncludedConsumerRoutingLocal() {
        val project = project()
        consumer(project, "mingwX64(\"windows\"); jvm(\"desktop\")", includePort = false)
        project.resolve("settings.gradle.kts").writeText("""
            plugins { id("love.yinlin.native-bridge.settings") }
            rootProject.name = "consumer"
            gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.maintainer", true)
            nativeBridgeSources.include("render", file("port"), "test.port:render", "local-port")
            includeBuild("ui") { name = "local-ui" }
        """.trimIndent())
        val port = project.resolve("port").apply { mkdirs() }
        port.resolve("settings.gradle.kts").writeText("rootProject.name = \"render\"\n")
        port.resolve("build.gradle.kts").writeText("""
            import org.gradle.api.attributes.Attribute
            import org.gradle.api.attributes.Usage
            plugins { base }
            group = "test.upstream"
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
        val ui = project.resolve("ui").apply { mkdirs() }
        consumer(ui, "mingwX64(\"windows\"); jvm(\"desktop\")", includePort = false)
        ui.resolve("gradle.properties").writeText(project.resolve("gradle.properties").readText())
        ui.resolve("settings.gradle.kts").writeText("""
            plugins { id("love.yinlin.native-bridge.settings") }
            rootProject.name = "ui-consumer"
            gradle.extensions.extraProperties.set("love.yinlin.nativeCompose.maintainer", true)
            nativeBridgeSources.include("render", file("../port"), "test.port:render", "local-port")
        """.trimIndent())
        val output = runner(project, "verifyRouting", ":local-ui:verifyRouting", ":local-ui:nativeBridgeReport").build().output
        val nativeRoutes = output.lineSequence().filter { it.startsWith("ROUTE windows =") }.toList()
        assertEquals(2, nativeRoutes.size)
        nativeRoutes.forEach { assertContains(it, "local-port") }
        val jvmRoutes = output.lineSequence().filter { it.startsWith("ROUTE desktop =") }.toList()
        assertEquals(2, jvmRoutes.size)
        jvmRoutes.forEach {
            assertContains(it, "test.upstream:render:1.0")
            assertFalse(it.contains("local-port"))
        }
        assertContains(output, "included build local-port, project :")
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
        module(project, "test.sidecar", "1.0", artifact = "icudata")
        project.resolve("repo/test/sidecar/icudata/1.0/icudata-1.0-payload.dat").writeText("classified runtime fixture\n")
        project.resolve("gradle.properties").writeText("kotlin.native.ignoreDisabledTargets=true\n")
        project.resolve("runtime.txt").writeText("runtime fixture\n")
        project.resolve("build.gradle.kts").writeText("""
            plugins {
                id("org.jetbrains.kotlin.multiplatform")
                id("love.yinlin.native-bridge")
            }
            repositories {
                maven { url = uri("repo") }
                mavenCentral()
            }
            kotlin { $target("host") }
            nativeApplication {
                enabled.set(true)
                baseName.set("native-probe")
                runtime("$konan") {
                    files.from("runtime.txt")
                    artifacts.put("test.sidecar:icudata:1.0:payload@dat", "payload.dat")
                }
            }
        """.trimIndent())
        val source = project.resolve("src/commonMain/kotlin/Main.kt")
        source.parentFile.mkdirs()
        source.writeText("fun main() { println(\"NATIVE_BRIDGE_EXECUTED\") }\n")
        val output = runner(project, "runHostDebugExecutable", "packageHostDebugExecutable").build().output
        assertContains(output, "NATIVE_BRIDGE_EXECUTED")
        val stage = project.resolve("build/nativeApplication/host/debugExecutable")
        assertTrue(stage.resolve("runtime.txt").isFile)
        assertEquals("classified runtime fixture\n", stage.resolve("payload.dat").readText())
        val exe = stage.resolve(if (windows) "native-probe.exe" else "native-probe.kexe")
        assertTrue(exe.isFile)
        if (!windows) assertTrue(exe.canExecute())
        val archive = project.resolve("build/distributions").listFiles()!!.single { it.extension == "zip" }
        java.util.zip.ZipFile(archive).use { zip ->
            assertTrue(zip.getInputStream(zip.getEntry(exe.name)).readBytes().contentEquals(exe.readBytes()))
            assertTrue(zip.getInputStream(zip.getEntry("runtime.txt")).readBytes().contentEquals(stage.resolve("runtime.txt").readBytes()))
            assertTrue(zip.getInputStream(zip.getEntry("payload.dat")).readBytes().contentEquals(stage.resolve("payload.dat").readBytes()))
        }
        FileSystems.newFileSystem(archive.toPath(), emptyMap<String, Any>()).use { zip ->
            assertEquals(PosixFilePermissions.fromString("rwxr-xr-x"), Files.getAttribute(zip.getPath("/${exe.name}"), "zip:permissions"))
            assertEquals(PosixFilePermissions.fromString("rw-r--r--"), Files.getAttribute(zip.getPath("/runtime.txt"), "zip:permissions"))
        }
    }
}
