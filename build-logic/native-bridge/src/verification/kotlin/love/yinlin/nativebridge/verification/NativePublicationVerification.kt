package love.yinlin.nativebridge.verification

import java.io.File
import java.nio.file.Files
import java.util.Properties
import java.util.zip.ZipFile

/** Audits actual Maven files, KLIB manifests, embedded native archives and test reports. */
internal class NativePublicationVerification(private val nativeDirectory: File) {
    private val compose = nativeDirectory.resolve("upstream/compose.json").readJson()
    private val skiko = nativeDirectory.resolve("upstream/lock.json").readJson()
    private val modules = compose["modules"].jsonObject()
    private val compiler = catalogVersions().getValue("kotlin")

    private fun catalogVersions(): Map<String, String> {
        val text = nativeDirectory.parentFile.resolve("libs.versions.toml").readText().substringAfter("[versions]").substringBefore("[libraries]")
        return Regex("(?m)^([A-Za-z0-9_.-]+)\\s*=\\s*\"([^\"]+)\"\\s*$").findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    }

    fun checkSources() {
        val versions = catalogVersions()
        check(compose["version"] == versions["cmp"] && skiko["versions"].jsonObject()["skiko"] == versions["skiko"]) { "Review the source locks for the selected CMP/Skiko release" }
        check(nativeDirectory.resolve("upstream/sdl.json").readJson()["version"] == versions["sdl"]) { "SDL catalog/lock mismatch" }
        check(nativeDirectory.resolve("upstream/icu.json").readJson()["skia"] == skiko["skia"]) { "Review the ICU locale data for the selected Skia SDK" }
        for ((artifact, value) in modules) {
            val module = value.jsonObject()
            check((module["redirectTargets"] as List<*>).toSet() == targetSuffixes.keys && (module["metadataTargets"] as List<*>).toSet() == targetSuffixes.keys) { "Missing owned Native target: $artifact" }
            val reference = module["versionRef"] as? String ?: "cmp"
            check((module["version"] ?: compose["version"]) == versions[reference]) { "Source/catalog version mismatch: $artifact" }
        }
        val allowed = setOf("nativeMain", "windowsMain", "linuxMain", "macosMain", "nativeTest", "windowsTest", "linuxTest", "macosTest")
        for (module in nativeDirectory.resolve("adaptation").listFiles().orEmpty().filter { it.isDirectory }) {
            for (source in module.resolve("src").listFiles().orEmpty()) {
                check(source.isDirectory && source.name in allowed) { "Unexpected maintained source root: $source" }
                if (source.name.endsWith("Test")) {
                    check(source.walkTopDown().filter { it.extension == "kt" }.none { Regex("\\bexpect\\s+(?:fun|class|object|interface|val|var)\\b").containsMatchIn(it.readText()) }) { "Tests must use concrete fixtures: $source" }
                }
            }
        }
        for (manifest in listOf("adaptation-origins.json", "compose-origins.json")) {
            for (relative in nativeDirectory.resolve("upstream/$manifest").readJson()["files"].jsonObject().keys) {
                check(relative.startsWith("adaptation/") && nativeDirectory.contained(relative).isFile) { "Missing maintained source: $relative" }
            }
        }
        println("PASS: ${modules.size + 2} owned libraries; uniform Native roots and pinned sources")
    }

    internal fun verifyFiles(metadata: JsonObject, directory: File) {
        for (variant in metadata.objects("variants")) {
            for (entry in variant.objects("files")) {
                val file = directory.contained(entry["url"] as String)
                check(file.isFile && file.length() == (entry["size"] as Number).toLong()) { "Missing or wrong-sized Maven artifact: $file" }
                for (algorithm in listOf("sha256", "sha512")) {
                    if (entry.containsKey(algorithm)) check(file.digest(algorithm) == entry[algorithm]) { "Maven checksum mismatch: $file" }
                }
            }
        }
    }

