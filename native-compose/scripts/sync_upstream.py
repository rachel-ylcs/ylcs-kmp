"""Fetch immutable sources and reproduce the Native fork. Python 3.11+, Git required."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path, PurePosixPath
import platform
import re
import shutil
import subprocess
import sys
import tomllib
import urllib.request
import zipfile
from upstream_versions import main_profile, apply_build_profile

ROOT = Path(__file__).resolve().parents[1]
VENDOR = ROOT / "vendor"
SKIKO_VENDOR = VENDOR / "skiko"


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def load_lock():
    lock = json.loads((ROOT / "upstream/lock.json").read_text(encoding="utf-8"))
    catalog = tomllib.loads((ROOT.parent / "libs.versions.toml").read_text(encoding="utf-8"))
    lock["upstreamVersions"] = dict(lock["versions"])
    # Source identity is not a dependency override: never relabel old Skiko source as a new release.
    if catalog["versions"]["skiko"] != lock["upstreamVersions"]["skiko"]:
        raise ValueError(f"Main catalog selects Skiko {catalog['versions']['skiko']}; update its pinned source/Skia inputs in upstream/lock.json. The main project does not need a downgrade.")
    lock["versions"] = {key: catalog["versions"][key] for key in lock["versions"]}
    native_skiko = catalog["libraries"]["skikoNative"]
    if native_skiko["version"]["ref"] != "skiko" or native_skiko["module"] != f"{lock['group']}:skiko":
        raise ValueError("Native Skiko coordinates changed in libs.versions.toml; review upstream/lock.json first.")
    return lock


def unpack(archive, destination):
    with zipfile.ZipFile(archive) as source:
        for item in source.infolist():
            parts = PurePosixPath(item.filename).parts
            if not parts or len(parts) == 1:
                continue
            if any(p in ("..", ".") or ":" in p or "\\" in p for p in parts):
                raise ValueError(f"Unsafe archive entry: {item.filename}")
            target = destination.joinpath(*parts[1:])
            if not target.resolve().is_relative_to(destination.resolve()):
                raise ValueError(f"Archive entry escapes destination: {item.filename}")
            if (item.external_attr >> 16) & 0o170000 == 0o120000:
                raise ValueError(f"Symlink in source archive: {item.filename}")
            if item.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with source.open(item) as src, target.open("wb") as dst:
                    shutil.copyfileobj(src, dst)


def fetch(name, entry, offline):
    if not re.fullmatch(r"[a-z0-9-]+", name) or not re.fullmatch(r"[a-f0-9]{40}", entry["commit"]):
        raise ValueError("Invalid source lock")
    archive = SKIKO_VENDOR / f"{name}.zip"
    if not archive.exists():
        if offline:
            raise ValueError(f"Offline archive missing: {archive}")
        url = f'https://codeload.github.com/{entry["repository"]}/zip/{entry["commit"]}'
        print(f"Downloading {name} at {entry['commit']}", flush=True)
        with urllib.request.urlopen(url, timeout=60) as response, archive.with_suffix(".part").open("wb") as target:
            shutil.copyfileobj(response, target)
        archive.with_suffix(".part").replace(archive)
    if digest(archive) != entry["sha256"]:
        raise ValueError(f"SHA-256 mismatch: {archive}; preserve it for investigation and remove it manually before retrying")
    # Use hash-addressed raw sources; do not trust or overwrite an existing developer checkout.
    destination = SKIKO_VENDOR / "sources" / entry["sha256"]
    marker = destination / ".rachel-extracted"
    if not marker.exists():
        unpack(archive, destination)
        marker.write_text(entry["sha256"], encoding="utf-8")
    return destination


def host_skia_key():
    system = platform.system()
    machine = platform.machine().lower()
    if system in ("Windows", "Linux") and machine in ("amd64", "x86_64"):
        return f"{system.lower()}-Release-x64"
    if system == "Darwin" and machine in ("arm64", "aarch64"):
        return "macos-Release-arm64"
    raise ValueError(f"Unsupported Core build host: {system}/{machine}; use Windows x64, Linux x64 or macOS ARM64")


def prepare_skia(lock, offline):
    key = host_skia_key()
    filename = f"Skia-{lock['skia']}-{key}.zip"
    expected = lock["skiaAssets"][key]
    cached = SKIKO_VENDOR / "skia" / f"{expected}.zip"
    if not cached.is_file():
        cached.parent.mkdir(parents=True, exist_ok=True)
        temporary = cached.with_suffix(".part")
        previous = next((path for path in sorted((SKIKO_VENDOR / "prepared").glob(f"*/skiko/dependencies/skia/{lock['skia']}/{filename}"))
                         if path.is_file() and path.stat().st_size and digest(path) == expected), None)
        if previous:
            shutil.copyfile(previous, temporary)
        elif offline:
            raise ValueError(f"Offline Skia archive missing: {cached}; run sync_upstream.py online once")
        else:
            print(f"Downloading audited Skia archive {filename}", flush=True)
            url = f"https://github.com/JetBrains/skia/releases/download/{lock['skia']}/{filename}"
            with urllib.request.urlopen(url, timeout=60) as source, temporary.open("wb") as target:
                shutil.copyfileobj(source, target)
        if digest(temporary) != expected:
            raise ValueError(f"Skia SHA-256 mismatch: {temporary}; preserved for investigation")
        temporary.replace(cached)
    if digest(cached) != expected:
        raise ValueError(f"Skia SHA-256 mismatch: {cached}; preserved for investigation")
    return filename, cached


def prepare(lock, sources, skia_archive):
    spec = importlib.util.spec_from_file_location("skiko_adapt", ROOT / "scripts/skiko_adapt.py")
    adapter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(adapter)
    files = json.loads((ROOT / "upstream/skiko-native-files.json").read_text(encoding="utf-8"))
    profile = main_profile(ROOT.parent)
    fingerprint = hashlib.sha256()
    inputs = [ROOT / "upstream/lock.json", ROOT / "upstream/skiko-native-files.json", ROOT / "upstream/adaptation-origins.json", ROOT / "scripts/skiko_adapt.py", ROOT / "scripts/upstream_versions.py", Path(__file__), ROOT / "licenses/THIRD_PARTY_NOTICES.md"]
    inputs += sorted((ROOT / "licenses").glob("*.txt"), key=lambda path: path.relative_to(ROOT).as_posix())
    inputs += [ROOT / "scripts/prepare_sdl.py", ROOT / "upstream/sdl.json"]
    inputs += [ROOT.parent / name for name in ["gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat"]]
    for module in (ROOT / "adaptation").iterdir():
        if module.is_dir() and module.name != "samples":
            inputs += [path for path in module.rglob("*") if path.is_file()]
    input_hashes = {}
    for path in sorted(inputs, key=lambda p: p.relative_to(ROOT.parent).as_posix()):
        relative = path.relative_to(ROOT.parent).as_posix()
        input_hashes[relative] = digest(path)
        fingerprint.update(relative.encode("utf-8") + b"\0" + path.read_bytes())
    effective = {"versions": lock["versions"], "libraries": profile["libraries"], "java": profile["java"], "gradleProperties": profile["gradleProperties"]}
    fingerprint.update(json.dumps(effective, sort_keys=True).encode("utf-8"))
    destination = SKIKO_VENDOR / "prepared" / fingerprint.hexdigest()[:16]
    marker = destination / ".rachel-prepared.json"
    if not marker.exists():
        if destination.exists():
            raise ValueError(f"Incomplete preparation preserved at {destination}; inspect it and remove manually before retrying")
        shutil.copytree(sources["skiko-upstream"], destination)
        for name in files:
            if not (destination / name).resolve().is_relative_to(destination.resolve()):
                raise ValueError(f"Unsafe manifest path: {name}")
            upstream = sources["skiko-upstream"] / name
            base = sources["skiko-merge-base"] / name
            port = sources["skiko-reference"] / name
            output = destination / name
            if not port.exists():
                if upstream.exists() and base.read_bytes() != upstream.read_bytes():
                    raise ValueError(f"Upstream changed a file removed by the port: {name}")
                output.unlink(missing_ok=True)
            elif not base.exists():
                if upstream.exists() and upstream.read_bytes() != port.read_bytes():
                    raise ValueError(f"Both upstreams added {name}")
                output.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(port, output)
            else:
                result = subprocess.run(["git", "merge-file", "-p", "-L", "UPSTREAM", "-L", "BASE", "-L", "PORT", str(upstream), str(base), str(port)], capture_output=True)
                text = result.stdout.decode("utf-8").replace("\r\n", "\n")
                if result.returncode < 0 or result.returncode > 127:
                    raise ValueError(result.stderr.decode("utf-8", errors="replace"))
                if result.returncode:
                    text = adapter.resolve_merge(name, text)
                output.write_text(text, encoding="utf-8", newline="\n")
        adapter.adapt(destination, lock)
        apply_build_profile(destination, ROOT.parent, profile)
        marker.write_text(json.dumps({"fingerprint": fingerprint.hexdigest(), "sources": lock["sources"], "versions": lock["versions"]}, indent=2) + "\n", encoding="utf-8")
    if skia_archive:
        filename, cached = skia_archive
        archive = destination / "skiko/dependencies/skia" / lock["skia"] / filename
        if not archive.is_file() or digest(archive) != digest(cached):
            archive.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(cached, archive)
    state = {"build": str((destination / "skiko").relative_to(ROOT)).replace("\\", "/"), "fingerprint": fingerprint.hexdigest(),
             "inputs": input_hashes, "versions": lock["versions"], "catalogSha256": profile["catalogSha256"], "java": profile["java"]}
    (SKIKO_VENDOR / "state.json").write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
    print(f"Prepared Skiko {lock['versions']['skiko']} -> {state['build']}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--check-updates", action="store_true", help="Report upstream refs without changing the lock or prepared sources")
    args = parser.parse_args()
    lock = load_lock()
    if args.check_updates:
        for name, entry in lock["sources"].items():
            result = subprocess.run(["git", "ls-remote", f'https://github.com/{entry["repository"]}.git', entry["ref"]], check=True, capture_output=True, text=True)
            print(f"{name}: locked={entry['commit']} remote={result.stdout.strip() or 'immutable commit'}")
        return
    SKIKO_VENDOR.mkdir(parents=True, exist_ok=True)
    sources = {name: fetch(name, entry, args.offline) for name, entry in lock["sources"].items()}
    prepare(lock, sources, prepare_skia(lock, args.offline))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
