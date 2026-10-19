plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "love.yinlin"
version = libs.versions.rachel.get()

// The settings plugin needs these types before project plugin requests are resolved.
// Keep this internal artifact free of KGP/AGP/Compose implementation dependencies.
val bootstrap = sourceSets.create("bootstrap")
sourceSets.main {
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
}

kotlin {
    jvmToolchain(25)
}

val bootstrapJar = tasks.register<Jar>("bootstrapJar") {
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
