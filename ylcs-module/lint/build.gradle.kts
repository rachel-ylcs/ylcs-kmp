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
val ktlintReports = layout.buildDirectory.dir("reports/ktlint")
val xlsxWriter = layout.projectDirectory.file("write_ktlint_xlsx.py")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "检查 Kotlin 源码规范"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("love.yinlin.lint.RepositoryKtlint")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("kotlin-logging-to-jul", "true")
    systemProperty("kotlin-logging.logStartupMessage", "false")
    inputs.files(kotlinSources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(xlsxWriter).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(ktlintReports)
    outputs.upToDateWhen { false }
    args(repositoryDirectory.asFile.absolutePath, ktlintReports.get().asFile.absolutePath, xlsxWriter.asFile.absolutePath)
    doFirst {
        val sourceList = temporaryDir.resolve("sources.txt")
        val paths = kotlinSources.files.map { it.relativeTo(repositoryDirectory.asFile).invariantSeparatorsPath }.sorted()
        sourceList.writeText(paths.joinToString("\n"))
        args(sourceList.absolutePath)
    }
}
