package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files
import java.util.Properties
import java.util.zip.ZipFile

class NativeUpstreamSync(directory: File) {
    internal val nativeDirectory = directory.canonicalFile
    internal val repository = nativeDirectory.parentFile
    internal val vendor = nativeDirectory.resolve("vendor")
    internal val catalog = NativeSourceCatalog(repository)
    internal val patches = SourcePatches(nativeDirectory)
    internal val workspace = SourceWorkspace(vendor)
    private val skikoOrigins = nativeDirectory.resolve("adaptation/provenance/adaptation-origins.json")

    internal fun kotlinProperties(): Map<String, String> {
        val properties = Properties().apply { repository.resolve("gradle.properties").inputStream().use(::load) }
        return properties.stringPropertyNames().filter {
            it.startsWith("rachel.kotlin.") || it in setOf("kotlin.native.ignoreDisabledTargets", "kotlin.native.jvmArgs")
        }.sorted().associateWith(properties::getProperty)
    }

    init {
        check(nativeDirectory.name == "native-compose" && nativeDirectory.resolve("upstream").isDirectory) { "Not a Native Compose source directory: $directory" }
        vendor.mkdirs()
        check(vendor.canonicalFile.parentFile == nativeDirectory && !Files.isSymbolicLink(vendor.toPath())) { "Native vendor directory escapes its repository" }
    }

    private fun inputHashes(): Map<String, String> {
        val files = mutableListOf<File>()
        for (relative in listOf("native-compose/upstream", "native-compose/gradle", "native-compose/adaptation/provenance",
            "build-logic/repository-conventions/src/main/kotlin/love/yinlin/repository/nativecompose", "build-logic/native-bridge/gradle")) {
            files.addAll(repository.resolve(relative).walkTopDown().filter { it.isFile })
        }
        files.addAll(listOf("native-compose/build.gradle.kts", ".gitattributes", "settings.gradle.kts", "gradle.properties", "gradle/wrapper/gradle-wrapper.properties",
            "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat").map(repository::resolve))
        return files.distinct().sortedBy { it.invariantSeparatorsPath }.associate { it.relativeTo(repository).invariantSeparatorsPath to it.sourceHash() }
    }

    internal fun <T> stage(prefix: String, action: (File) -> T): T {
        val directory = Files.createTempDirectory(vendor.toPath(), prefix).toFile()
        try { return action(directory) }
        finally {
            checkedSourcePath(vendor, directory)
            check(directory.parentFile.canonicalFile == vendor.canonicalFile && directory.name.startsWith(prefix))
            if (File.separatorChar == '\\') {
                // Git can mark loose objects and packs read-only on Windows.
                Files.walk(directory.toPath()).use { paths ->
                    paths.filter { Files.isRegularFile(it, java.nio.file.LinkOption.NOFOLLOW_LINKS) }.forEach {
                        val attributes = Files.getFileAttributeView(it, java.nio.file.attribute.DosFileAttributeView::class.java, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                        if (attributes.readAttributes().isReadOnly) attributes.setReadOnly(false)
                    }
                }
            }
            Files.walk(directory.toPath()).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
            }
        }
    }

    private fun version(name: String): String = catalog.versions.getValue(name).also {
        check(Regex("[0-9]+(?:[.][0-9]+)+(?:-[A-Za-z0-9.-]+)?").matches(it)) { "Invalid upstream version for $name: $it" }
    }

    /** Returns the exact generation prepared for this Gradle invocation. */
    fun prepare(offline: Boolean = false): File = workspace.locked(vendor) {
        stage(".inputs-") { staging -> prepare(NativeSourceInputs(vendor, offline, staging)) }
    }