    internal fun verifyPublication(metadata: JsonObject, directory: File, group: String, artifact: String, version: String) {
        val component = metadata["component"].jsonObject()
        check(component["group"] == group && component["version"] == version) { "Wrong coordinates: $artifact" }
        verifyFiles(metadata, directory)
        // KMP leaf metadata identifies its root component; the POM still uses the leaf GAV.
        val pom = directory.resolve("$artifact-$version.pom")
        check(pom.isFile) { "Missing POM: $pom" }
        val project = pom.safeXml().documentElement
        check(project.nodeName == "project") { "Invalid POM: $pom" }
        val coordinates = (0 ..< project.childNodes.length).map { project.childNodes.item(it) }
            .filter { it.nodeName in setOf("groupId", "artifactId", "version") }.associate { it.nodeName to it.textContent.trim() }
        check(coordinates == mapOf("groupId" to group, "artifactId" to artifact, "version" to version)) { "Wrong POM coordinates: $pom" }
    }

    private fun identity(artifact: String): Triple<String, String, String> {
        if (artifact in setOf("skiko", "skiko-skottie")) return Triple(skiko["group"] as String, skiko["versions"].jsonObject()["skiko"] as String, "org.jetbrains.skiko")
        val module = modules.getValue(artifact).jsonObject()
        return Triple((module["group"] ?: compose["group"]) as String, (module["version"] ?: compose["version"]) as String,
            module["upstreamGroup"] as? String ?: "org.jetbrains.compose.${module["family"] ?: "ui"}")
    }

