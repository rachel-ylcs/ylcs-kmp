package love.yinlin.repository.nativecompose

import java.io.File

internal class ComposeSourceSync(private val sync: NativeUpstreamSync) {
    private val nativeDirectory = sync.nativeDirectory
    private val vendor = sync.vendor
    private val lock = sync.compose
    private val modules = lock["modules"].sourceObject()
    private val targets = setOf("mingw_x64", "linux_x64", "macos_arm64")
    private val families = setOf("ui", "foundation", "animation", "components", "navigation3", "material", "material3", "adaptive")

    fun prepare(offline: Boolean) {
        validate()
        val owner = vendor.resolve("compose")
        sync.workspace.locked(owner) {
            val core = owner.resolve("workspace")
            val selected = modules.filterValues { it.sourceObject()["sourceRepository"] == null }.map { (name, value) -> sourcePath(name, value.sourceObject()) }
            prepareGit(core, owner.resolve("source-edits.json"), owner.resolve("checkout.txt"), lock, selected, sync.patches.manifest["compose"].sourceObject(), offline)
            val resources = lock["resourceSource"].sourceObject()
            prepareGit(vendor.resolve("resources/workspace"), vendor.resolve("resources/source-edits.json"), vendor.resolve("resources/checkout.txt"), resources,
                listOf("components/resources/library"), sync.patches.manifest["resources"].sourceObject(), offline)
            for ((name, value) in modules) {
                val module = value.sourceObject()
                if (module["sourceRepository"] == "maven") prepareMaven(name, module, offline)
            }
            prepareBuild(core)
            println("Prepared Compose ${lock["version"]}: ${modules.keys.joinToString()}")
        }
    }

    private fun validate() {
        for ((name, value) in modules) {
            val module = value.sourceObject()
            val family = module["family"] as? String ?: "ui"
            check(family in families && Regex("$family(?:-[a-z0-9]+)*").matches(name)) { "Invalid Compose module: $name" }
            check((module["redirectTargets"] as List<*>).toSet() == targets && (module["metadataTargets"] as List<*>).toSet() == targets) { "An owned Compose module omits a desktop Native target: $name" }
            val version = module["version"] as? String ?: lock["version"] as String
            val reference = module["versionRef"] as? String ?: "cmp"
            val coordinates = sync.catalog.libraries.getValue(module["alias"] as String)
            check(version == sync.catalog.versions[reference] && coordinates == ("${module["group"] ?: lock["group"]}:$name" to version)) { "Compose Native catalog/lock mismatch: $name" }
            val upstream = module["upstreamGroup"] as? String ?: "org.jetbrains.compose.$family"
            check(Regex("[a-z0-9.]+").matches(upstream))
            val adaptation = adaptation(name, module)
            check(Regex("[a-z0-9.]+_[a-z0-9-]+").matches(adaptation) && nativeDirectory.resolve("adaptation/$adaptation/build.gradle.kts").isFile) { "Missing Compose adaptation: $name" }
            val sourceSets = module["sourceSets"].sourceObject()
            for ((sourceSet, paths) in sourceSets) {
                check(sourceSet in setOf("commonMain", "skikoMain", "nativeMain", "windowsMain", "linuxMain", "macosMain")) { "Invalid Compose source set: $sourceSet" }
                for (path in paths as List<*>) {
                    val parts = (path as String).split('/')
                    check(parts.size == 3 && parts[0] == "src" && parts[2] == "kotlin" && parts[1] in setOf("commonMain", "nonAndroidMain", "nonAndroidExcludingWebMain", "nonJvmMain", "skikoMain", "jbMain", "nativeMain", "darwinMain", "macosMain", "appleMain", "blockingMain")) { "Invalid upstream source directory: $path" }
                    check(parts[1] !in setOf("darwinMain", "appleMain") || sourceSet == "macosMain") { "Cocoa sources must remain in the macOS fragment" }
                }
            }
            val fragments = module["adaptationSourceSets"] as? Map<*, *> ?: emptyMap<String, String>()
            check(fragments.isEmpty() || (name == "animation-graphics" && fragments == mapOf("commonMain" to "nativeMain"))) { "Unreviewed adaptation metadata fragment: $name" }
            check(module["sourceRepository"] in setOf(null, "resources", "maven")) { "Unreviewed source repository: $name" }
            if (module["sourceRepository"] == "resources") check(name == "components-resources" && module["sourcePath"] == "components/resources/library")
            if (module["sourceRepository"] == "maven") check(module["sourcePath"] == name)
        }
    }

