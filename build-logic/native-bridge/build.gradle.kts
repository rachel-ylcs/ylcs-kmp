import groovy.json.JsonSlurper
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("org.jetbrains.kotlin.jvm")
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
sourceSets.main {
    compileClasspath += settingsSource.output
    runtimeClasspath += settingsSource.output
}

dependencies {
    add(settingsSource.compileOnlyConfigurationName, gradleApi())
    add(settingsSource.compileOnlyConfigurationName, gradleKotlinDsl())
    implementation(libs.gradle.plugin.kotlin)
    compileOnly(libs.gradle.plugin.compose)
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
        if (command != "sources") dependsOn(":repository-conventions:prepareNativeSources")
    }
}

val writeCompatibility = tasks.register<WriteProperties>("writeCompatibility") {
    destinationFile.set(layout.buildDirectory.file("generated/compatibility/rachel-native-bridge.properties"))
    property("skiko.upstream.version", libs.versions.skiko.get())
    val portVersion = "${libs.versions.rachel.get()}-cmp-${libs.versions.cmp.get()}"
    property("skiko.port.module", "love.yinlin.native.skiko:skiko:$portVersion")
    property("skiko.skottie.module", "love.yinlin.native.skiko:skiko-skottie:$portVersion")
    property("sdl.version", libs.versions.sdl.get())
    val composeSpec = rootProject.file("../native-compose/upstream/compose.json")
    inputs.file(composeSpec)
    val compose = JsonSlurper().parse(composeSpec) as Map<*, *>
    val composeModules = compose["modules"] as Map<*, *>
    val catalog = project.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")
    property("compose.upstream.version", catalog.findVersion("cmp").get().requiredVersion)
    property("compose.ui.modules", composeModules.keys.joinToString(","))
    for ((artifact, value) in composeModules) {
        val module = value as Map<*, *>
        val upstreamGroup = (module["upstreamGroup"] ?: "org.jetbrains.compose.${module["family"] ?: "ui"}") as String
        property("compose.$artifact.upstream", "$upstreamGroup:$artifact")
        property("compose.$artifact.upstream.version", catalog.findVersion((module["versionRef"] ?: "cmp") as String).get().requiredVersion)
        property("compose.$artifact.module", "love.yinlin.native.${upstreamGroup.removePrefix("org.jetbrains.")}:$artifact:$portVersion")
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

tasks.withType<Jar>().matching { it.name == "sourcesJar" }.configureEach { from(settingsSource.allSource) }

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