    private fun prepare(resolver: NativeSourceInputs): File {
        val inputs = inputHashes()
        val catalogHash = catalog.sha256
        check(catalog.file.sourceHash() == catalogHash) { "Version catalog changed during Native source preparation; retry synchronization." }
        val cmpVersion = version("cmp")
        val skikoVersion = version("skiko")
        val sdlVersion = version("sdl")
        val icuVersion = version("icu")
        val composeSpec = nativeDirectory.resolve("upstream/compose.json").sourceJson()
        val versions = catalog.upstreamVersions(composeSpec["modules"].sourceObject())
        val hash = bytesHash(sourceBytes(versions))
        // Keep paths short for MSVC/CMake; the marker always verifies all 256 bits.
        val generation = checkedSourcePath(vendor, vendor.resolve("g/${hash.take(24)}"))
        val owner = checkedSourcePath(vendor, generation.resolve(".rachel-generation.json"))
        if (generation.exists()) {
            check(owner.isFile && owner.sourceJson()["sha256"] == hash) { "Unrecognized Native generation preserved: $generation" }
        }
        val skikoSpec = nativeDirectory.resolve("upstream/skiko.json").sourceJson()
        check(skikoSpec["group"] == "love.yinlin.native.skiko") { "Invalid Native Skiko publication namespace" }
        val sdlSpec = nativeDirectory.resolve("upstream/sdl.json").sourceJson()
        val icuSpec = nativeDirectory.resolve("upstream/icu.json").sourceJson()
        val maven = linkedMapOf<String, NativeSourceInputs.Archive>()
        val modules = composeSpec["modules"].sourceObject().mapValues { (name, value) ->
            val module = value.sourceObject()
            val selectedVersion = version(module["versionRef"] as? String ?: "cmp")
            if (module["sourceRepository"] != "maven") module + ("version" to selectedVersion)
            else {
                val group = (module["upstreamGroup"] as String).replace('.', '/')
                val artifact = "$name-macosarm64"
                val url = "https://repo.maven.apache.org/maven2/$group/$artifact/$selectedVersion/$artifact-$selectedVersion-sources.jar"
                val archive = resolver.archive("maven-$name", selectedVersion, url, 32L * 1024 * 1024)
                maven[name] = archive
                module + mapOf("version" to selectedVersion, "sourceArchive" to (module["sourceArchive"].sourceObject() + archive.metadata))
            }
        }
        val core = resolver.gitTree("compose", composeSpec["repository"] as String, cmpVersion, modules.filterValues { it["sourceRepository"] == null }.map { (name, module) -> ComposeSourceSync.sourcePath(name, module) })
        val resources = resolver.gitTree("resources", composeSpec["resourceSource"].sourceObject()["repository"] as String, cmpVersion, listOf("components/resources/library"))
        val compose = composeSpec + core.metadata + mapOf("resourceSource" to resources.metadata, "modules" to modules)
        val skikoArchive = resolver.archive("skiko", skikoVersion, "https://codeload.github.com/JetBrains/skiko/zip/refs/tags/v$skikoVersion", 128L * 1024 * 1024)
        val skikoRecord = checkedSourcePath(vendor, generation.resolve("metadata/skiko.json"))
        val previousSkiko = if (skikoRecord.isFile) skikoRecord.sourceJson() else emptyMap()
        val skia = (previousSkiko["skia"] as? String)?.takeIf {
            previousSkiko["source"].sourceObject()["sha256"] == skikoArchive.metadata["sha256"] && Regex("m[0-9]+-[a-f0-9]+").matches(it)
        } ?: skiaVersion(skikoArchive.file)
        val host = skiaHostKey()
        val hostSdk = checkedSourcePath(vendor, generation.resolve("skiko/workspace/skiko/dependencies/skia/$skia/Skia-$skia-$host.zip"))
        val runtime = icuSpec["runtime"].sourceObject()
        val sdk = setOf(host, runtime["archive"] as String).associateWith { key ->
            resolver.archive("skia-$key", skia, "https://github.com/JetBrains/skia/releases/download/$skia/Skia-$skia-$key.zip", 1024L * 1024 * 1024, hostSdk.takeIf { key == host })
        }
        val patchOrigins = skikoOrigins.sourceJson().getValue("patchOrigins")
        val skiko = skikoSpec + mapOf("patchOrigins" to patchOrigins, "version" to skikoVersion, "source" to (skikoArchive.metadata + ("ref" to "refs/tags/v$skikoVersion")), "skia" to skia,
            "skiaAssets" to sdk.mapValues { it.value.metadata })
        val sdlArchive = resolver.archive("sdl", sdlVersion, "https://github.com/libsdl-org/SDL/releases/download/release-$sdlVersion/SDL3-$sdlVersion.zip", 128L * 1024 * 1024)
        val sdl = sdlSpec + mapOf("version" to sdlVersion, "source" to sdlArchive.metadata)
        val icuArchive = resolver.archive("icu", icuVersion, "https://github.com/unicode-org/icu/releases/download/release-$icuVersion/icu4c-$icuVersion-data-bin-l.zip", 64L * 1024 * 1024)
        if (!owner.isFile) writeSource(vendor, owner, sourceBytes(mapOf("sha256" to hash, "identity" to versions)))
        // This directory is reused across configuration and patch changes; a failed refresh is not ready.
        val ready = checkedSourcePath(vendor, generation.resolve("ready.json"))
        check(!ready.exists() || ready.delete()) { "Cannot invalidate Native source readiness: $ready" }
        writeMetadata(generation, "skiko", skiko)
        writeMetadata(generation, "compose", compose)
        writeMetadata(generation, "sdl", sdl)
        prepareSdl(generation, sdlArchive, sdl)
        prepareIcu(generation, icuVersion, icuArchive, sdk.getValue(runtime.getValue("archive") as String), runtime, skia)
        val hostArchive = sdk.getValue(host)
        prepareSkia(generation, host, hostSdk.resolveSibling(hostSdk.nameWithoutExtension), hostArchive)
        prepareSkiko(generation, skikoArchive, skiko, sdl)
        ComposeSourceSync(this, generation, compose).prepare(core, resources, maven)
        check(catalog.file.sourceHash() == catalogHash && inputHashes() == inputs) { "Native synchronization inputs changed during preparation; retry synchronization." }
        if (hostSdk.isFile && hostSdk.sourceHash() == hostArchive.metadata["sha256"]) {
            check(hostSdk.delete()) { "Cannot remove superseded Skia archive: $hostSdk" }
        }
        val current = mapOf("directory" to generation.relativeTo(nativeDirectory).invariantSeparatorsPath, "sha256" to hash, "catalogSha256" to catalogHash, "inputs" to inputs)
        writeSource(vendor, generation.resolve("ready.json"), sourceBytes(current))
        writeSource(vendor, vendor.resolve("current.json"), sourceBytes(current))
        println("Prepared Native Compose $cmpVersion / Skiko $skikoVersion / SDL $sdlVersion: $generation")
        return generation
    }

