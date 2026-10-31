import groovy.json.JsonSlurper
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "love.yinlin.native"
version = libs.versions.rachel.get()

kotlin {
    jvmToolchain(25)
    sourceSets.main { kotlin.srcDir("../native-binaries/src/main/kotlin") }
}

val settingsSource = sourceSets.create("settings")
val verificationSource = sourceSets.create("verification")
val composeTestPlugin = configurations.create("composeTestPlugin") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val rachelTestPlugin = configurations.create("rachelTestPlugin") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
sourceSets.main {
    compileClasspath += settingsSource.output
    runtimeClasspath += settingsSource.output
}
sourceSets.test {
    compileClasspath += verificationSource.output
    runtimeClasspath += verificationSource.output
}
kotlin.target.compilations.getByName("test").associateWith(kotlin.target.compilations.getByName("verification"))

dependencies {
    add(settingsSource.compileOnlyConfigurationName, gradleApi())
    add(settingsSource.compileOnlyConfigurationName, gradleKotlinDsl())
    implementation(libs.gradle.plugin.kotlin)
    compileOnly(libs.gradle.plugin.compose)
    add(composeTestPlugin.name, libs.gradle.plugin.compose)
    add(composeTestPlugin.name, libs.gradle.plugin.composeCompiler)
    add(composeTestPlugin.name, project(":native-dokka"))
    add(rachelTestPlugin.name, project(":public-plugin"))
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
    add(verificationSource.implementationConfigurationName, localGroovy())
    add(verificationSource.implementationConfigurationName, kotlin("stdlib"))
}

for ((taskName, command) in mapOf("checkNativeSources" to "sources", "verifyNativeHost" to "host", "assembleNativeRepository" to "assemble")) {
    tasks.register<JavaExec>(taskName) {
        group = "verification"
        description = "Verify Native Compose $command using maintained Kotlin checks"
        classpath = verificationSource.runtimeClasspath
        mainClass.set("love.yinlin.nativebridge.verification.NativeVerification")
        args(command, rootProject.file("../native-compose").absolutePath)
    }
}

val writeCompatibility = tasks.register<WriteProperties>("writeCompatibility") {
    destinationFile.set(layout.buildDirectory.file("generated/compatibility/rachel-native-bridge.properties"))
    property("skiko.upstream.version", libs.versions.skiko.get())
    val nativeSkiko = libs.native.skiko.asProvider().get()
    property("skiko.port.module", "${nativeSkiko.module.group}:${nativeSkiko.module.name}:${nativeSkiko.versionConstraint.requiredVersion}")
    property("skiko.skottie.module", libs.native.skiko.skottie.get().toString())
    property("sdl.version", libs.versions.sdl.get())
    val composeLock = rootProject.file("../native-compose/upstream/compose.json")
    inputs.file(composeLock)
    val compose = JsonSlurper().parse(composeLock) as Map<*, *>
    val composeModules = compose["modules"] as Map<*, *>
    val catalog = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    property("compose.upstream.version", catalog.findVersion("cmp").get().requiredVersion)
    property("compose.ui.modules", composeModules.keys.joinToString(","))
    for ((artifact, value) in composeModules) {
        val module = value as Map<*, *>
        property("compose.$artifact.upstream", "${module["upstreamGroup"] ?: "org.jetbrains.compose.${module["family"] ?: "ui"}"}:$artifact")
        property("compose.$artifact.upstream.version", catalog.findVersion((module["versionRef"] ?: "cmp") as String).get().requiredVersion)
        property("compose.$artifact.module", catalog.findLibrary(module["alias"] as String).get().get().toString())
        property("compose.$artifact.targets", (module["redirectTargets"] as List<*>).joinToString(","))
        property("compose.$artifact.metadata.targets", (module["metadataTargets"] as List<*>).joinToString(","))
    }
}
tasks.processResources { from(writeCompatibility) }

