package love.yinlin.repository.nativecompose

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration

class NativeUpstreamSync(directory: File) {
    internal val nativeDirectory = directory.canonicalFile
    internal val repository = nativeDirectory.parentFile
    internal val vendor = nativeDirectory.resolve("vendor")
    internal val catalog = NativeSourceCatalog(repository)
    internal val compose = nativeDirectory.resolve("upstream/compose.json").sourceJson()
    internal val skiko = nativeDirectory.resolve("upstream/lock.json").sourceJson()
    internal val patches = SourcePatches(nativeDirectory)
    internal val workspace = SourceWorkspace(vendor)

    init {
        check(nativeDirectory.name == "native-compose" && nativeDirectory.resolve("upstream").isDirectory) { "Not a Native Compose source directory: $directory" }
        vendor.mkdirs()
        check(vendor.canonicalFile.parentFile == nativeDirectory && !Files.isSymbolicLink(vendor.toPath())) { "Native vendor directory escapes its repository" }
        check(compose["version"] == catalog.versions["cmp"] && skiko["versions"].sourceObject()["skiko"] == catalog.versions["skiko"]) { "Update the pinned upstream sources before changing CMP/Skiko versions" }
        val library = catalog.libraries.getValue("native-skiko")
        check(library.first == "${skiko["group"]}:skiko" && library.second == catalog.versions["skiko"]) { "Native Skiko catalog/lock mismatch" }
        val spec = patches.manifest["skiko"].sourceObject()
        check(spec["version"] == skiko["versions"].sourceObject()["skiko"] && spec["sources"] == skiko["sources"] && spec["skia"] == skiko["skia"] && spec["skiaAssets"] == skiko["skiaAssets"]) { "Skiko patch provenance does not match the source lock" }
        check(patches.manifest["compose"].sourceObject()["commit"] == compose["commit"] && patches.manifest["resources"].sourceObject()["commit"] == compose["resourceSource"].sourceObject()["commit"] &&
            patches.manifest["compose"].sourceObject()["version"] == compose["version"] && patches.manifest["resources"].sourceObject()["version"] == compose["version"]) { "Compose patches do not match the source commits/releases" }
    }

    internal fun inputHashes(): Map<String, String> {
        val files = mutableListOf<File>()
        files.addAll(nativeDirectory.resolve("upstream").walkTopDown().filter { it.isFile })
        files.addAll(nativeDirectory.resolve("gradle/templates").walkTopDown().filter { it.isFile })
        files.addAll(repository.resolve("build-logic/repository-conventions/src/main/kotlin/love/yinlin/repository/nativecompose").walkTopDown().filter { it.extension == "kt" })
        files.add(repository.resolve("build-logic/native-bridge/gradle/sdl-sdk.gradle.kts"))
        files.add(repository.resolve(".gitattributes"))
        files.add(repository.resolve("settings.gradle.kts"))
        files.add(repository.resolve("gradle.properties"))
        files.addAll(listOf("gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat").map { repository.resolve(it) })
        return files.distinct().sortedBy { it.invariantSeparatorsPath }.associate { it.relativeTo(repository).invariantSeparatorsPath to it.sourceHash() }
    }

    internal fun stage(prefix: String, action: (File) -> Unit) {
        val directory = Files.createTempDirectory(vendor.toPath(), prefix).toFile()
        try { action(directory) }
        finally {
            checkedSourcePath(vendor, directory)
            check(directory.parentFile.canonicalFile == vendor.canonicalFile && directory.name.startsWith(prefix))
            if (directory.exists()) check(directory.deleteRecursively()) { "Cannot remove source staging directory: $directory" }
        }
    }

    fun prepare(offline: Boolean = false) {
        prepareSkiko(offline)
        prepareSdl(offline)
        prepareIcu(offline)
        ComposeSourceSync(this).prepare(offline)
    }

