import groovy.json.JsonSlurper
import love.yinlin.nativebridge.NativeApplicationExtension
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
    "SDL version changed in ../libs.versions.toml; review upstream/sdl.json and re-run prepare_sdl.py."
}
val skikoVersion = libs.versions.skiko.get()
val portGroup = lock["group"] as String
check(libs.skikoNative.get().toString() == "$portGroup:skiko:$skikoVersion") {
    "Native Skiko coordinates changed in ../libs.versions.toml; review upstream/lock.json."
}

val sourceBuild = gradle.includedBuilds.find { it.name == "skiko" }
val hostPublication = when (HostManager.host.name) {
    "mingw_x64" -> "MingwX64"
    "linux_x64" -> "LinuxX64"
    "macos_arm64" -> "MacosArm64"
    else -> null
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

subprojects {
    layout.buildDirectory.set(nativeDirectory.dir("vendor/build/samples/$name"))
    extensions.extraProperties.set("kotlin.mpp.enableCInteropCommonization", providers.gradleProperty("kotlin.mpp.enableCInteropCommonization").getOrElse("true"))
}

tasks.register<Exec>("doctorNative") {
    group = "native setup"
    commandLine(providers.gradleProperty("python.command").getOrElse("python"), nativeDirectory.file("scripts/doctor.py").asFile)
}

tasks.register<Exec>("checkUpstreamUpdates") {
    group = "native setup"
    commandLine(providers.gradleProperty("python.command").getOrElse("python"), nativeDirectory.file("scripts/sync_upstream.py").asFile, "--check-updates")
}
