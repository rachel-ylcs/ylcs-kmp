import groovy.json.JsonSlurper
import org.jetbrains.kotlin.konan.target.HostManager

plugins { idea }

val nativeDirectory = layout.projectDirectory
layout.buildDirectory.set(nativeDirectory.dir("vendor/build/native-compose"))
idea.module {
    // Included builds provide their own source roots inside vendor.
    excludeDirs.addAll(files("vendor", ".gradle", ".kotlin").files)
}
val lock = JsonSlurper().parse(file("upstream/lock.json")) as Map<*, *>
val sdlLock = JsonSlurper().parse(file("upstream/sdl.json")) as Map<*, *>
check(libs.versions.sdl.get() == sdlLock["version"]) {
    "SDL version changed in ../libs.versions.toml; review upstream/sdl.json and run prepareNativeSources."
}
val skikoVersion = libs.versions.skiko.get()
val portGroup = lock["group"] as String
check(libs.native.skiko.asProvider().get().toString() == "$portGroup:skiko:$skikoVersion") {
    "Native Skiko coordinates changed in ../libs.versions.toml; review upstream/lock.json."
}

val sourceBuild = gradle.includedBuilds.find { it.name == "skiko" }
val hostPublication = when (HostManager.host.name) {
    "mingw_x64" -> "MingwX64"
    "linux_x64" -> "LinuxX64"
    "macos_arm64" -> "MacosArm64"
    else -> null
}
val hostTarget = when (HostManager.host.name) {
    "mingw_x64" -> "windows"
    "linux_x64" -> "linux"
    "macos_arm64" -> "macos"
    else -> null
}
val composeSource = gradle.includedBuilds.find { it.name == "compose-ui" }
if (sourceBuild != null && composeSource != null && hostTarget != null) {
    val composeLock = JsonSlurper().parse(file("upstream/compose.json")) as Map<*, *>
    val modules = composeLock["modules"] as Map<*, *>
    tasks.register("testHost") {
        group = "verification"
        description = "Run all maintained Native library regressions for this host"
        val skikoTarget = when (hostTarget) {
            "windows" -> "mingwX64"
            "linux" -> "linuxX64"
            else -> "macosArm64"
        }
        dependsOn(sourceBuild.task(":${skikoTarget}RegressionTest"))
        for ((artifact, value) in modules) {
            val module = value as Map<*, *>
            val group = module["upstreamGroup"] ?: "org.jetbrains.compose.${module["family"] ?: "ui"}"
            val sources = nativeDirectory.dir("adaptation/${module["adaptation"] ?: "${group}_$artifact"}/src")
            val tests = fileTree(sources) { include("nativeTest/**/*.kt", "${hostTarget}Test/**/*.kt") }
            if (!tests.isEmpty) dependsOn(composeSource.task(":$artifact:${hostTarget}Test"))
        }
    }
}
if (sourceBuild != null && hostPublication != null) {
    tasks.register<Copy>("publishSkikoToVerificationRepository") {
        group = "publishing"
        dependsOn(sourceBuild.task(":publishKotlinMultiplatformPublicationToBuildRepoRepository"))
        dependsOn(sourceBuild.task(":publish${hostPublication}PublicationToBuildRepoRepository"))
        dependsOn(sourceBuild.task(":skiko-skottie:publishKotlinMultiplatformPublicationToBuildRepoRepository"))
        dependsOn(sourceBuild.task(":skiko-skottie:publish${hostPublication}PublicationToBuildRepoRepository"))
        from(sourceBuild.projectDir.resolve("build/repo"))
        into(layout.buildDirectory.dir("repository"))
    }
}

composeSource?.let { source ->
    tasks.register("publishComposeUiToVerificationRepository") {
        group = "publishing"
        dependsOn(source.task(":publishAllHostToVerificationRepository"))
    }
}

tasks.register<Sync>("publishHost") {
    group = "publishing"
    description = "Publish all owned host KLIBs to one local verification repository"
    dependsOn("publishSkikoToVerificationRepository", "publishComposeUiToVerificationRepository")
    from(layout.buildDirectory.dir("repository"))
    from(nativeDirectory.dir("vendor/build/compose-ui/repository")) {
        val composeLock = JsonSlurper().parse(file("upstream/compose.json")) as Map<*, *>
        val modules = composeLock["modules"] as Map<*, *>
        val groups = modules.values.map { (it as Map<*, *>)["group"] ?: composeLock["group"] }.toSet()
        include(groups.map { "${it.toString().replace('.', '/')}/**" })
    }
    into(nativeDirectory.dir("vendor/build/repository"))
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

val repositoryBuildLogic = gradle.includedBuilds.single { it.projectDir.canonicalFile == nativeDirectory.dir("../build-logic").asFile.canonicalFile }
for ((name, target) in mapOf(
    "prepareSources" to ":repository-conventions:prepareNativeSources",
    "checkUpstreamUpdates" to ":repository-conventions:checkNativeUpstreamUpdates",
    "checkSources" to ":native-bridge:checkNativeSources",
    "verifyHost" to ":native-bridge:verifyNativeHost",
    "assembleVerificationRepository" to ":native-bridge:assembleNativeRepository",
)) {
    tasks.register(name) {
        group = if (name in setOf("prepareSources", "checkUpstreamUpdates")) "native setup" else "verification"
        dependsOn(repositoryBuildLogic.task(target))
    }
}