    private fun sourcePath(name: String, module: SourceObject): String = module["sourcePath"] as? String ?: "compose/${module["family"] ?: "ui"}/$name"
    private fun adaptation(name: String, module: SourceObject): String = module["adaptation"] as? String ?: "org.jetbrains.compose.${module["family"] ?: "ui"}_$name"
    private fun sourceRoot(module: SourceObject): File = when (module["sourceRepository"]) {
        "maven" -> vendor.resolve("maven/workspace")
        "resources" -> vendor.resolve("resources/workspace")
        else -> vendor.resolve("compose/workspace")
    }

    internal fun prepareGit(directory: File, record: File, marker: File, identity: SourceObject, paths: List<String>, patch: SourceObject, offline: Boolean) {
        val repository = identity["repository"] as String
        val commit = identity["commit"] as String
        check(repository in setOf("JetBrains/compose-multiplatform-core", "JetBrains/compose-multiplatform") && Regex("[a-f0-9]{40}").matches(commit)) { "Unaudited Compose source identity" }
        val url = "https://github.com/$repository.git"
        val fresh = !directory.resolve(".git").isDirectory
        if (fresh) {
            check(!directory.exists() || directory.listFiles().orEmpty().isEmpty()) { "Unrecognized Compose checkout preserved: $directory" }
            check(!offline) { "Offline Compose checkout is missing: $directory" }
            directory.mkdirs()
            sourceGit(directory, "init", "-q")
            sourceGit(directory, "config", "core.autocrlf", "false")
            sourceGit(directory, "remote", "add", "origin", url)
            sourceGit(directory, "config", "remote.origin.promisor", "true")
            sourceGit(directory, "config", "remote.origin.partialclonefilter", "blob:none")
        }
        check(sourceGit(directory, "remote", "get-url", "origin").trim() == url) { "Unexpected Compose origin; checkout preserved" }
        check(sourceGit(directory, "diff", "--cached", "--name-only").isBlank()) { "Staged Compose edits preserved" }
        val current = if (fresh) null else sourceGit(directory, "rev-parse", "HEAD").trim()
        val selected = commit + "\n" + paths.sorted().joinToString("\n", postfix = "\n")
        if (record.isFile) {
            val previous = record.sourceJson()
            check(previous["commit"] == current) { "Managed Compose commit changed; source edits preserved" }
            val files = previous["files"].sourceObject()
            var prepared = true
            for ((relative, hashes) in files) {
                sourceRelativePath(relative)
                val target = checkedSourcePath(vendor, directory.resolve(relative))
                val actual = target.takeIf { it.isFile }?.sourceHash()
                val values = if (hashes is Map<*, *>) setOf(hashes["original"], hashes["prepared"]) else setOf(hashes, null)
                check(actual in values) { "Local Compose source edit preserved: $relative" }
                if (hashes !is Map<*, *> || actual != hashes["prepared"]) prepared = false
            }
            val changed = sourceGit(directory, "diff", "--name-only", "-z").split('\u0000').filter { it.isNotEmpty() }.toSet()
            check((changed - files.keys).isEmpty()) { "Local tracked Compose edits preserved: ${changed - files.keys}" }
            val untracked = sourceGit(directory, "ls-files", "--others", "--exclude-standard", "-z").split('\u0000').filter {
                it.isNotEmpty() && "/src/" in "/$it" && it.substringAfterLast('.') in setOf("kt", "cc", "cpp", "h", "hh", "def")
            }.toSet()
            check((untracked - files.keys).isEmpty()) { "Local untracked Compose sources preserved: ${untracked - files.keys}" }
            if (prepared && current == commit && marker.isFile && marker.readText() == selected && previous["patchSha256"] == patch["sha256"] && files.keys == sync.patches.paths(patch)) return
            val tracked = files.filterValues { hashes -> hashes !is Map<*, *> || hashes["original"] != null }.keys
            for (relative in files.keys - tracked) checkedSourcePath(vendor, directory.resolve(relative)).delete()
            if (tracked.isNotEmpty()) sourceGit(directory, "restore", "--source=HEAD", "--worktree", "--", *tracked.toTypedArray())
        }
        check(sourceGit(directory, "diff", "--name-only").isBlank()) { "Local tracked Compose edits preserved" }
        val existingSelection = if (!fresh && current == commit && !marker.isFile) sourceGit(directory, "sparse-checkout", "list").lineSequence().filter { it.isNotBlank() }.toSet() else emptySet()
        if (current == commit && !marker.isFile && existingSelection == paths.toSet()) writeSource(vendor, marker, selected.toByteArray())
        if (current != commit || !marker.isFile || marker.readText() != selected) {
            check(!offline) { "Compose checkout does not match the locked source selection; prepare online once" }
            sourceGit(directory, "sparse-checkout", "init", "--cone")
            sourceGit(directory, "sparse-checkout", "set", *paths.toTypedArray())
            sourceGit(directory, "fetch", "--filter=blob:none", "--depth=1", "origin", commit)
            sourceGit(directory, "checkout", "--detach", commit)
            check(sourceGit(directory, "rev-parse", "HEAD").trim() == commit)
            writeSource(vendor, marker, selected.toByteArray())
        }
        sync.patches.prepareGitPatch(directory, record, commit, patch)
    }