val settingsJar = tasks.register<Jar>("settingsJar") {
    description = "Lightweight Settings entry without the Kotlin Gradle Plugin runtime"
    archiveClassifier.set("settings")
    from(settingsSource.output)
    from(tasks.pluginDescriptors) {
        include("${libs.plugins.nativeBridgeSettings.get().pluginId}.properties")
        into("META-INF/gradle-plugins")
    }
}
configurations.create("settingsElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    outgoing.artifact(settingsJar)
}
tasks.jar { from(settingsSource.output) }
tasks.pluginUnderTestMetadata { pluginClasspath.from(settingsSource.output, composeTestPlugin) }

gradlePlugin {
    plugins {
        create("nativeBridge") {
            id = libs.plugins.nativeBridge.get().pluginId
            implementationClass = "love.yinlin.nativebridge.NativeBridgePlugin"
            displayName = "Rachel Native Bridge"
            description = "Target-specific Native dependency routing and executable distributions"
        }
        create("nativeBridgeSettings") {
            id = libs.plugins.nativeBridgeSettings.get().pluginId
            implementationClass = "love.yinlin.nativebridge.NativeBridgeSettingsPlugin"
            displayName = "Rachel Native Bridge Sources"
            description = "Connect Native source builds during Settings evaluation"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "LocalVerification"
            url = uri(rootProject.layout.buildDirectory.dir("native-bridge-repository"))
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    doFirst {
        check(repository.url.scheme == "file") { "Native Bridge remote Maven publishing is disabled; use the local verification repository" }
    }
}

// Both entry IDs share one implementation GAV. Settings selects a small
// classifier and excludes KGP, preserving the repository bootstrap boundary.
afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") { artifact(settingsJar) }
    publishing.publications.named<MavenPublication>("nativeBridgeSettingsPluginMarkerMaven") {
        pom.withXml {
            val dependencies = asNode().get("dependencies") as groovy.util.NodeList
            for (dependency in dependencies.flatMap { (it as groovy.util.Node).children() }.filterIsInstance<groovy.util.Node>()) {
                dependency.appendNode("classifier", "settings")
                val exclusion = dependency.appendNode("exclusions").appendNode("exclusion")
                exclusion.appendNode("groupId", "*")
                exclusion.appendNode("artifactId", "*")
            }
        }
    }
}

val hostPackagingTest = "love.yinlin.nativebridge.NativeBridgeFunctionalTest.hostComposeTasksRunBothBuildTypesAndPrepareAnUnpackedApplication"

tasks.register<Test>("hostTest") {
    group = "verification"
    description = "Run the host Native executable and distribution regression"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching(hostPackagingTest) }
    shouldRunAfter(tasks.test)
}

tasks.test { filter { excludeTestsMatching(hostPackagingTest) } }

tasks.withType<Test>().configureEach {
    useJUnit()
    inputs.files(rachelTestPlugin)
    doFirst { systemProperty("native.tests.rachelClasspath", rachelTestPlugin.asPath) }
    systemProperty("native.tests.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
    systemProperty("native.tests.work", layout.buildDirectory.dir("functional-tests").get().asFile.absolutePath)
    systemProperty("native.tests.skikoLibraryScript", layout.projectDirectory.file("gradle/skiko-library-tests.gradle.kts").asFile.absolutePath)
    inputs.file(layout.projectDirectory.file("gradle/skiko-library-tests.gradle.kts"))
    inputs.file(rootProject.file("../native-compose/gradle/templates/compose-ui.build.gradle.kts.template"))
    systemProperty("native.tests.kotlinVersion", libs.versions.kotlin.get())
    systemProperty("native.tests.composeVersion", libs.versions.cmp.get())
    systemProperty("native.tests.composeUiTemplate", rootProject.file("../native-compose/gradle/templates/compose-ui.build.gradle.kts.template").absolutePath)
    systemProperty("native.tests.versionCatalog", rootProject.file("../libs.versions.toml").absolutePath)
    maxParallelForks = 1
}
