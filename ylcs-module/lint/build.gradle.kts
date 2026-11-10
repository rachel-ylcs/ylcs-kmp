plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.ktlint.engine)
    implementation(libs.ktlint.standard)
}

val repositoryDirectory = layout.settingsDirectory
val kotlinSources = fileTree(repositoryDirectory) {
    include("ylcs-module/**/*.kt", "ylcs-app/**/*.kt")
    exclude("**/build/**", "**/.gradle/**", "**/cpp/**")
}
val ktlintReport = layout.buildDirectory.file("reports/ktlint/ktlint.md")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "检查 Kotlin 源码规范"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("love.yinlin.lint.RepositoryKtlint")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("kotlin-logging-to-jul", "true")
    systemProperty("kotlin-logging.logStartupMessage", "false")
    inputs.files(kotlinSources).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(ktlintReport)
    outputs.upToDateWhen { false }
    args(repositoryDirectory.asFile.absolutePath, ktlintReport.get().asFile.absolutePath)
    doFirst {
        val sourceList = temporaryDir.resolve("sources.txt")
        val paths = kotlinSources.files.map { it.relativeTo(repositoryDirectory.asFile).invariantSeparatorsPath }.sorted()
        sourceList.writeText(paths.joinToString("\n"))
        args(sourceList.absolutePath)
    }
}
