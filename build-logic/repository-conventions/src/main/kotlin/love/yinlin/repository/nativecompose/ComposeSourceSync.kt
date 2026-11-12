package love.yinlin.repository.nativecompose

import java.io.File

internal class ComposeSourceSync(private val sync: NativeUpstreamSync, private val generation: File, private val metadata: SourceObject) {
    private val nativeDirectory = sync.nativeDirectory
    private val vendor = sync.vendor
    private val modules = metadata["modules"].sourceObject()
    private val targets = setOf("mingw_x64", "linux_x64", "macos_arm64")
    private val families = setOf("ui", "foundation", "animation", "components", "navigation3", "material", "material3", "adaptive")

    fun prepare(core: NativeSourceInputs.GitTree, resources: NativeSourceInputs.GitTree, archives: Map<String, NativeSourceInputs.Archive>) {
        validate()
        prepareTree("compose", core)
        prepareTree("resources", resources)
        for ((name, archive) in archives) prepareMaven(name, modules.getValue(name).sourceObject(), archive)
        prepareBuild(generation.resolve("compose/workspace"))
    }

    private fun validate() {
        for ((name, value) in modules) {
            val module = value.sourceObject()
            val family = module["family"] as? String ?: "ui"
            check(family in families && Regex("$family(?:-[a-z0-9]+)*").matches(name)) { "Invalid Compose module: $name" }
            check((module["redirectTargets"] as List<*>).toSet() == targets && (module["metadataTargets"] as List<*>).toSet() == targets) { "An owned Compose module omits a desktop Native target: $name" }
            val upstream = module["upstreamGroup"] as? String ?: "org.jetbrains.compose.$family"
            check((module["group"] ?: metadata["group"]) == "love.yinlin.native.${upstream.removePrefix("org.jetbrains.")}") { "Native publication namespace mismatch: $name" }
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

    private fun adaptation(name: String, module: SourceObject): String = module["adaptation"] as? String ?: "org.jetbrains.compose.${module["family"] ?: "ui"}_$name"
    private fun sourceRoot(module: SourceObject): File = when (module["sourceRepository"]) {
        "maven" -> generation.resolve("maven/workspace")
        "resources" -> generation.resolve("resources/workspace")
        else -> generation.resolve("compose/workspace")
    }

    private fun prepareTree(name: String, tree: NativeSourceInputs.GitTree) {
        val destination = generation.resolve("$name/workspace")
        val record = generation.resolve("$name/files.json")
        val identity = tree.metadata + ("patch" to sync.patches.manifest[name])
        if (sync.workspace.reusable(destination, record, identity)) return
        sync.stage(".compose-source-") { staging ->
            for (relative in tree.files.keys) tree.directory.resolve(relative).copyTo(staging.resolve(relative))
            sync.patches.apply(staging, sync.patches.manifest[name].sourceObject())
            sync.workspace.synchronize(staging, destination, record, identity)
        }
    }

    private fun prepareMaven(name: String, module: SourceObject, archive: NativeSourceInputs.Archive) {
        val source = module["sourceArchive"].sourceObject()
        val patch = sync.patches.manifest["maven"].sourceObject()[name]?.sourceObject()
        val replacements = (module["sourceReplacements"] as? Map<*, *>) ?: emptyMap<String, String>()
        for (relative in replacements.values) check(nativeDirectory.resolve("adaptation/${adaptation(name, module)}/${sourceRelativePath(relative as String)}").isFile) { "Missing maintained replacement: $relative" }
        val destination = generation.resolve("maven/workspace/$name")
        val record = generation.resolve("maven/states/$name.json")
        val identity = mapOf("sha256" to archive.metadata["sha256"], "module" to module, "patch" to patch)
        if (sync.workspace.reusable(destination, record, identity)) return
        val selected = module["sourceSets"].sourceObject().values.flatMap { it as List<*> }.map { (it as String).split('/')[1] }.toSet()
        sync.stage(".maven-source-") { staging ->
            SourceArchive.extract(archive.file, staging, false) { path ->
                val first = path.substringBefore('/')
                when {
                    first in selected && path.endsWith(".kt") -> "src/$first/kotlin/${path.substringAfter('/')}"
                    path == source["licensePath"] -> "LICENSE.txt"
                    else -> null
                }
            }
            if (patch != null) sync.patches.apply(staging, patch)
            val exclusions = (module["excludeSources"] as? List<*>)?.map { it as String }?.toSet() ?: emptySet()
            check(exclusions == replacements.keys) { "Missing maintained source replacement provenance: $name" }
            for (relative in exclusions) check(!staging.resolve("src/${relative.substringBefore('/')}/kotlin/${relative.substringAfter('/')}").exists()) { "Upstream actual shadow remains: $relative" }
            check(staging.resolve("LICENSE.txt").isFile && staging.walkTopDown().any { it.extension == "kt" }) { "Missing Maven source/license: $name" }
            sync.workspace.synchronize(staging, destination, record, identity)
        }
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
        writeSource(vendor, build.resolve("settings.gradle.kts"), templates.resolve("compose-ui.settings.gradle.kts.template").readText()
            .replace("__REPOSITORY__", build.toPath().relativize(sync.repository.toPath()).toString().replace(File.separatorChar, '/'))
            .replace("__PROJECTS__", projects.joinToString("\n")).toByteArray())
        writeSource(vendor, build.resolve("build.gradle.kts"), templates.resolve("compose-ui.build.gradle.kts.template").readText().replace("__JAVA_VERSION__", sync.catalog.java.toString()).toByteArray())
        val properties = linkedMapOf("kotlin.mpp.applyDefaultHierarchyTemplate" to "false")
        properties.putAll(sync.kotlinProperties())
        writeSource(vendor, build.resolve("gradle.properties"), properties.entries.joinToString("\n", postfix = "\n") { "${it.key}=${it.value}" }.toByteArray())
        build.resolve("gradle/wrapper").mkdirs()
        if (!build.resolve("gradle/wrapper/gradle-wrapper.properties").exists()) build.resolve("gradle/wrapper/gradle-wrapper.properties").writeText("")
        sync.catalog.copyWrappers(build)
    }

    companion object {
        fun sourcePath(name: String, module: SourceObject): String = module["sourcePath"] as? String ?: "compose/${module["family"] ?: "ui"}/$name"
    }
}
