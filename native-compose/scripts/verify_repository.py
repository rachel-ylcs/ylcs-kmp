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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=ROOT / "build/repository")
    parser.add_argument("--target", choices=["mingw_x64", "linux_x64", "macos_arm64"], required=True)
    args = parser.parse_args()
    lock = load_lock()
    version = lock["portVersion"]
    base = args.repository / lock["group"].replace(".", "/")
    suffix = {"mingw_x64": "mingwx64", "linux_x64": "linuxx64", "macos_arm64": "macosarm64"}[args.target]
    target_directory = base / f"skiko-{suffix}" / version
    root_directory = base / "skiko" / version
    root_metadata = json.loads((root_directory / f"skiko-{version}.module").read_text(encoding="utf-8"))
    target_metadata = json.loads((target_directory / f"skiko-{suffix}-{version}.module").read_text(encoding="utf-8"))
    for metadata, directory in [(root_metadata, root_directory), (target_metadata, target_directory)]:
        checked(metadata["component"]["group"] == lock["group"], "Wrong publication group")
        checked(metadata["component"]["version"] == version, "Wrong publication version")
        verify_files(metadata, directory)
    target_variants = [v for v in root_metadata["variants"] if v.get("attributes", {}).get("org.jetbrains.kotlin.native.target") == args.target]
    checked(target_variants, f"Root metadata does not advertise {args.target}")
    checked(any(v.get("available-at", {}).get("module") == f"skiko-{suffix}" for v in target_variants), "Root metadata does not point to the target module")
    main = target_directory / f"skiko-{suffix}-{version}.klib"
    with zipfile.ZipFile(main) as archive:
        binaries = [name for name in archive.namelist() if "/included/" in name and name.endswith((".lib", ".a"))]
        checked(any(Path(name).name in ["skia.lib", "libskia_static.a"] for name in binaries), "Skia static archive is not embedded")
        checked(any("native-bridges" in name for name in binaries), "C++ bridge archive is not embedded")
    for klib in target_directory.glob("*.klib"):
        with zipfile.ZipFile(klib) as archive:
            for name in archive.namelist():
                if name.endswith("/manifest"):
                    manifest = archive.read(name).decode("utf-8")
                    checked(f"compiler_version={lock['versions']['kotlin']}" in manifest, f"Unexpected compiler: {klib}")
                    checked(f"native_targets={args.target}" in manifest, f"Wrong KLIB target: {klib}")
                    for line in manifest.splitlines():
                        if line.startswith("linkerOpts="):
                            checked(not re.search(r"[A-Za-z]:[\\/]|/(?:Users|home|workspace|tmp|private|Volumes)/", line), f"Build-machine linker path leaked into {klib}")
    sources = target_directory / f"skiko-{suffix}-{version}-sources.jar"
    with zipfile.ZipFile(sources) as archive:
        names = archive.namelist()
        checked("META-INF/licenses/Apache-2.0.txt" in names, "Source artifact is missing the Skiko license")
        checked("META-INF/licenses/THIRD_PARTY_NOTICES.md" in names, "Source artifact is missing provenance")
        checked(any(n.startswith("native-sources/") and n.endswith((".cc", ".cpp")) for n in names), "Source artifact is missing compiled C++ sources")
    if args.target == "mingw_x64":
        data = target_directory / f"skiko-{suffix}-{version}-icudtl.dat"
        checked(data.is_file() and data.stat().st_size > 0, "Windows ICU data artifact is missing")
    print(json.dumps({"result": "PASS", "target": args.target, "version": version, "embedded_archives": len(binaries), "repository": str(args.repository.resolve())}, indent=2))


if __name__ == "__main__":
    main()