    private fun prepareIcu(offline: Boolean) {
        val lock = nativeDirectory.resolve("upstream/icu.json").sourceJson()
        check(lock["skia"] == skiko["skia"]) { "Review the ICU data version when changing the Skia SDK" }
        val source = lock["source"].sourceObject()
        val data = lock["data"].sourceObject()
        val owner = vendor.resolve("icu")
        workspace.locked(owner) {
            val archive = owner.resolve("icu-data.zip")
            val build = owner.resolve("workspace")
            val marker = build.resolve(".rachel-prepared.json")
            val fingerprint = bytesHash(sourceBytes(mapOf("lock" to lock, "inputs" to inputHashes())))
            val previous = if (marker.isFile) marker.sourceJson() else emptyMap()
            if (previous["fingerprint"] != fingerprint || !build.resolve("icudtl.dat").isFile || !build.resolve("icu-LICENSE.txt").isFile) stage(".icu-data-") { staging ->
                downloadSource(source["url"] as String, archive, source["sha256"] as String, offline, 64L * 1024 * 1024)
                SourceArchive.extract(archive, staging, false)
                val input = staging.resolve(sourceRelativePath(data["file"] as String))
                check(input.sourceHash() == data["sha256"] && staging.resolve("LICENSE").sourceHash() == lock["licenseSha256"]) { "ICU data/license checksum mismatch" }
                Files.move(input.toPath(), staging.resolve("icudtl.dat").toPath())
                Files.move(staging.resolve("LICENSE").toPath(), staging.resolve("icu-LICENSE.txt").toPath())
                workspace.synchronize(staging, build, marker, mapOf("schema" to 3, "fingerprint" to fingerprint, "version" to lock["version"]))
            }
            check(build.resolve("icudtl.dat").sourceHash() == data["sha256"]) { "Prepared ICU data differs from its lock" }
            check(build.resolve("icu-LICENSE.txt").sourceHash() == lock["licenseSha256"]) { "Prepared ICU license differs from its lock" }
            println("Prepared complete ICU ${lock["version"]} locale data")
        }
    }

