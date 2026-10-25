"""Rachel's small, reviewable delta over the locked Skiko three-way source merge."""
from pathlib import Path
import json
import os
import re
import shutil
import tomllib
from upstream_versions import align_catalog, main_profile


def replace_once(text, before, after):
    if text.count(before) != 1:
        raise ValueError(f"Upstream drift: expected one occurrence of {before!r}")
    return text.replace(before, after, 1)


def remove_block(text, marker):
    """Remove a reviewed optional Gradle block, retaining the rest of the upstream script."""
    while marker in text:
        start = text.index(marker)
        opening = text.index("{", start)
        depth = 1
        end = opening + 1
        while depth and end < len(text):
            if text[end] == "{":
                depth += 1
            elif text[end] == "}":
                depth -= 1
            end += 1
        if depth:
            raise ValueError(f"Unbalanced upstream block: {marker}")
        text = text[:start] + "// Rachel core build excludes Android configuration.\n" + text[end:]
    return text


def resolve_merge(path, text):
    pattern = r"<<<<<<< UPSTREAM\n(.*?)=======\n(.*?)>>>>>>> PORT\n"
    conflicts = list(re.finditer(pattern, text, re.DOTALL))
    if path.endswith("/publishing.kt") and len(conflicts) == 1:
        text = re.sub(pattern, lambda m: m[1].replace("SkikoArtifacts.DEFAULT_GROUP_ID", "skikoArtifacts.groupId"), text, flags=re.DOTALL)
    elif path.endswith("/NativeTasksConfiguration.kt") and len(conflicts) == 2:
        text = re.sub(pattern, lambda m: m[2], text, flags=re.DOTALL)
        # Preserve 0.156's deduplication on ELF/Mach-O. COFF uses the port's CRT filtering.
        text = replace_once(text, "    val nativeArchives = resolvedBinaryInputs.staticArchivePaths.distinct()", '''    val mergedArchive = layout.buildDirectory.file("mergedSkiaArchives/$targetString/libskia_static.a")
    val mergeArchivesTask = if (os == OS.Windows) null else
        project.tasks.register<MergeStaticArchivesTask>("mergeSkiaArchives$targetString") {
            dependsOn(unzipper)
            targetOs.set(os)
            archives.set(resolvedBinaryInputs.staticArchivePaths.distinct().map(::File))
            output.set(mergedArchive)
        }
    val nativeArchives = if (os == OS.Windows) resolvedBinaryInputs.staticArchivePaths.distinct()
        else listOf(mergedArchive.get().asFile.absolutePath)''')
        text = text.replace("dependsOn(mergeArchivesTask)", "mergeArchivesTask?.let { dependsOn(it) }")
    else:
        raise ValueError(f"Unreviewed merge conflict: {path}")
    if "<<<<<<<" in text or ">>>>>>>" in text:
        raise ValueError(f"Unresolved conflict: {path}")
    return text