    fun verify(repository: File, target: String) {
        val suffix = targetSuffixes.getValue(target)
        val artifacts = listOf("skiko", "skiko-skottie") + modules.keys
        val owned = artifacts.map { artifact -> identity(artifact).third to artifact }.toSet()
        for (artifact in artifacts) {
            val (group, version, upstreamGroup) = identity(artifact)
            val base = repository.resolve(group.replace('.', '/'))
            val root = base.resolve("$artifact/$version")
            val native = base.resolve("$artifact-$suffix/$version")
            val rootMetadata = root.resolve("$artifact-$version.module").readJson()
            val targetMetadata = native.resolve("$artifact-$suffix-$version.module").readJson()
            for ((metadata, directory) in listOf(rootMetadata to root, targetMetadata to native)) {
                verifyPublication(metadata, directory, group, if (directory == root) artifact else "$artifact-$suffix", version)
                for (variant in metadata.objects("variants")) {
                    for (dependency in variant.objects("dependencies")) {
                        val name = targetSuffixes.values.fold(dependency["module"] as String) { value, platform -> value.removeSuffix("-$platform") }
                        check((dependency["group"] to name) !in owned) { "$artifact still exports an upstream owned dependency: $dependency" }
                    }
                }
            }
            val variants = rootMetadata.objects("variants").filter { (it["attributes"] as? Map<*, *>)?.get("org.jetbrains.kotlin.native.target") == target }
            check(variants.any { (it["available-at"] as? Map<*, *>)?.get("module") == "$artifact-$suffix" }) { "Root metadata omits $artifact/$target" }
            val main = native.resolve("$artifact-$suffix-$version.klib")
            val exportedOptions = mutableListOf<String>()
            var sdl = false
            for (klib in native.listFiles().orEmpty().filter { it.extension == "klib" }) {
                ZipFile(klib).use { archive ->
                    val names = archive.entries().asSequence().map { it.name }.toList()
                    sdl = sdl || names.any { "/included/" in it && it.substringAfterLast('/') in setOf("SDL3-static.lib", "libSDL3.a") }
                    val manifests = names.filter { it.endsWith("/manifest") }
                    check(manifests.isNotEmpty()) { "KLIB manifest is missing: $klib" }
                    for (name in manifests) {
                        val text = archive.entryText(name)
                        val properties = Properties().apply { text.reader().use { load(it) } }
                        check(properties["native_targets"] == target && properties["compiler_version"] == compiler) { "Wrong compiler or target: $klib" }
                        if (klib == main) check(properties["unique_name"] == "$upstreamGroup:$artifact") { "KLIB identity changed: $klib" }
                        for (line in text.lineSequence()) {
                            if (line.startsWith("linkerOpts=")) {
                                exportedOptions.add(line)
                                if ("cinterop-sdl" in klib.name) check("-Wl," !in line && "-Xlinker" !in line) { "SDL driver flags must be normalized: $klib" }
                            }
                            if (line.startsWith("linkerOpts=") || line.startsWith("libraryPaths=")) {
                                check(!Regex("[A-Za-z]:[\\\\/]|/(?:Users|home|workspace|tmp|private|Volumes)/").containsMatchIn(line)) { "Producer path leaked into $klib" }
                            }
                        }
                    }
                    if (klib == main && artifact in setOf("skiko", "skiko-skottie")) {
                        val binaries = names.filter { "/included/" in it && (it.endsWith(".a") || it.endsWith(".lib")) }
                        val expected = if (artifact == "skiko") "skia.lib" else "skottie.lib"
                        check(binaries.any { it.substringAfterLast('/') in setOf(expected, "libskia_static.a") } && binaries.any { "native-bridges" in it }) { "Static renderer/C++ bridge archives are missing: $klib" }
                    }
                }
            }
            check(main.isFile) { "Main KLIB is missing: $main" }
            if (artifact == "skiko") {
                check(sdl) { "Core KLIB must embed static SDL" }
                val icu = native.resolve("$artifact-$suffix-$version-icudtl.dat")
                val expectedIcu = nativeDirectory.resolve("upstream/icu.json").readJson()["data"].jsonObject()["sha256"]
                check(icu.isFile && icu.digest("SHA-256") == expectedIcu) { "Complete ICU locale data is missing or has the wrong version: $icu" }
                val textIcu = native.resolve("$artifact-$suffix-$version-icudtl-text.dat")
                val expectedTextIcu = nativeDirectory.resolve("upstream/icu.json").readJson()["runtime"].jsonObject()["sha256"]
                check(textIcu.isFile && textIcu.digest("SHA-256") == expectedTextIcu) { "JetBrains text ICU data is missing or has the wrong version: $textIcu" }
                val license = native.resolve("$artifact-$suffix-$version-icu-license.txt")
                check(license.isFile && license.digest("SHA-256") == nativeDirectory.resolve("upstream/icu.json").readJson()["licenseSha256"]) { "ICU data license is missing: $license" }
                if (target == "linux_x64") for (system in listOf("fontconfig", "GL", "dl")) check(exportedOptions.any { "-l$system" in it }) { "Linux $system link dependency is missing" }
            }
            if (artifact == "skiko-skottie") {
                check(targetMetadata.objects("variants").flatMap { it.objects("dependencies") }.any {
                    it["group"] == group && it["module"] in setOf("skiko", "skiko-$suffix") && it["version"].jsonObject()["requires"] == version
                }) { "Skottie must export matching Core" }
            }
            for (source in listOf(root.resolve("$artifact-$version-sources.jar"), native.resolve("$artifact-$suffix-$version-sources.jar"))) {
                verifySources(source, artifact)
            }
        }
        println("PASS: ${artifacts.size} owned Maven modules / $target")
    }

    private fun verifySources(file: File, artifact: String) {
        ZipFile(file).use { archive ->
            val names = archive.entries().asSequence().map { it.name }.toList()
            check(names.any { it.endsWith(".kt") } && names.none { "StubsMain/" in it }) { "Missing real source implementation: $file" }
            if (artifact in setOf("skiko", "skiko-skottie")) {
                check("META-INF/licenses/Apache-2.0.txt" in names && "META-INF/licenses/THIRD_PARTY_NOTICES.md" in names) { "Skiko license/provenance is missing: $file" }
                check(names.any { it.startsWith("native-sources/") && (it.endsWith(".cc") || it.endsWith(".cpp")) }) { "C++ sources are missing: $file" }
                if (artifact == "skiko") check("META-INF/licenses/SDL.txt" in names && "META-INF/licenses/ICU.txt" in names) { "SDL/ICU license is missing" }
            } else check("META-INF/licenses/LICENSE.txt" in names && "META-INF/provenance/compose.json" in names) { "Compose license/provenance is missing: $file" }
        }
    }

