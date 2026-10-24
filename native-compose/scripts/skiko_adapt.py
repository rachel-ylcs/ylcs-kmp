"""Rachel's small, reviewable delta over the locked Skiko three-way source merge."""
from pathlib import Path
import re
import shutil
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


def adapt(root: Path, lock):
    notices = root / "skiko/rachel-notices"
    notices.mkdir()
    own_root = Path(__file__).resolve().parents[1]
    shutil.copyfile(own_root / "licenses/THIRD_PARTY_NOTICES.md", notices / "THIRD_PARTY_NOTICES.md")
    shutil.copyfile(own_root / "upstream/adaptation-origins.json", notices / "adaptation-origins.json")
    for license_file in (own_root / "licenses").glob("*.txt"):
        shutil.copyfile(license_file, notices / license_file.name)
    # Existing dependency versions come exclusively from the parent version catalog.
    catalog = root / "dependencies.toml"
    align_catalog(catalog, main_profile(own_root.parent),
        {"kotlin": "kotlin", "coroutines": "coroutines", "androidGradlePlugin": "agp", "dokka": "dokka"})

    props = root / "skiko/gradle.properties"
    text = props.read_text(encoding="utf-8")
    kotlin_language_version = ".".join(lock["versions"]["kotlin"].split(".")[:2])
    changes = {
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

    # Compile maintained files in-place. Remove shadowed files only in this disposable
    # prepared copy; the hash-addressed official source checkout remains untouched.
    for module, project_dir, context_name in [
        ("org.jetbrains.skiko_skiko", root / "skiko", "skikoProjectContext"),
        ("org.jetbrains.skiko_skiko-skottie", root / "skiko/skiko-skottie", "skikoSkottieProjectContext"),
    ]:
        adaptation = own_root / "adaptation" / module
        for source in (adaptation / "src").rglob("*"):
            if source.is_file():
                (project_dir / "src" / source.relative_to(adaptation / "src")).unlink(missing_ok=True)
        build_file = project_dir / "build.gradle.kts"
        binding = f'\n    project.extensions.extraProperties["rachel.skiko.context"] = {context_name}\n'
        binding += f'    project.extensions.extraProperties["rachel.native.adaptation"] = "{adaptation.as_posix()}"\n'
        binding += f'    project.apply(from = "{(adaptation / "build.gradle.kts").as_posix()}")\n'
        # Register after Skia tasks/the hierarchy exist, before the publication target snapshot.
        anchor = "    skikoProjectContext.declareSkiaTasks()\n" if module.endswith("_skiko") else "    applyHierarchyTemplate(skikoSourceSetHierarchyTemplate)\n"
        text = replace_once(build_file.read_text(encoding="utf-8"), anchor, anchor + binding)
        build_file.write_text(text, encoding="utf-8", newline="\n")

    # A missing native toolchain must not produce a source-only, un-linkable publication.
    build = root / "skiko/build.gradle.kts"
    text = build.read_text(encoding="utf-8")
    text += '''
// Rachel: sources artifacts include compiled native bridge sources and provenance.
tasks.withType<org.gradle.api.tasks.bundling.AbstractArchiveTask>().configureEach {
    if (name == "sourcesJar" || name.endsWith("SourcesJar")) {
        from(rootProject.file("rachel-notices")) { into("META-INF/licenses") }
        listOf("commonMain/cpp", "nativeJsMain/cpp", "nativeNativeJs/cpp", "windowsMain/cpp").forEach { path ->
            from("src/$path") { into("native-sources/$path") }
        }
    }
}

// Rachel: expose only complete target publications produced on the matching host.
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