def adapt_kotlin_sources(root: Path):
    """Apply the pinned source's Kotlin 2.5 compatibility changes before compilation."""
    sources = root / "skiko/src"
    path = sources / "commonMain/kotlin/org/jetbrains/skia/Matrix33.kt"
    # Retain the explicit inline decision for this tiny conversion; the cost
    # heuristic is informational and is suppressed only on this declaration.
    text = replace_once(path.read_text(encoding="utf-8"), "internal inline fun Float.toRadians()",
                        '@Suppress("NOTHING_TO_INLINE")\ninternal inline fun Float.toRadians()')
    path.write_text(text, encoding="utf-8", newline="\n")

    path = sources / "nativeMain/kotlin/org/jetbrains/skia/Actuals.native.kt"
    text = replace_once(path.read_text(encoding="utf-8"), "    block()\n", "    val _ = block()\n")
    path.write_text(text, encoding="utf-8", newline="\n")

    path = sources / "nativeMain/kotlin/org/jetbrains/skia/impl/Stats.native.kt"
    text = replace_once(path.read_text(encoding="utf-8"), "        if (enabled) nativeCalls.incrementAndGet()",
                        "        if (enabled) {\n            val _ = nativeCalls.incrementAndGet()\n        }")
    for operation in ["incrementAndGet", "decrementAndGet"]:
        text = replace_once(text, f"            allocated.{operation}()", f"            val _ = allocated.{operation}()")
    path.write_text(text, encoding="utf-8", newline="\n")

    # This is Native-only code now. Bind directly to the same Native symbol
    # annotation instead of suppressing optional-expectation errors inherited from JS.
    path = sources / "nativeMain/kotlin/org/jetbrains/skia/PaintFilterCanvas.shared.kt"
    text = replace_once(path.read_text(encoding="utf-8"),
                        '@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE") // https://youtrack.jetbrains.com/issue/KT-50727\n\n',
                        '@file:OptIn(kotlin.native.SymbolNameIsInternal::class)\n\n')
    for symbol in ["org_jetbrains_skia_PaintFilterCanvas__1nInit", "org_jetbrains_skia_PaintFilterCanvas__1nGetOnFilterPaint"]:
        text = replace_once(text, f'@ExternalSymbolName("{symbol}")', f'@kotlin.native.SymbolName("{symbol}")')
    path.write_text(text, encoding="utf-8", newline="\n")

    path = sources / "nativeMain/kotlin/org/jetbrains/skia/shaper/Shaper.shared.kt"
    text = replace_once(path.read_text(encoding="utf-8"), '            @Suppress("CAST_NEVER_SUCCEEDS")\n', "")
    for name in ["managedFontIter", "managedBidiIter", "managedScriptIter", "managedLangIter", "managedRunHandler"]:
        text = replace_once(text, f"getPtr({name}) as InteropPointer", f"getPtr({name})")
    path.write_text(text, encoding="utf-8", newline="\n")


def prepare_notices(root: Path, lock, own_root: Path):
    """Keep source publication notices derived from the audited inputs."""
    notices = root / "skiko/rachel-notices"
    notices.mkdir()
    shutil.copyfile(root / "LICENSE", notices / "Apache-2.0.txt")
    shutil.copyfile(root / "NOTICE", notices / "NOTICE")
    shutil.copyfile(own_root / "upstream/adaptation-origins.json", notices / "adaptation-origins.json")
    (notices / "upstream-lock.json").write_text(json.dumps(lock, indent=4) + "\n", encoding="utf-8")
    sdl = json.loads((own_root / "upstream/sdl.json").read_text(encoding="utf-8"))
    text = "# Native desktop source provenance\n\n"
    text += "These are Rachel's modified Native desktop builds, not official JetBrains Skiko releases.\n\n"
    for name, entry in lock["sources"].items():
        text += f"- {name}: https://github.com/{entry['repository']} at {entry['commit']}.\n"
    text += f"- Skia: {lock['skia']}; archive hashes are recorded in upstream-lock.json.\n"
    text += f"- SDL {sdl['version']}: {sdl['source']['url']}; SHA-256 {sdl['source']['sha256']}.\n\n"
    text += "Original copyright and license notices remain in the upstream sources. "
    text += "Apache-2.0.txt and NOTICE come directly from Skiko; Core source publications also include "
    text += "SDL.txt from the audited SDL source. adaptation-origins.json records reference-derived files.\n"
    (notices / "THIRD_PARTY_NOTICES.md").write_text(text, encoding="utf-8", newline="\n")


