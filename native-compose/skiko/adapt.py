"""Rachel's small, reviewable delta over the locked Skiko three-way source merge."""
from pathlib import Path
import re
import shutil


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
    shutil.copyfile(own_root / "THIRD_PARTY_NOTICES.md", notices / "THIRD_PARTY_NOTICES.md")
    for license_file in (own_root / "licenses").glob("*.txt"):
        shutil.copyfile(license_file, notices / license_file.name)
    # Existing dependency versions come exclusively from the parent version catalog.
    catalog = root / "dependencies.toml"
    text = catalog.read_text(encoding="utf-8")
    for upstream, project in {"kotlin": "kotlin", "coroutines": "coroutines", "androidGradlePlugin": "agp", "dokka": "dokka"}.items():
        text, count = re.subn(rf'(?m)^{upstream} = "[^"]+"$', f'{upstream} = "{lock["versions"][project]}"', text)
        if count != 1:
            raise ValueError(f"Missing upstream version: {upstream}")
    catalog.write_text(text, encoding="utf-8", newline="\n")

    props = root / "skiko/gradle.properties"
    text = props.read_text(encoding="utf-8")
    kotlin_language_version = ".".join(lock["versions"]["kotlin"].split(".")[:2])
    changes = {
        "deploy.version": lock["portVersion"],
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
    for module in ["ci", "docs", "import-generator", "skiko-skottie", "skiko-graphite"]:
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

    # A missing native toolchain must not produce a source-only, un-linkable publication.
    build = root / "skiko/build.gradle.kts"
    text = build.read_text(encoding="utf-8")
    text += '''
// Rachel: sources artifacts include compiled native bridge sources and provenance.
tasks.withType<org.gradle.api.tasks.bundling.AbstractArchiveTask>().configureEach {
    if (name == "sourcesJar" || name.endsWith("SourcesJar")) {
        from("rachel-notices") { into("META-INF/licenses") }
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