    private fun prepareSkiko(offline: Boolean) {
        val owner = vendor.resolve("skiko")
        workspace.locked(owner) {
            val upstream = skiko["sources"].sourceObject().getValue("skiko-upstream").sourceObject()
            val repositoryName = upstream["repository"] as String
            val commit = upstream["commit"] as String
            check(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+").matches(repositoryName) && Regex("[a-f0-9]{40}").matches(commit)) { "Invalid Skiko source identity" }
            val archive = owner.resolve("skiko-upstream.zip")
            val inputs = inputHashes()
            val inventory = nativeDirectory.resolve("adaptation").listFiles().orEmpty().filter { it.name.startsWith("org.jetbrains.skiko_") }.flatMap { module -> module.resolve("src").walkTopDown().filter { it.isFile }.map { it.relativeTo(nativeDirectory).invariantSeparatorsPath }.toList() }.sorted()
            val fingerprint = bytesHash(sourceBytes(mapOf("inputs" to inputs, "catalog" to NativeCiInputs(repository).identity(catalog), "inventory" to inventory)))
            val build = owner.resolve("workspace")
            val marker = build.resolve(".rachel-prepared.json")
            val previous = if (marker.isFile) marker.sourceJson() else emptyMap()
            if (previous["fingerprint"] != fingerprint || !workspace.reusable(build, marker, mapOf("fingerprint" to fingerprint))) stage(".skiko-source-") { staging ->
                downloadSource("https://codeload.github.com/$repositoryName/zip/$commit", archive, upstream["sha256"] as String, offline, 128L * 1024 * 1024)
                SourceArchive.extract(archive, staging, true)
                patches.apply(staging, patches.manifest["skiko"].sourceObject())
                catalog.alignUpstreamCatalog(staging.resolve("dependencies.toml"))
                updateSkikoProfile(staging)
                catalog.copyWrappers(staging)
                prepareNotices(staging)
                removeShadows(staging)
                workspace.synchronize(staging, build, marker, mapOf("schema" to 3, "fingerprint" to fingerprint, "sources" to skiko["sources"]), setOf("skiko/dependencies/skia"))
            }
            val key = skiaHostKey()
            val filename = "Skia-${skiko["skia"]}-$key.zip"
            val expected = skiko["skiaAssets"].sourceObject().getValue(key) as String
            val cached = owner.resolve("skia/$expected.zip")
            val target = build.resolve("skiko/dependencies/skia/${skiko["skia"]}/$filename")
            if (!target.isFile || target.sourceHash() != expected) {
                downloadSource("https://github.com/JetBrains/skia/releases/download/${skiko["skia"]}/$filename", cached, expected, offline, 1024L * 1024 * 1024)
                cached.copyTo(checkedSourcePath(vendor, target).apply { parentFile.mkdirs() }, overwrite = true)
            }
            else if (!cached.isFile) target.copyTo(cached.apply { parentFile.mkdirs() })
            writeSource(vendor, owner.resolve("state.json"), sourceBytes(mapOf("schema" to 3, "build" to "vendor/skiko/workspace/skiko", "fingerprint" to fingerprint,
                "inputs" to inputs, "sourceInventory" to inventory, "catalogSha256" to catalog.file.sourceHash(), "java" to catalog.java,
                "versions" to skiko["versions"].sourceObject().mapValues { (name, value) -> catalog.versions[name] ?: value })))
            println("Prepared Skiko ${catalog.versions["skiko"]} sources")
        }
    }

    private fun updateSkikoProfile(directory: File) {
        val properties = directory.resolve("skiko/gradle.properties")
        val values = linkedMapOf("deploy.version" to catalog.versions.getValue("skiko"), "deploy.group" to skiko["group"].toString(), "dependencies.skia" to skiko["skia"].toString(),
            "skiko.kotlin.language.version" to catalog.versions.getValue("kotlin").split('.').take(2).joinToString("."), "skiko.kotlin.api.version" to catalog.versions.getValue("kotlin").split('.').take(2).joinToString("."))
        val rootProperties = repository.resolve("gradle.properties").readText()
        Regex("(?m)^kotlin\\.native\\.ignoreDisabledTargets[ \\t]*=[ \\t]*([^\r\n]*)").find(rootProperties)?.let { values["kotlin.native.ignoreDisabledTargets"] = it.groupValues[1].trim() }
        var text = properties.readText()
        for ((name, value) in values) {
            val pattern = Regex("(?m)^${Regex.escape(name)}[^\r\n]*")
            text = if (pattern.containsMatchIn(text)) pattern.replace(text) { "$name=$value" } else "$text\n$name=$value\n"
        }
        properties.writeText(text)
        val buildSrc = directory.resolve("skiko/buildSrc/build.gradle.kts")
        buildSrc.writeText(Regex("jvmToolchain\\(\\d+\\)").replace(buildSrc.readText(), "jvmToolchain(${catalog.java})"))
    }

    private fun prepareNotices(directory: File) {
        val notices = directory.resolve("skiko/rachel-notices").apply { mkdirs() }
        directory.resolve("LICENSE").copyTo(notices.resolve("Apache-2.0.txt"), overwrite = true)
        directory.resolve("NOTICE").copyTo(notices.resolve("NOTICE"), overwrite = true)
        nativeDirectory.resolve("upstream/adaptation-origins.json").copyTo(notices.resolve("adaptation-origins.json"), overwrite = true)
        notices.resolve("upstream-lock.json").writeBytes(sourceBytes(skiko))
        val sdl = nativeDirectory.resolve("upstream/sdl.json").sourceJson()
        val text = buildString {
            append("# Native desktop source provenance\n\nRachel's modified Native desktop builds of the locked Skiko sources.\n\n")
            for ((name, source) in skiko["sources"].sourceObject()) {
                val entry = source.sourceObject()
                append("- $name: https://github.com/${entry["repository"]} at ${entry["commit"]}.\n")
            }
            append("- Skia: ${skiko["skia"]}; archive hashes are in upstream-lock.json.\n- SDL ${sdl["version"]}: ${sdl["source"].sourceObject()["url"]}; SHA-256 ${sdl["source"].sourceObject()["sha256"]}.\n\n")
            append("Original licenses and copyright notices remain in the sources. adaptation-origins.json records reference-derived files; the unified patch includes the reviewed community delta.\n")
        }
        notices.resolve("THIRD_PARTY_NOTICES.md").writeText(text)
    }

    private fun removeShadows(directory: File) {
        val origins = nativeDirectory.resolve("upstream/adaptation-origins.json").sourceJson()["files"].sourceObject()
        for ((relative, origin) in origins) {
            val maintained = nativeDirectory.resolve(sourceRelativePath(relative))
            check(maintained.isFile) { "Missing maintained Skiko source: $relative" }
            checkedSourcePath(directory, directory.resolve(sourceRelativePath(origin as String))).delete()
        }
    }

    private fun prepareSdl(offline: Boolean) {
        val lock = nativeDirectory.resolve("upstream/sdl.json").sourceJson()
        val version = lock["version"] as String
        check(version == catalog.versions["sdl"]) { "SDL catalog/source lock mismatch" }
        val source = lock["source"].sourceObject()
        val archive = vendor.resolve("sdl-source-$version.zip")
        val destination = vendor.resolve("sdl-source-$version")
        val marker = destination.resolve(".rachel-source-sha256")
        if (!marker.isFile || marker.readText() != source["sha256"]) stage(".sdl-source-") { staging ->
            downloadSource(source["url"] as String, archive, source["sha256"] as String, offline, 128L * 1024 * 1024)
            SourceArchive.extract(archive, staging, true)
            staging.resolve(".rachel-source-sha256").writeText(source["sha256"] as String)
            check(!destination.exists()) { "Unrecognized SDL source directory preserved: $destination" }
            Files.move(staging.toPath(), destination.toPath())
        }
        check(destination.resolve("include/SDL3/SDL.h").isFile && destination.resolve("LICENSE.txt").isFile) { "SDL headers/license are missing" }
        println("Prepared SDL $version sources")
    }

    private fun nativeHostTarget(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            os.startsWith("windows") && arch in setOf("amd64", "x86_64") -> "mingw_x64"
            os.startsWith("linux") && arch in setOf("amd64", "x86_64") -> "linux_x64"
            os.startsWith("mac") && arch in setOf("arm64", "aarch64") -> "macos_arm64"
            else -> error("Unsupported desktop Native host: $os/$arch")
        }
    }