    private fun prepareMaven(name: String, module: SourceObject, offline: Boolean) {
        val version = module["version"] as? String ?: lock["version"] as String
        val group = module["upstreamGroup"] as String
        check(group in setOf("org.jetbrains.androidx.navigation3", "org.jetbrains.compose.material", "org.jetbrains.compose.material3", "org.jetbrains.compose.material3.adaptive")) { "Unaudited Maven source group: $group" }
        val source = module["sourceArchive"].sourceObject()
        val expected = "https://repo.maven.apache.org/maven2/${group.replace('.', '/')}/$name-macosarm64/$version/$name-macosarm64-$version-sources.jar"
        check(source["url"] == expected && Regex("\\d+\\.\\d+\\.\\d+(?:-[A-Za-z0-9.]+)?").matches(version)) { "Unaudited Maven source identity: $name" }
        val archive = vendor.resolve("maven/$name-$version-sources.jar")
        val patch = sync.patches.manifest["maven"].sourceObject()[name]?.sourceObject()
        if (patch != null) {
            check(patch["sourceSha256"] == source["sha256"]) { "Maven patch identity changed: $name" }
            sync.patches.file(patch)
        }
        val replacements = (module["sourceReplacements"] as? Map<*, *>) ?: emptyMap<String, String>()
        for (relative in replacements.values) check(nativeDirectory.resolve("adaptation/${adaptation(name, module)}/${sourceRelativePath(relative as String)}").isFile) { "Missing maintained replacement: $relative" }
        val destination = vendor.resolve("maven/workspace/$name")
        val record = vendor.resolve("maven/states/$name.json")
        val identity = mapOf("schema" to 4, "url" to expected, "sha256" to source["sha256"], "fingerprint" to bytesHash(sourceBytes(mapOf("module" to module, "patch" to patch, "inputs" to sync.inputHashes()))))
        if (sync.workspace.reusable(destination, record, identity)) return
        downloadSource(expected, archive, source["sha256"] as String, offline, 32L * 1024 * 1024)
        val selected = module["sourceSets"].sourceObject().values.flatMap { it as List<*> }.map { (it as String).split('/')[1] }.toSet()
        sync.stage(".maven-source-") { staging ->
            SourceArchive.extract(archive, staging, false) { path ->
                val first = path.substringBefore('/')
                when {
                    first in selected && path.endsWith(".kt") -> "src/$first/kotlin/${path.substringAfter('/')}"
                    path == source["licensePath"] -> "LICENSE.txt"
                    else -> null
                }
            }
            if (patch != null) {
                sync.patches.apply(staging, patch)
            }
            val exclusions = (module["excludeSources"] as? List<*>)?.map { it as String }?.toSet() ?: emptySet()
            check(exclusions == replacements.keys) { "Missing maintained source replacement provenance: $name" }
            for (relative in exclusions) check(!staging.resolve("src/${relative.substringBefore('/')}/kotlin/${relative.substringAfter('/')}").exists()) { "Upstream actual shadow remains: $relative" }
            check(staging.resolve("LICENSE.txt").isFile && staging.walkTopDown().any { it.extension == "kt" }) { "Missing Maven source/license: $name" }
            // Earlier source records are validated before migration to per-file hashes.
            migrateMavenRecord(name, destination, record)
            sync.workspace.synchronize(staging, destination, record, identity)
        }
    }

