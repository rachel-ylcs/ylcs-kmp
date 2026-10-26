plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "love.yinlin"
version = libs.versions.rachel.get()

dependencies {
    implementation(project(path = ":public-plugin", configuration = "bootstrapElements"))
    implementation(project(path = ":native-bridge", configuration = "settingsElements"))
    testImplementation(kotlin("test-junit"))
}

val nativeSourcesDirectory = rootProject.layout.projectDirectory.dir("../native-compose")
for ((taskName, command) in mapOf("prepareNativeSources" to "prepare", "checkNativeUpstreamUpdates" to "updates", "restoreNativeSourceCache" to "cache-restore", "createNativeSourceCache" to "cache-create")) {
    tasks.register<JavaExec>(taskName) {
        group = "native setup"
        description = "${command.replace('-', ' ')} the locked Native Compose sources"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("love.yinlin.repository.nativecompose.NativeSourcesMain")
        args(command, nativeSourcesDirectory.asFile.absolutePath)
        if (command == "prepare" && gradle.startParameter.isOffline) args("offline")
        if (command == "prepare") mustRunAfter("restoreNativeSourceCache")
        if (command == "cache-create") mustRunAfter("prepareNativeSources")
    }
}

kotlin {
    jvmToolchain(25)
}

val versionResources = layout.buildDirectory.dir("generated/versionResources")
val repositoryVersions = tasks.register<WriteProperties>("repositoryVersions") {
    description = "生成仓库版本号信息"
    destinationFile = versionResources.map { it.file("rachel-repository-versions.properties") }
    property("current", libs.versions.rachel)
    property("code", libs.versions.rachelCode)
    property("minimum", libs.versions.rachelMinimum)
    property("minimumCode", libs.versions.rachelMinimumCode)
}

sourceSets.main {
    resources.srcDir(repositoryVersions.map { it.destinationFile.get().asFile.parentFile })
}

gradlePlugin {
    plugins {
        create("repository") {
            id = libs.plugins.repository.get().pluginId
            implementationClass = "love.yinlin.repository.RepositoryPlugin"
            displayName = "Rachel repository conventions"
            description = "Local shared Rachel and YLCS build configuration"
        }
    }
}
