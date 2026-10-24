import groovy.json.JsonSlurper
import love.yinlin.nativebridge.NativeApplicationExtension
import org.jetbrains.kotlin.konan.target.HostManager

plugins { idea }

val nativeDirectory = layout.projectDirectory
idea.module {
    // Included builds provide their own source roots inside vendor.
    excludeDirs.addAll(files("vendor", "gradle-plugin", ".gradle", ".kotlin").files)
}
val lock = JsonSlurper().parse(file("upstream/lock.json")) as Map<*, *>
val lockedVersions = lock["versions"] as Map<*, *>
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val sdlLock = JsonSlurper().parse(file("upstream/sdl.json")) as Map<*, *>
check(libs.versions.sdl.get() == sdlLock["version"]) {
    "SDL version changed in ../libs.versions.toml; review upstream/sdl.json and re-run prepare_sdl.py."
}
for (key in listOf("kotlin", "cmp", "skiko", "coroutines", "agp", "dokka")) {
    check(catalog.findVersion(key).get().requiredVersion == lockedVersions[key]) {
        "Version $key changed in ../libs.versions.toml; review upstream/lock.json and re-run sync_upstream.py."
    }
}
val portVersion = lock["portVersion"] as String
val portGroup = lock["group"] as String

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
        from(sourceBuild.projectDir.resolve("build/repo"))
        into(layout.buildDirectory.dir("repository"))
    }
}

subprojects {
    extensions.extraProperties.set("kotlin.mpp.enableCInteropCommonization", providers.gradleProperty("kotlin.mpp.enableCInteropCommonization").getOrElse("true"))
    pluginManager.withPlugin("love.yinlin.native-bridge") {
        extensions.configure<NativeApplicationExtension> {
            resources.from(nativeDirectory.file("THIRD_PARTY_NOTICES.md"), nativeDirectory.dir("licenses"))
            runtime("mingw_x64") {
                if (sourceBuild != null) {
                    val state = JsonSlurper().parse(nativeDirectory.file("vendor/state.json").asFile) as Map<*, *>
                    val skia = lock["skia"] as String
                    files.from(nativeDirectory.file("${state["build"]}/dependencies/skia/$skia/Skia-$skia-windows-Release-x64/out/Release-windows-x64/icudtl.dat"))
                    files.builtBy(sourceBuild.task(":unzipSkiaReleaseWindowsX64"))
                }
                else artifacts.put("$portGroup:skiko-mingwx64:$portVersion:icudtl@dat", "icudtl.dat")
            }
        }
    }
}

tasks.register<Exec>("doctorNative") {
    group = "native setup"
    commandLine(providers.gradleProperty("python.command").getOrElse("python"), nativeDirectory.file("scripts/doctor.py").asFile)
}

tasks.register<Exec>("checkUpstreamUpdates") {
    group = "native setup"
    commandLine(providers.gradleProperty("python.command").getOrElse("python"), nativeDirectory.file("scripts/sync_upstream.py").asFile, "--check-updates")
}