    private fun writeMetadata(generation: File, name: String, metadata: SourceObject) {
        writeSource(vendor, generation.resolve("metadata/$name.json"), sourceBytes(metadata))
    }

    private fun skiaVersion(archive: File): String = ZipFile(archive).use { zip ->
        val entry = zip.entries().asSequence().single { it.name.substringAfter('/') == "skiko/gradle.properties" }
        check(entry.size in 1 .. 64L * 1024) { "Invalid Skiko Gradle properties" }
        val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        Regex("(?m)^dependencies[.]skia=(m[0-9]+-[a-f0-9]+)[ \t]*\r?$").find(content)?.groupValues?.get(1)
            ?: error("Cannot resolve the Skia SDK from Skiko's release")
    }

    private fun prepareIcu(generation: File, version: String, archive: NativeSourceInputs.Archive, sdk: NativeSourceInputs.Archive, runtime: SourceObject, skia: String) {
        val destination = generation.resolve("icu/workspace")
        val record = generation.resolve("icu/files.json")
        val identity = mapOf("archiveSha256" to archive.metadata["sha256"], "sdkSha256" to sdk.metadata["sha256"], "version" to version, "runtime" to runtime)
        if (!workspace.reusable(destination, record, identity)) stage(".icu-") { staging ->
            val dataFile = "icudt${version.substringBefore('.')}l.dat"
            val extracted = SourceArchive.extract(archive.file, staging, false) { relative ->
                when (relative) {
                    dataFile -> "icudtl.dat"
                    "LICENSE" -> "icu-LICENSE.txt"
                    else -> null
                }
            }
            check(extracted.size == 2) { "Missing complete ICU data or license in ${archive.file}" }
            val text = SourceArchive.extract(sdk.file, staging, false) { relative -> if (relative == runtime["entry"]) "text/icudtl.dat" else null }
            check(text.size == 1) { "The Skia SDK is missing its external text ICU data" }
            workspace.synchronize(staging, destination, record, identity)
        }
        writeMetadata(generation, "icu", mapOf("version" to version, "skia" to skia, "source" to archive.metadata,
            "data" to mapOf("sha256" to destination.resolve("icudtl.dat").sourceHash()),
            "runtime" to (runtime + ("sha256" to destination.resolve("text/icudtl.dat").sourceHash())),
            "licenseSha256" to destination.resolve("icu-LICENSE.txt").sourceHash()))
    }

