plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
}

group = "love.yinlin"
version = libs.versions.rachel.get()

dependencies {
    implementation(kotlin("stdlib"))
    implementation(localGroovy())
    implementation(project(path = ":public-plugin", configuration = "bootstrapElements"))
    implementation(project(path = ":native-bridge", configuration = "settingsElements"))
}

val nativeSourcesDirectory = rootProject.layout.projectDirectory.dir("../native-compose")
tasks.register<JavaExec>("writeNativeCiInputs") {
    group = "native setup"
    description = "Resolve the Native cache identity and relevant catalog changes without preparing upstream sources"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("love.yinlin.repository.nativecompose.NativeCiInputs")
    args(rootProject.projectDir.parentFile.absolutePath, layout.buildDirectory.file("native-ci/inputs.properties").get().asFile.absolutePath, providers.gradleProperty("native.ci.baseCatalog").getOrElse(""))
}

tasks.register<JavaExec>("prepareNativeSources") {
    group = "native setup"
    description = "Resolve the version catalog and prepare isolated Native Compose sources"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("love.yinlin.repository.nativecompose.NativeSourcesMain")
    args(nativeSourcesDirectory.asFile.absolutePath)
    if (gradle.startParameter.isOffline) args("offline")
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
