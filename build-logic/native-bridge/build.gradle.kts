import groovy.json.JsonSlurper

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "love.yinlin.native"
version = libs.versions.rachel.get()

kotlin { jvmToolchain(25) }

val settingsPluginTestRuntime = configurations.create("settingsPluginTestRuntime") {
    isCanBeConsumed = false
}

dependencies {
    implementation(libs.gradle.plugin.kotlin)
    testImplementation(gradleTestKit())
    add(settingsPluginTestRuntime.name, project(":native-bridge-settings"))
    testImplementation(kotlin("test-junit"))
}

val compatibility = JsonSlurper().parse(file("../../native-compose/upstream/lock.json")) as Map<*, *>
val writeCompatibility = tasks.register<WriteProperties>("writeCompatibility") {
    destinationFile.set(layout.buildDirectory.file("generated/compatibility/rachel-native-bridge.properties"))
    property("skiko.upstream.version", libs.versions.skiko.get())
    property("skiko.port.module", "${compatibility["group"]}:skiko:${compatibility["portVersion"]}")
}
tasks.processResources { from(writeCompatibility) }

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(settingsPluginTestRuntime)
}

gradlePlugin {
    plugins {
        create("nativeBridge") {
            id = libs.plugins.nativeBridge.get().pluginId
            implementationClass = "love.yinlin.nativebridge.NativeBridgePlugin"
            displayName = "Rachel Native Bridge"
            description = "Target-specific Native dependency routing and executable distributions"
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

tasks.test {
    useJUnit()
    systemProperty("native.tests.gradleUserHome", gradle.gradleUserHomeDir.absolutePath)
    systemProperty("native.tests.work", layout.buildDirectory.dir("functional-tests").get().asFile.absolutePath)
    maxParallelForks = 1
}
