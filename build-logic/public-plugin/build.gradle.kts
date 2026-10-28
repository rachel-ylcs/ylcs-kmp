plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "love.yinlin"
version = libs.versions.rachel.get()

val bootstrap = sourceSets.create("bootstrap")
sourceSets.main {
    compileClasspath += bootstrap.output
    runtimeClasspath += bootstrap.output
}
sourceSets.test {
    compileClasspath += bootstrap.output
    runtimeClasspath += bootstrap.output
}

dependencies {
    add(bootstrap.compileOnlyConfigurationName, gradleApi())
    add(bootstrap.compileOnlyConfigurationName, gradleKotlinDsl())
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.android)
    implementation(libs.gradle.plugin.compose)
    implementation(libs.gradle.plugin.composeCompiler)
    implementation(libs.gradle.plugin.serialization)
    implementation(libs.gradle.plugin.mavenPublish)
    implementation(libs.gradle.plugin.dokka)
    compileOnly(project(":native-bridge"))
    testImplementation(kotlin("test-junit"))
}

kotlin {
    jvmToolchain(25)
    // Embed the shared binary tooling so plain Native projects do not need the Compose bridge.
    sourceSets.main { kotlin.srcDir("../native-binaries/src/main/kotlin") }
    sourceSets.test { kotlin.srcDir("../native-binaries/src/test/kotlin") }
}

val bootstrapJar = tasks.register<Jar>("bootstrapJar") {
    description = "编译构建启动器"
    archiveClassifier = bootstrap.name
    from(bootstrap.output)
}

configurations.create("bootstrapElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    outgoing.artifact(bootstrapJar)
}

tasks.jar {
    from(bootstrap.output)
}

gradlePlugin {
    plugins {
        create("rachel") {
            id = libs.plugins.rachel.get().pluginId
            implementationClass = "love.yinlin.gradle.RachelPlugin"
            displayName = "Rachel build templates"
            description = "Kotlin Multiplatform, application and CMake build templates"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "localPluginRepository"
            url = layout.buildDirectory.dir("plugin-repository").get().asFile.toURI()
        }
    }
}
