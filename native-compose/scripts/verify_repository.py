"""Audit a local Native Skiko publication before testing an independent consumer."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile
from sync_upstream import ROOT, load_lock


def checked(condition, message):
    if not condition:
        raise ValueError(message)


def verify_files(module, directory):
    for variant in module["variants"]:
        for entry in variant.get("files", []):
            path = directory / entry["url"]
            checked(path.is_file(), f"Missing publication artifact: {path}")
            checked(path.stat().st_size == entry["size"], f"Wrong artifact size: {path}")
            for algorithm in ["sha256", "sha512"]:
                if algorithm in entry:
                    with path.open("rb") as stream:
                        actual = hashlib.file_digest(stream, algorithm).hexdigest()
                    checked(actual == entry[algorithm], f"{algorithm} mismatch: {path}")


def verify_module(repository, target, lock, artifact):
    version = lock["versions"]["skiko"]
    base = repository / lock["group"].replace(".", "/")
    suffix = {"mingw_x64": "mingwx64", "linux_x64": "linuxx64", "macos_arm64": "macosarm64"}[target]
    target_directory = base / f"{artifact}-{suffix}" / version
    root_directory = base / artifact / version
    root_metadata = json.loads((root_directory / f"{artifact}-{version}.module").read_text(encoding="utf-8"))
    target_metadata = json.loads((target_directory / f"{artifact}-{suffix}-{version}.module").read_text(encoding="utf-8"))
    for metadata, directory in [(root_metadata, root_directory), (target_metadata, target_directory)]:
        checked(metadata["component"]["group"] == lock["group"], "Wrong publication group")
        checked(metadata["component"]["version"] == version, "Wrong publication version")
        verify_files(metadata, directory)
    target_variants = [v for v in root_metadata["variants"] if v.get("attributes", {}).get("org.jetbrains.kotlin.native.target") == target]
    checked(target_variants, f"Root metadata does not advertise {target}")
    checked(any(v.get("available-at", {}).get("module") == f"{artifact}-{suffix}" for v in target_variants), "Root metadata does not point to the target module")
    if artifact == "skiko-skottie":
        dependencies = [dependency for variant in target_metadata["variants"] for dependency in variant.get("dependencies", [])]
        checked(any(d.get("group") == lock["group"] and d.get("module") in ["skiko", f"skiko-{suffix}"]
                    and d.get("version", {}).get("requires") == version for d in dependencies),
                "Skottie must expose its matching Core dependency")
    main = target_directory / f"{artifact}-{suffix}-{version}.klib"
    with zipfile.ZipFile(main) as archive:
        binaries = [name for name in archive.namelist() if "/included/" in name and name.endswith((".lib", ".a"))]
        expected = "skia.lib" if artifact == "skiko" else "skottie.lib"
        checked(any(Path(name).name in [expected, "libskia_static.a"] for name in binaries), f"{artifact} static archive is not embedded")
        checked(any("native-bridges" in name for name in binaries), "C++ bridge archive is not embedded")
    exported_linker_options = []
    sdl_archives = []
    for klib in target_directory.glob("*.klib"):
        with zipfile.ZipFile(klib) as archive:
            sdl_archives += [name for name in archive.namelist() if "/included/" in name and Path(name).name in ["SDL3-static.lib", "libSDL3.a"]]
            for name in archive.namelist():
                if name.endswith("/manifest"):
                    manifest = archive.read(name).decode("utf-8")
                    checked(f"compiler_version={lock['versions']['kotlin']}" in manifest, f"Unexpected compiler: {klib}")
                    checked(f"native_targets={target}" in manifest, f"Wrong KLIB target: {klib}")
                    for line in manifest.splitlines():
                        if line.startswith("linkerOpts="):
                            exported_linker_options.append(line)
                            if "cinterop-sdl" in klib.name:
                                # SDL pkg-config flags are normalized for direct ld.
                                # Windows Core separately uses intentional clang-driver CRT flags.
                                checked("-Wl," not in line and "-Xlinker" not in line,
                                        f"SDL compiler-driver flags must be converted to native linker arguments: {klib}")
                        if line.startswith(("linkerOpts=", "libraryPaths=")):
                            checked(not re.search(r"[A-Za-z]:[\\/]|/(?:Users|home|workspace|tmp|private|Volumes)/", line), f"Build-machine linker path leaked into {klib}")
    if artifact == "skiko":
        checked(sdl_archives, "Core publication must embed the SDL static implementation in its CInterop")
    if artifact == "skiko" and target == "linux_x64":
        options = " ".join(exported_linker_options)
        for library in ["fontconfig", "GL", "dl"]:
            checked(f"-l{library}" in options, f"Linux KLIB must export the {library} system link dependency")
    sources = target_directory / f"{artifact}-{suffix}-{version}-sources.jar"
    with zipfile.ZipFile(sources) as archive:
        names = archive.namelist()
        checked("META-INF/licenses/Apache-2.0.txt" in names, "Source artifact is missing the Skiko license")
        checked("META-INF/licenses/THIRD_PARTY_NOTICES.md" in names, "Source artifact is missing provenance")
        if artifact == "skiko":
            checked("META-INF/licenses/SDL.txt" in names, "Static SDL publication is missing its license")
        checked(any(n.startswith("native-sources/") and n.endswith((".cc", ".cpp")) for n in names), "Source artifact is missing compiled C++ sources")
    if target == "mingw_x64" and artifact == "skiko":
        data = target_directory / f"{artifact}-{suffix}-{version}-icudtl.dat"
        checked(data.is_file() and data.stat().st_size > 0, "Windows ICU data artifact is missing")
    return {"module": artifact, "embedded_archives": len(binaries)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=ROOT / "vendor/build/native-compose/repository")
    parser.add_argument("--target", choices=["mingw_x64", "linux_x64", "macos_arm64"], required=True)
    args = parser.parse_args()
    lock = load_lock()
    modules = [verify_module(args.repository, args.target, lock, artifact) for artifact in ["skiko", "skiko-skottie"]]
    print(json.dumps({"result": "PASS", "target": args.target, "version": lock["versions"]["skiko"],
                      "modules": modules, "repository": str(args.repository.resolve())}, indent=2))


if __name__ == "__main__":
    main()