    internal fun expectedTests(target: String, skikoOnly: Boolean): Map<String, Int> {
        val result = linkedMapOf<String, Int>()
        for (module in nativeDirectory.resolve("adaptation").listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("org.jetbrains.skiko_") == skikoOnly }) {
            for (sourceSet in listOf("nativeTest", "${targetSourceNames.getValue(target)}Test")) {
                for (file in module.resolve("src/$sourceSet").walkTopDown().filter { it.isFile && it.extension == "kt" }) {
                    val text = file.readText()
                    val count = Regex("(?m)^\\s*@Test\\b").findAll(text).count()
                    if (count == 0) continue
                    check(Regex("\\bclass\\s+${Regex.escape(file.nameWithoutExtension)}\\b").containsMatchIn(text)) { "Test class must match its file: $file" }
                    check(result.put(file.nameWithoutExtension, count) == null) { "Duplicate maintained test: $file" }
                }
            }
        }
        return result
    }

    internal fun verifyReports(directories: List<File>, expected: Map<String, Int>) {
        val found = linkedMapOf<String, Int>()
        val reports = directories.flatMap { it.walkTopDown().filter { file -> file.isFile && file.name.startsWith("TEST-") && file.extension == "xml" && file.toPath().any { part -> part.toString() == "test-results" } }.toList() }
        check(reports.isNotEmpty()) { "No Native JUnit reports: $directories" }
        for (report in reports) {
            val suite = report.safeXml().documentElement
            val name = suite.getAttribute("name").substringAfterLast('.')
            val count = expected[name] ?: continue
            check(found.put(name, count) == null) { "Duplicate Native test report: $name" }
            check(suite.getAttribute("tests").toInt() == count && listOf("errors", "failures", "skipped").all { suite.getAttribute(it).ifEmpty { "0" }.toInt() == 0 }) { "Failed, skipped or missing tests in $report" }
        }
        check(found == expected) { "Missing Native test classes: ${expected.keys - found.keys}" }
        println("PASS: ${found.values.sum()} maintained Native regressions")
    }

    fun verifyHost(repository: File, target: String) {
        checkSources()
        verify(repository, target)
        verifyReports(listOf(nativeDirectory.resolve("vendor/build/compose-ui")), expectedTests(target, false))
        val skikoBuild = nativeDirectory.resolve("vendor/skiko/workspace/skiko")
        verifyReports(listOf(skikoBuild.resolve("build"), skikoBuild.resolve("skiko-skottie/build")), expectedTests(target, true))
    }

    fun assemble(inputs: Map<String, File>, output: File) {
        check(inputs.keys == targetSuffixes.keys && !output.exists()) { "Assembly requires three hosts and a fresh output directory" }
        val destination = output.canonicalFile.toPath()
        for ((target, input) in inputs) {
            val path = input.canonicalFile.toPath()
            check(!destination.startsWith(path) && !path.startsWith(destination)) { "Maven input and output must not overlap" }
            check(input.walkTopDown().none { Files.isSymbolicLink(it.toPath()) }) { "Symlink in Maven input: $input" }
            verify(input, target)
        }
        val selections = mutableListOf<Pair<File, String>>()
        for (artifact in listOf("skiko", "skiko-skottie") + modules.keys) {
            val (group, _, _) = identity(artifact)
            val prefix = group.replace('.', '/')
            selections.add(inputs.getValue("macos_arm64") to "$prefix/$artifact")
            for ((target, suffix) in targetSuffixes) selections.add(inputs.getValue(target) to "$prefix/$artifact-$suffix")
        }
        // Validate everything before creating output. Each target contributes only its publications.
        for ((input, relative) in selections) check(input.contained(relative).isDirectory) { "Missing host publication: $relative" }
        output.parentFile.mkdirs()
        val staging = Files.createTempDirectory(output.parentFile.toPath(), ".native-maven-").toFile()
        try {
            for ((input, relative) in selections) input.contained(relative).copyTreeTo(staging.resolve(relative))
            for (target in targetSuffixes.keys) verify(staging, target)
            Files.move(staging.toPath(), output.toPath())
        } finally {
            check(staging.canonicalFile.parentFile == output.parentFile.canonicalFile && staging.name.startsWith(".native-maven-"))
            staging.deleteRecursively()
        }
        println("PASS: assembled three-target Maven repository: $output")
    }
}
