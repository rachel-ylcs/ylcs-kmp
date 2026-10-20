plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "love.yinlin"
version = libs.versions.rachel.get()

dependencies {
    implementation(project(path = ":public-plugin", configuration = "bootstrapElements"))
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