    private fun migrateMavenRecord(name: String, destination: File, record: File) {
        if (!record.isFile) return
        val previous = record.sourceJson()
        if (previous.containsKey("fileHashes")) return
        val prefix = "workspace/$name/"
        val hashes = previous["files"].sourceObject().mapKeys { (path, _) ->
            check(path.startsWith(prefix)) { "Invalid legacy Maven source record: $name" }
            sourceRelativePath(path.removePrefix(prefix))
        }
        for ((relative, expected) in hashes) {
            val file = checkedSourcePath(vendor, destination.resolve(relative))
            val actual = file.takeIf { it.isFile }?.sourceHash()
            val allowed = if (expected is List<*>) expected else listOf(expected)
            check(actual == null || actual in allowed) { "Local Maven source edit preserved: $file" }
        }
        writeSource(vendor, record, sourceBytes(previous + ("files" to hashes.keys.sorted()) + ("fileHashes" to hashes)))
    }

    private fun prepareBuild(core: File) {
        check(core.resolve("LICENSE.txt").isFile) { "Compose source license is missing" }
        val projects = mutableListOf<String>()
        for ((name, value) in modules) {
            val module = value.sourceObject()
            val directory = sourceRoot(module).resolve(sourcePath(name, module))
            for (paths in module["sourceSets"].sourceObject().values) for (relative in paths as List<*>) {
                val source = directory.resolve(relative as String)
                check(source.isDirectory && source.walkTopDown().any { it.extension == "kt" }) { "Selected upstream source directory is empty: $source" }
            }
            val projectPath = directory.relativeTo(sync.repository).invariantSeparatorsPath
            projects.addAll(listOf("if (\"$name\" in selectedModules) {", "    include(\":$name\")", "    project(\":$name\").apply {",
                "        projectDir = repositoryDirectory.resolve(\"$projectPath\").canonicalFile",
                "        buildFileName = projectDir.toPath().relativize(repositoryDirectory.resolve(\"native-compose/adaptation/${adaptation(name, module)}/build.gradle.kts\").toPath()).toString()", "    }", "}"))
        }
        val build = core.resolve("native-desktop")
        val templates = nativeDirectory.resolve("gradle/templates")
        writeSource(vendor, build.resolve("settings.gradle.kts"), templates.resolve("compose-ui.settings.gradle.kts.template").readText().replace("__PROJECTS__", projects.joinToString("\n")).toByteArray())
        writeSource(vendor, build.resolve("build.gradle.kts"), templates.resolve("compose-ui.build.gradle.kts.template").readText().replace("__JAVA_VERSION__", sync.catalog.java.toString()).toByteArray())
        val properties = linkedMapOf("kotlin.mpp.applyDefaultHierarchyTemplate" to "false")
        Regex("(?m)^kotlin\\.native\\.ignoreDisabledTargets[ \\t]*=[ \\t]*([^\r\n]*)").find(sync.repository.resolve("gradle.properties").readText())?.let { properties["kotlin.native.ignoreDisabledTargets"] = it.groupValues[1].trim() }
        writeSource(vendor, build.resolve("gradle.properties"), properties.entries.joinToString("\n", postfix = "\n") { "${it.key}=${it.value}" }.toByteArray())
        build.resolve("gradle/wrapper").mkdirs()
        if (!build.resolve("gradle/wrapper/gradle-wrapper.properties").exists()) build.resolve("gradle/wrapper/gradle-wrapper.properties").writeText("")
        sync.catalog.copyWrappers(build)
        writeSource(vendor, vendor.resolve("compose/state.json"), sourceBytes(mapOf("schema" to 3, "build" to "vendor/compose/workspace/native-desktop", "commit" to lock["commit"],
            "inputs" to sync.inputHashes(), "catalogSha256" to sync.catalog.file.sourceHash(), "version" to lock["version"], "modules" to modules.keys.toList(), "java" to sync.catalog.java,
            "moduleVersions" to modules.mapValues { (_, module) -> module.sourceObject()["version"] ?: lock["version"] })))
    }
}