    private fun prepareSkia(generation: File, target: String, destination: File, archive: NativeSourceInputs.Archive) {
        val record = generation.resolve("skia/$target/files.json")
        val identity = mapOf("archiveSha256" to archive.metadata["sha256"], "layout" to 1)
        if (workspace.reusable(destination, record, identity)) return
        stage(".skia-") { staging ->
            val files = SourceArchive.extract(archive.file, staging, false) { relative ->
                val name = relative.substringAfterLast('/')
                if (target.startsWith("windows-") && name.startsWith("lib") && name.endsWith(".lib"))
                    relative.removeSuffix(name) + name.removePrefix("lib")
                else relative
            }
            check(files.keys.any { it.startsWith("include/") } && files.keys.any { it.startsWith("out/") }) { "Incomplete Skia SDK: $target" }
            workspace.synchronize(staging, destination, record, identity, adoptMatchingFiles = true)
        }
    }

    private fun prepareSkiko(generation: File, archive: NativeSourceInputs.Archive, skiko: SourceObject, sdl: SourceObject) {
        val destination = generation.resolve("skiko/workspace")
        val record = generation.resolve("skiko/files.json")
        val identity = mapOf("sourceSha256" to skiko["source"].sourceObject()["sha256"], "patch" to patches.manifest["skiko"], "originsSha256" to skikoOrigins.sourceHash())
        if (!workspace.reusable(destination, record, identity)) stage(".skiko-") { staging ->
            SourceArchive.extract(archive.file, staging, true)
            patches.apply(staging, patches.manifest["skiko"].sourceObject())
            for (module in listOf("skiko", "skiko/skiko-skottie")) {
                val directory = staging.resolve(module)
                Files.move(directory.resolve("build.gradle.kts").toPath(), directory.resolve("rachel-upstream.gradle.kts").toPath())
            }
            catalog.alignUpstreamCatalog(staging.resolve("dependencies.toml"))
            updateSkikoProfile(staging, skiko)
            for (path in listOf("skiko/settings.gradle.kts", "skiko/buildSrc/settings.gradle.kts")) {
                val settings = staging.resolve(path)
                val build = destination.resolve(path).parentFile
                val repositoryPath = build.toPath().relativize(repository.toPath()).toString().replace(File.separatorChar, '/')
                settings.writeText(settings.readText().replace("__REPOSITORY__", repositoryPath))
            }
            catalog.copyWrappers(staging)
            prepareNotices(staging, skiko, sdl)
            removeShadows(staging)
            workspace.synchronize(staging, destination, record, identity, setOf("skiko/dependencies/skia"))
        }
        refreshSkikoBuild(destination, record, skiko, sdl)
    }

    private fun refreshSkikoBuild(destination: File, record: File, skiko: SourceObject, sdl: SourceObject) {
        stage(".skiko-build-") { staging ->
            val upstreamScripts = record.sourceJson()["fileHashes"].sourceObject().keys.filter { it.endsWith(".gradle.kts") }
            val files = mutableSetOf("dependencies.toml", "skiko/gradle.properties", "skiko/buildSrc/build.gradle.kts", "LICENSE", "NOTICE")
            files.addAll(upstreamScripts)
            files.addAll(destination.walkTopDown().onEnter { it.name !in setOf("build", ".gradle", ".git", ".kotlin", "dependencies") }.filter {
                it.name == "gradle-wrapper.properties" && it.parentFile.name == "wrapper" && it.parentFile.parentFile.name == "gradle"
            }.map { it.relativeTo(destination).invariantSeparatorsPath })
            for (relative in files) destination.resolve(relative).copyTo(staging.resolve(relative))
            catalog.alignUpstreamCatalog(staging.resolve("dependencies.toml"))
            updateSkikoProfile(staging, skiko)
            val suppressWarnings = "@file:Suppress(\"warnings\")\n\n"
            for (relative in upstreamScripts) {
                val script = staging.resolve(relative)
                val source = script.readText()
                if (!source.startsWith(suppressWarnings)) script.writeText(suppressWarnings + source)
            }
            catalog.copyWrappers(staging)
            prepareNotices(staging, skiko, sdl)
            workspace.updateGenerated(staging, destination, record)
        }
    }

