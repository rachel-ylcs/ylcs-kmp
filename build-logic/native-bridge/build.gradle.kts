import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "love.yinlin.native"
version = libs.versions.rachel.get()

kotlin { jvmToolchain(25) }

val settingsSource = sourceSets.create("settings")
sourceSets.main {
    compileClasspath += settingsSource.output
    runtimeClasspath += settingsSource.output
}

dependencies {
    add(settingsSource.compileOnlyConfigurationName, gradleApi())
    add(settingsSource.compileOnlyConfigurationName, gradleKotlinDsl())
    implementation(libs.gradle.plugin.kotlin)
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
}

val writeCompatibility = tasks.register<WriteProperties>("writeCompatibility") {
    destinationFile.set(layout.buildDirectory.file("generated/compatibility/rachel-native-bridge.properties"))
    property("skiko.upstream.version", libs.versions.skiko.get())
    val nativeSkiko = libs.skikoNative.get()
    property("skiko.port.module", "${nativeSkiko.module.group}:${nativeSkiko.module.name}:${nativeSkiko.versionConstraint.requiredVersion}")
    property("skiko.skottie.module", libs.skikoNativeSkottie.get().toString())
    property("sdl.version", libs.versions.sdl.get())
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
tasks.pluginUnderTestMetadata { pluginClasspath.from(settingsSource.output) }

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

tasks.test {
    useJUnit()
    systemProperty("native.tests.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
    systemProperty("native.tests.work", layout.buildDirectory.dir("functional-tests").get().asFile.absolutePath)
    maxParallelForks = 1
}