    private fun skiaHostKey(): String = when (nativeHostTarget()) {
        "mingw_x64" -> "windows-Release-x64"
        "linux_x64" -> "linux-Release-x64"
        else -> "macos-Release-arm64"
    }

    fun checkUpdates() {
        val identities = skiko["sources"].sourceObject().mapValues { it.value.sourceObject() } + mapOf("Compose" to compose, "Resources" to compose["resourceSource"].sourceObject())
        for ((name, identity) in identities) {
            val ref = identity["ref"] as String
            val remote = sourceCommand(repository, listOf("git", "ls-remote", "https://github.com/${identity["repository"]}.git", ref, "$ref^{}"))
            println("$name: locked=${identity["commit"]} remote=${remote.trim().ifEmpty { "immutable commit" }}")
        }
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()
        for ((name, value) in compose["modules"].sourceObject()) {
            val module = value.sourceObject()
            if (module["sourceRepository"] != "maven") continue
            val url = "https://repo.maven.apache.org/maven2/${(module["upstreamGroup"] as String).replace('.', '/')}/$name/maven-metadata.xml"
            val response = client.send(HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
            val xml = response.body().use { it.readNBytes(1024 * 1024 + 1) }
            check(response.statusCode() == 200 && xml.size <= 1024 * 1024) { "Cannot read Maven release metadata: $name" }
            val latest = Regex("<latest>([^<]+)</latest>").find(xml.toString(Charsets.UTF_8))?.groupValues?.get(1)
            println("${module["upstreamGroup"]}:$name: locked=${module["version"] ?: compose["version"]} latest=$latest")
        }
    }
}