    private fun updateSkikoProfile(directory: File, skiko: SourceObject) {
        val properties = directory.resolve("skiko/gradle.properties")
        val language = catalog.versions.getValue("kotlin").split('.').take(2).joinToString(".")
        val values = linkedMapOf("deploy.version" to "${catalog.versions.getValue("rachel")}-cmp-${catalog.versions.getValue("cmp")}", "deploy.group" to skiko["group"].toString(), "dependencies.skia" to skiko["skia"].toString(),
            "skiko.kotlin.language.version" to language, "skiko.kotlin.api.version" to language,
            "skiko.kotlin.native.language.version" to language, "skiko.kotlin.native.api.version" to language)
        values.putAll(kotlinProperties())
        var text = properties.readText()
        for ((name, value) in values) {
            val pattern = Regex("(?m)^${Regex.escape(name)}[ \\t]*=[^\r\n]*")
            text = if (pattern.containsMatchIn(text)) pattern.replace(text) { "$name=$value" } else "$text\n$name=$value\n"
        }
        properties.writeText(text)
        val buildSrc = directory.resolve("skiko/buildSrc/build.gradle.kts")
        val buildScript = Regex("jvmToolchain\\(\\d+\\)").replace(buildSrc.readText(), "jvmToolchain(${catalog.java})")
        val generationInputs = """
            |tasks.configureEach {
            |    inputs.property("rachel.native.generation", projectDir.resolve("../../../..").canonicalFile.name)
            |}
        """.trimMargin() + "\n"
        buildSrc.writeText(if (generationInputs in buildScript) buildScript else buildScript + generationInputs)
    }

    private fun prepareNotices(directory: File, skiko: SourceObject, sdl: SourceObject) {
        val notices = directory.resolve("skiko/rachel-notices").apply { mkdirs() }
        directory.resolve("LICENSE").copyTo(notices.resolve("Apache-2.0.txt"), overwrite = true)
        directory.resolve("NOTICE").copyTo(notices.resolve("NOTICE"), overwrite = true)
        skikoOrigins.copyTo(notices.resolve("adaptation-origins.json"), overwrite = true)
        notices.resolve("upstream-sources.json").writeBytes(sourceBytes(mapOf("skiko" to skiko, "sdl" to sdl, "patches" to patches.manifest)))
        notices.resolve("THIRD_PARTY_NOTICES.md").writeText(
            "# Native desktop source provenance\n\nRachel modifies Skiko and SDL for Native desktop. See upstream-sources.json for the resolved releases and SHA-256 values. " +
                "Original licenses remain in the sources. adaptation-origins.json records reference-derived files.\n"
        )
    }

    private fun removeShadows(directory: File) {
        val origins = skikoOrigins.sourceJson()["files"].sourceObject()
        for ((relative, origin) in origins) {
            val maintained = nativeDirectory.resolve(sourceRelativePath(relative))
            check(maintained.isFile) { "Missing maintained Skiko source: $relative" }
            checkedSourcePath(directory, directory.resolve(sourceRelativePath(origin as String))).delete()
        }
    }

    private fun prepareSdl(generation: File, archive: NativeSourceInputs.Archive, metadata: SourceObject) {
        val destination = generation.resolve("sdl/source")
        val record = generation.resolve("sdl/files.json")
        val identity = mapOf("sourceSha256" to metadata["source"].sourceObject()["sha256"], "patch" to patches.manifest["sdl"], "build" to metadata["build"])
        if (workspace.reusable(destination, record, identity)) return
        stage(".sdl-") { staging ->
            SourceArchive.extract(archive.file, staging, true)
            check(staging.resolve("include/SDL3/SDL.h").isFile && staging.resolve("LICENSE.txt").isFile) { "SDL headers/license are missing" }
            check(metadata["build"].sourceObject()["dynamicApi"] == false) { "The static Compose SDL profile requires direct API calls" }
            NativeSdlSource.disableDynamicApi(staging)
            patches.apply(staging, patches.manifest["sdl"].sourceObject())
            workspace.synchronize(staging, destination, record, identity)
        }
    }

    private fun skiaHostKey(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            os.startsWith("windows") && arch in setOf("amd64", "x86_64") -> "windows-Release-x64"
            os.startsWith("linux") && arch in setOf("amd64", "x86_64") -> "linux-Release-x64"
            os.startsWith("mac") && arch in setOf("arm64", "aarch64") -> "macos-Release-arm64"
            else -> error("Unsupported desktop Native host: $os/$arch")
        }
    }
}