def adapt(root: Path, lock):
    own_root = Path(__file__).resolve().parents[1]
    prepare_notices(root, lock, own_root)
    origins = json.loads((own_root / "upstream/adaptation-origins.json").read_text(encoding="utf-8"))["files"]
    # Existing dependency versions come exclusively from the parent version catalog.
    catalog = root / "dependencies.toml"
    align_catalog(catalog, main_profile(own_root.parent),
        {"kotlin": "kotlin", "coroutines": "coroutines", "androidGradlePlugin": "agp", "dokka": "dokka"})

    props = root / "skiko/gradle.properties"
    text = props.read_text(encoding="utf-8")
    kotlin_language_version = ".".join(lock["versions"]["kotlin"].split(".")[:2])
    # Three-host CI builds each platform's CInterop on its matching host. KGP
    # reports this expected host limitation separately from disabled-task warnings.
    warning_property = "kotlin.suppressGradlePluginWarnings"
    previous_warnings = re.search(rf"(?m)^{re.escape(warning_property)}[ \t]*=[ \t]*([^\r\n]*)", text)
    suppressed_warnings = [value.strip() for value in previous_warnings[1].split(",") if value.strip()] if previous_warnings else []
    if "CrossCompilationWithCinterops" not in suppressed_warnings:
        suppressed_warnings.append("CrossCompilationWithCinterops")
    changes = {
        warning_property: ",".join(suppressed_warnings),
        "kotlin.mpp.enableCInteropCommonization": "true",
        "deploy.version": lock["versions"]["skiko"],
        "deploy.group": lock["group"],
        "deploy.release": "true",
        "dependencies.skia": lock["skia"],
        "skiko.kotlin.language.version": kotlin_language_version,
        "skiko.kotlin.api.version": kotlin_language_version,
        "skiko.awt.enabled": "false",
        "skiko.native.windows.enabled": "true",
        "skiko.native.linux.x64.enabled": "true",
        "skiko.native.linux.arm64.enabled": "false",
        "skiko.native.mac.enabled": "true",
        "kotlin.mpp.enableCInteropCommonization": "true",
    }
    for key, value in changes.items():
        pattern = rf"(?m)^{re.escape(key)}=.*$"
        if re.search(pattern, text):
            text = re.sub(pattern, f"{key}={value}", text)
        else:
            text += f"\n{key}={value}\n"
    props.write_text(text, encoding="utf-8", newline="\n")

    settings = root / "skiko/settings.gradle.kts"
    text = settings.read_text(encoding="utf-8")
    for module in ["ci", "docs", "import-generator", "skiko-graphite"]:
        text = replace_once(text, f'include("{module}")', f'// Rachel: {module} is outside the core Native build.')
    settings.write_text(text, encoding="utf-8", newline="\n")

    # No Android target is built here. Keep AGP out of the Native build's runtime classpath.
    build_src = root / "skiko/buildSrc/build.gradle.kts"
    text = build_src.read_text(encoding="utf-8")
    for dependency in ["android.gradlePlugin", "dokka.gradlePlugin", "kotlinx.benchmark.gradlePlugin"]:
        text = replace_once(text, f"    implementation(libs.{dependency})\n", "")
    build_src.write_text(text, encoding="utf-8", newline="\n")
    for relative in ["skiko/build.gradle.kts", "skiko/test-utils/build.gradle.kts"]:
        path = root / relative
        text = path.read_text(encoding="utf-8")
        text = replace_once(text, "import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget\n", "")
        text = replace_once(text, '    id("com.android.kotlin.multiplatform.library") apply false\n', "")
        text = remove_block(text, "if (supportAndroid) {")
        for flag in ["supportAwt", "supportJs", "supportWasm", "supportNativeIosArm64", "supportNativeIosSimulatorArm64", "supportNativeIosX64", "supportNativeTvosArm64", "supportNativeTvosSimulatorArm64", "supportNativeTvosX64"]:
            text = remove_block(text, f"if ({flag}) {{")
        # Keep the reviewed Apple implementation, without adding an unrequested x64 target.
        macos_x64 = "        macosX64()\n" if relative.startswith("skiko/test-utils/") else "        skikoProjectContext.configureNativeTarget(OS.MacOS, Arch.X64, macosX64())\n"
        text = replace_once(text, macos_x64, "")
        if relative == "skiko/test-utils/build.gradle.kts":
            text = replace_once(text, """    if (supportNativeLinux) {
        linuxX64()
        linuxArm64()
    }""", """    if (supportNativeLinuxX64) {
        linuxX64()
    }
    if (supportNativeLinuxArm64) {
        linuxArm64()
    }""")
        text = text.replace("    org.jetbrains.dokka\n", "").replace("    org.jetbrains.kotlinx.benchmark\n", "")
        path.write_text(text, encoding="utf-8", newline="\n")
    for relative in ["skiko/build.gradle.kts", "skiko/settings.gradle.kts", "skiko/buildSrc/settings.gradle.kts", "skiko/test-utils/build.gradle.kts"]:
        path = root / relative
        text = path.read_text(encoding="utf-8").replace("https://cache-redirector.jetbrains.com/maven-central", "https://repo.maven.apache.org/maven2")
        path.write_text(text, encoding="utf-8", newline="\n")

    core_build = root / "skiko/build.gradle.kts"
    text = remove_block(core_build.read_text(encoding="utf-8"), "if (supportNativeWindows) {")
    core_build.write_text(text, encoding="utf-8", newline="\n")

    context = root / "skiko/buildSrc/src/main/kotlin/SkikoProjectContext.kt"
    text = context.read_text(encoding="utf-8")
    # Immutable, audited archives are seeded by sync_upstream.py; do not revalidate
    # them over HTTP during IDEA import. Extraction still verifies SHA-256 below.
    text = replace_once(text, "                onlyIfModified(true)", "                overwrite(false)")
    # Upstream enables every platform in IDEA, including excluded import-generator/AWT
    # modules. The Native core must expose the same explicit targets in CLI and IDE.
    text = text.replace(" // || isInIdea", "")
    text, idea_overrides = re.subn(r"\s+\|\|\s+isInIdea\b", "", text)
    if not idea_overrides:
        raise ValueError("Upstream drift: missing IDEA platform overrides")
    checksums = ",\n".join(f'                        "{key}" to "{value}"' for key, value in lock["skiaAssets"].items())
    verification = '''                dependsOn(downloadSkiaTask)
                doFirst {
                    val lockedChecksums = mapOf(
CHECKSUMS
                    )
                    val expected = lockedChecksums["$config-$buildType-$arch"]
                        ?: error("No audited Skia archive for $config-$buildType-$arch")
                    val sha256 = java.security.MessageDigest.getInstance("SHA-256")
                    java.security.DigestInputStream(downloadSkiaTask.get().dest.inputStream(), sha256).use {
                        it.transferTo(java.io.OutputStream.nullOutputStream())
                    }
                    val actual = sha256.digest().joinToString("") { "%02x".format(it) }
                    check(actual == expected) { "Skia archive SHA-256 mismatch: $artifactId" }
                }
                from(project.zipTree'''.replace("CHECKSUMS", checksums)
    text = replace_once(text, "                dependsOn(downloadSkiaTask)\n                from(project.zipTree", verification)
    context.write_text(text, encoding="utf-8", newline="\n")

    # Share BuildDesktopNativeTask's VS_PATH convention, retaining the upstream override.
    windows_sdk = root / "skiko/buildSrc/src/main/kotlin/windowsSdkPaths.kt"
    text = windows_sdk.read_text(encoding="utf-8")
    text = replace_once(text, "        val skikoVsbtPath = System.getenv(ENV_SKIKO_VSBT_PATH)", """        val skikoVsbtPath = System.getenv(ENV_SKIKO_VSBT_PATH)?.takeIf(String::isNotBlank)
            ?: System.getenv("VS_PATH")?.takeIf(String::isNotBlank)""")
    windows_sdk.write_text(text, encoding="utf-8", newline="\n")
    properties = root / "skiko/buildSrc/src/main/kotlin/properties.kt"
    text = properties.read_text(encoding="utf-8")
    text = replace_once(text, 'System.getenv()["SKIKO_VSBT_PATH"]', '(System.getenv("SKIKO_VSBT_PATH")?.takeIf(String::isNotBlank) ?: System.getenv("VS_PATH"))')
    properties.write_text(text, encoding="utf-8", newline="\n")

    # Native maintenance/CI only writes Maven verification files on disk.
    # Do not retain upstream's credential-based remote publication destination.
    publishing = root / "skiko/buildSrc/src/main/kotlin/publishing.kt"
    text = replace_once(publishing.read_text(encoding="utf-8"), '''            maven {
                name = "ComposeRepo"
                url = project.uri(skiko.composeRepoUrl)
                credentials {
                    username = skiko.composeRepoUserName
                    password = skiko.composeRepoKey
                }
            }
''', "")
    publishing.write_text(text, encoding="utf-8", newline="\n")

    # Native task linker flags are serialized in the KLIB. Never publish build-machine paths.
    # -include-binary embeds archives; cinterop exports only portable system link flags.
    native = root / "skiko/buildSrc/src/main/kotlin/tasks/configuration/NativeTasksConfiguration.kt"
    text = native.read_text(encoding="utf-8")
    text = text.replace("            options.addAll(resolvedBinaryInputs.directStaticArchivePaths)\n", "")
    text = text.replace('            options.add("--version-script=${hiddenSymbolsFile.get().asFile.absolutePath}")\n', "")
    text = replace_once(text, '''        OS.MacOS -> {
            configureCinterop(cinteropName, os, arch, target, targetString, resolvedBinaryInputs.frameworks)
            mutableListOfLinkerOptions(resolvedBinaryInputs.frameworks + resolvedBinaryInputs.linkFlags + listOf(
                "-unexported_symbols_list",
                hiddenSymbolsFile.get().asFile.absolutePath
            ))
        }''', '''        OS.MacOS -> {
            configureCinterop(cinteropName, os, arch, target, targetString, resolvedBinaryInputs.frameworks)
            // KLIBs must remain linkable after leaving the producer's machine.
            mutableListOfLinkerOptions(resolvedBinaryInputs.frameworks + resolvedBinaryInputs.linkFlags)
        }''')
    # NativeCompile linker flags alone are not exported in the KLIB manifest.
    # Consumers (including test binaries and Maven users) need the system libs too.
    text = replace_once(text, '''            mutableListOfLinkerOptions(options)
        }
        OS.Windows -> {''', '''            configureCinterop(cinteropName, os, arch, target, targetString, options)
            mutableListOfLinkerOptions(options)
        }
        OS.Windows -> {''')
    text = replace_once(text, '''                    *project.appleMacOsSdkFlags().toTypedArray(),
                    *buildType.clangFlags,''', '''                    *project.appleMacOsSdkFlags().toTypedArray(),
                    "-mmacosx-version-min=12.0",
                    *buildType.clangFlags,''')
    native.write_text(text, encoding="utf-8", newline="\n")

    # Gradle 9 rejects an explicitly null ExecSpec working directory. The default
    # is valid; only archive extraction needs an explicit directory.
    merge_archives = root / "skiko/buildSrc/src/main/kotlin/MergeStaticArchivesTask.kt"
    text = merge_archives.read_text(encoding="utf-8")
    text = replace_once(text, "        workingDir = cwd\n", "        if (cwd != null) workingDir = cwd\n")
    merge_archives.write_text(text, encoding="utf-8", newline="\n")

    # Native-only extensions can publish an ordinary KMP project dependency on Core.
    skottie = root / "skiko/skiko-skottie/build.gradle.kts"
    text = skottie.read_text(encoding="utf-8")
    text = replace_once(text, "import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget\n", "")
    text = replace_once(text, '    id("com.android.kotlin.multiplatform.library") apply false\n', "")
    for plugin in ["org.jetbrains.dokka", "org.jetbrains.kotlinx.benchmark"]:
        text = text.replace(f"    {plugin}\n", "")
    for flag in ["supportAndroid", "supportAwt", "supportWeb", "supportNativeIosArm64", "supportNativeIosSimulatorArm64", "supportNativeIosX64", "supportNativeTvosArm64", "supportNativeTvosSimulatorArm64", "supportNativeTvosX64"]:
        text = remove_block(text, f"if ({flag}) {{")
    text = replace_once(text, '    artifactIdPrefix = "skiko-skottie",', '    groupId = skiko.deployGroup,\n    artifactIdPrefix = "skiko-skottie",')
    text = replace_once(text, "        skikoSkottieProjectContext.configureNativeTarget(OS.MacOS, Arch.X64, macosX64(), ::coreNativeSymbolSources)\n", "")
    text = replace_once(text, """    if (supportNativeLinux) {
        skikoSkottieProjectContext.configureNativeTarget(OS.Linux, Arch.X64, linuxX64(), ::coreNativeSymbolSources)
        skikoSkottieProjectContext.configureNativeTarget(OS.Linux, Arch.Arm64, linuxArm64(), ::coreNativeSymbolSources)
    }""", """    if (supportNativeLinuxX64) {
        skikoSkottieProjectContext.configureNativeTarget(OS.Linux, Arch.X64, linuxX64(), ::coreNativeSymbolSources)
    }""")
    text = replace_once(text, '        compileOnly(project(":"))', '')
    text = re.sub(r"        /\*\n        We use compileOnly.*?         \*/\n", "", text, flags=re.DOTALL)
    text = text.replace("https://cache-redirector.jetbrains.com/maven-central", "https://repo.maven.apache.org/maven2")
    skottie.write_text(text, encoding="utf-8", newline="\n")

    # CRT and SDK aliases belong to Core; extensions obtain them through their API dependency.
    text = native.read_text(encoding="utf-8")
    text = replace_once(text, "        val runtimeAliases =\n            mapOf(", "        val runtimeAliases = if (kind != SkikoModuleKind.CORE) emptyMap<File, String>() else\n            mapOf(")
    text = replace_once(text, '            listOf("libcmt.lib", "libcpmt.lib", "libvcruntime.lib").map {', '            (if (kind == SkikoModuleKind.CORE) listOf("libcmt.lib", "libcpmt.lib", "libvcruntime.lib") else emptyList()).map {')
    native.write_text(text, encoding="utf-8", newline="\n")

    core_build = root / "skiko/build.gradle.kts"
    text = core_build.read_text(encoding="utf-8")
    text = replace_once(text, 'dynamicSystemLibs("fontconfig", "GL")', 'dynamicSystemLibs("fontconfig", "GL", "dl")')
    core_build.write_text(text, encoding="utf-8", newline="\n")

    # Compile maintained files in-place. Remove their upstream shadows from the
    # generated working tree; locked archives remain unchanged.
    for module, project_dir, context_name in [
        ("org.jetbrains.skiko_skiko", root / "skiko", "skikoProjectContext"),
        ("org.jetbrains.skiko_skiko-skottie", root / "skiko/skiko-skottie", "skikoSkottieProjectContext"),
    ]:
        adaptation = own_root / "adaptation" / module
        for source in (adaptation / "src").rglob("*"):
            if source.is_file():
                (project_dir / "src" / source.relative_to(adaptation / "src")).unlink(missing_ok=True)
                original = origins.get(source.relative_to(own_root).as_posix())
                if original:
                    (root / original).unlink(missing_ok=True)
        build_file = project_dir / "build.gradle.kts"
        binding = '\n    @Suppress("UNCHECKED_CAST")\n'
        binding += f'    (project.extensions.extraProperties["rachel.skiko.configure"] as (SkikoProjectContext) -> Unit)({context_name})\n'
        # Register after Skia tasks/the hierarchy exist, before the publication target snapshot.
        anchor = "    skikoProjectContext.declareSkiaTasks()\n" if module.endswith("_skiko") else "    applyHierarchyTemplate(skikoSourceSetHierarchyTemplate)\n"
        text = replace_once(build_file.read_text(encoding="utf-8"), anchor, anchor + binding)
        build_file.write_text(text, encoding="utf-8", newline="\n")

    # A missing native toolchain must not produce a source-only, un-linkable publication.
    build = root / "skiko/build.gradle.kts"
    text = build.read_text(encoding="utf-8")
    text += '''
// Rachel: all maintained/source-build compilations use the same language features.
allprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            compilerOptions.freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcollection-literals")
        }
    }
}

// Rachel: sources artifacts include compiled native bridge sources and provenance.
tasks.withType<org.gradle.api.tasks.bundling.AbstractArchiveTask>().configureEach {
    if (name == "sourcesJar" || name.endsWith("SourcesJar")) {
        from(rootProject.file("rachel-notices")) { into("META-INF/licenses") }
        listOf("commonMain/cpp", "nativeMain/cpp", "windowsMain/cpp", "linuxMain/cpp").forEach { path ->
            from("src/$path") { into("native-sources/$path") }
        }
    }
}

// Rachel: expose only complete target publications produced on the matching host.
tasks.withType<org.gradle.api.publish.maven.tasks.PublishToMavenRepository>().configureEach {
    doFirst {
        check(repository.url.scheme == "file") {
            "Native Skiko remote Maven publishing is disabled; use the local verification repository."
        }
    }
}

tasks.withType<org.gradle.api.publish.maven.tasks.AbstractPublishToMaven>().configureEach {
    doFirst {
        val publicationName = publication.name
        check(publicationName != "mingwX64" || hostOs == OS.Windows) {
            "Build and publish mingwX64 on Windows with the C++ toolchain installed."
        }
        check(publicationName != "linuxX64" || hostOs == OS.Linux) {
            "Build and publish linuxX64 on Linux with the C++ toolchain installed."
        }
        check(publicationName != "macosArm64" || (hostOs == OS.MacOS && hostArch == Arch.Arm64)) {
            "Build and publish macosArm64 on Apple Silicon macOS with Xcode installed."
        }
    }
}
'''
    build.write_text(text, encoding="utf-8", newline="\n")
    # Apply the same complete-publication guard and source/license provenance to Skottie.
    with skottie.open("a", encoding="utf-8", newline="\n") as output:
        output.write(text[text.index("// Rachel: sources artifacts"):])

    # This fork builds desktop Native only. Flatten historical Native/JS and
    # Apple shared layers into the source sets used by the three desktop targets.
    hierarchy = root / "skiko/buildSrc/src/main/kotlin/sourceHierarchy.kt"
    text = hierarchy.read_text(encoding="utf-8")
    text = remove_block(text, 'group("nativeJs") {')
    text = replace_once(text, '''            group("darwin") {
                group("uikit") {
                    group("tvos")
                    group("ios") {
                        withIos()
                    }
                }

                group("macos") {
                    withMacos()
                }

                group("tvos") {
                    withTvos()
                }
            }''', '''            group("macos") {
                withMacos()
            }''')
    hierarchy.write_text(text, encoding="utf-8", newline="\n")
    for module_dir in [root / "skiko", root / "skiko/skiko-skottie", root / "skiko/test-utils"]:
        for old, new in [("nativeJsMain", "nativeMain"), ("nativeJsTest", "nativeTest"), ("nativeNativeJs", "nativeMain"),
                         ("darwinMain", "macosMain"), ("darwinTest", "macosTest")]:
            directory = module_dir / "src" / old
            if not directory.is_dir():
                continue
            if not directory.resolve().is_relative_to(root.resolve()):
                raise ValueError(f"Unsafe normalized source directory: {directory}")
            for source in directory.rglob("*"):
                if not source.is_file():
                    continue
                relative = source.relative_to(directory).as_posix().replace(".nativejs.kt", ".shared.kt")
                if old.startswith("darwin"):
                    relative = relative.replace(".darwin.kt", ".macos.kt")
                destination = module_dir / "src" / new / relative
                if destination.exists():
                    raise ValueError(f"Native source merge collision: {destination}")
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.move(source, destination)
            shutil.rmtree(directory)
    adapt_kotlin_sources(root)
    for directory in [root / "skiko/buildSrc/src", root / "skiko"]:
        for source in directory.rglob("*.kt" if directory.name == "src" else "*.gradle.kts"):
            text = source.read_text(encoding="utf-8")
            updated = text.replace("src/nativeJsMain/", "src/nativeMain/").replace("src/nativeNativeJs/", "src/nativeMain/").replace("src/nativeJsTest/", "src/nativeTest/")
            updated = updated.replace("src/darwinMain/", "src/macosMain/").replace("src/darwinTest/", "src/macosTest/")
            updated = updated.replace('"src/nativeMain/cpp", "src/nativeMain/cpp"', '"src/nativeMain/cpp"')
            updated = updated.replace('"src/macosMain/cpp", "src/macosMain/cpp"', '"src/macosMain/cpp"')
            if updated != text:
                source.write_text(updated, encoding="utf-8", newline="\n")

    # The maintained script is the actual project build file, so IDEA imports its
    # buildSrc and plugin classpath. Project directories stay upstream: its tasks
    # and buildSrc intentionally resolve source/dependency paths against them.
    libraries = tomllib.loads(catalog.read_text(encoding="utf-8"))["libraries"]
    accessors = '''val kotlin = extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
fun kotlin(configure: org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension.() -> Unit) = kotlin.configure()
val publishing = extensions.getByType<org.gradle.api.publish.PublishingExtension>()
fun signing(configure: org.gradle.plugins.signing.SigningExtension.() -> Unit) = extensions.configure(configure)
val rachelCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

'''
    for module, project_dir, descriptor in [
        ("org.jetbrains.skiko_skiko", root / "skiko", "rootProject"),
        ("org.jetbrains.skiko_skiko-skottie", root / "skiko/skiko-skottie", 'project(":skiko-skottie")'),
    ]:
        build_file = project_dir / "build.gradle.kts"
        text = remove_block(build_file.read_text(encoding="utf-8"), "plugins {")
        for alias in sorted(libraries, key=len, reverse=True):
            accessor = "libs." + re.sub(r"[-_]", ".", alias)
            text = re.sub(re.escape(accessor) + r"\b", f'rachelCatalog.findLibrary("{alias}").get()', text)
        text = replace_once(text, "val skiko =", accessors + "val skiko =")
        (project_dir / "rachel-upstream.gradle.kts").write_text(text, encoding="utf-8", newline="\n")
        relative = os.path.relpath(own_root / "adaptation" / module / "build.gradle.kts", project_dir).replace("\\", "/")
        with settings.open("a", encoding="utf-8", newline="\n") as output:
            output.write(f'\n{descriptor}.buildFileName = "{relative}"\n')
