"""Fetch immutable sources and reproduce the Native fork. Python 3.11+, Git required."""
import argparse
from contextlib import contextmanager
import errno
import hashlib
import importlib.util
import json
from pathlib import Path, PurePosixPath
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import time
import tomllib
import urllib.request
import zipfile
from upstream_versions import main_profile, apply_build_profile

ROOT = Path(__file__).resolve().parents[1]
VENDOR = ROOT / "vendor"
SKIKO_VENDOR = VENDOR / "skiko"
WORKSPACE = SKIKO_VENDOR / "workspace"


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
    native_skiko = catalog["libraries"]["skiko-native"]
    if native_skiko["version"]["ref"] != "skiko" or native_skiko["module"] != f"{lock['group']}:skiko":
        raise ValueError("Native Skiko coordinates changed in libs.versions.toml; review upstream/lock.json first.")
    return lock


def unpack(archive, destination, members=None):
    with zipfile.ZipFile(archive) as source:
        for item in source.infolist():
            parts = PurePosixPath(item.filename).parts
            if not parts or len(parts) == 1:
                continue
            if any(p in ("..", ".") or ":" in p or "\\" in p for p in parts):
                raise ValueError(f"Unsafe archive entry: {item.filename}")
            if members is not None and PurePosixPath(*parts[1:]).as_posix() not in members:
                continue
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
    # Cache archives, not another full source checkout for every merge input.
    return archive


def checked_vendor_path(path):
    vendor = SKIKO_VENDOR.resolve()
    resolved = path.resolve()
    if not vendor.is_relative_to(ROOT.resolve()) or not resolved.is_relative_to(vendor) or resolved == vendor:
        raise ValueError(f"Unsafe generated source path: {path}")
    return resolved


def write_json(path, value):
    """Settings readers must see either the previous or the complete new state."""
    checked_vendor_path(path)
    temporary = path.with_name(path.name + ".tmp")
    checked_vendor_path(temporary)
    temporary.write_text(json.dumps(value, indent=4) + "\n", encoding="utf-8")
    temporary.replace(path)


@contextmanager
def preparation_lock():
    """Serialize preparation of the one working tree across Gradle/IDE imports."""
    with (SKIKO_VENDOR / ".prepare.lock").open("a+b") as stream:
        if stream.tell() == 0:
            stream.write(b"\0")
            stream.flush()
        stream.seek(0)
        if sys.platform == "win32":
            import msvcrt
            while True:
                try:
                    msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
                    break
                except OSError as error:
                    if error.errno not in (errno.EACCES, errno.EAGAIN, errno.EDEADLK):
                        raise
                    time.sleep(0.1)
        else:
            import fcntl
            fcntl.flock(stream.fileno(), fcntl.LOCK_EX)
        try:
            yield
        finally:
            stream.seek(0)
            if sys.platform == "win32":
                msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(stream.fileno(), fcntl.LOCK_UN)


def sync_workspace(staging, previous):
    """Update generated files in place, retaining build outputs and unchanged mtimes."""
    checked_vendor_path(WORKSPACE)
    WORKSPACE.mkdir(exist_ok=True)
    files = {path.relative_to(staging).as_posix(): path for path in staging.rglob("*") if path.is_file()}
    for relative in previous.get("files", []):
        if relative not in files:
            path = WORKSPACE / relative
            checked_vendor_path(path)
            if not path.resolve().is_relative_to(WORKSPACE.resolve()):
                raise ValueError(f"Generated file escapes the workspace: {relative}")
            path.unlink(missing_ok=True)
    for relative, source in files.items():
        destination = WORKSPACE / relative
        checked_vendor_path(destination)
        if not destination.resolve().is_relative_to(WORKSPACE.resolve()):
            raise ValueError(f"Generated file escapes the workspace: {relative}")
        if destination.is_dir():
            destination.rmdir()
        if not destination.is_file() or destination.read_bytes() != source.read_bytes():
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    return sorted(files)


def cleanup_legacy_sources():
    """Remove only recognized old generated copies, after the stable tree is ready."""
    for parent, pattern, marker in [(SKIKO_VENDOR / "prepared", r"[a-f0-9]{16}", ".rachel-prepared.json"),
                                     (SKIKO_VENDOR / "sources", r"[a-f0-9]{64}", ".rachel-extracted")]:
        if not parent.is_dir():
            continue
        checked_vendor_path(parent)
        for directory in parent.iterdir():
            if directory.is_dir() and re.fullmatch(pattern, directory.name) and (directory / marker).is_file():
                checked_vendor_path(directory)
                if (directory / ".git").exists():
                    raise ValueError(f"Git checkout preserved in legacy source directory: {directory}")
                original_marker = (directory / marker).read_bytes()
                try:
                    shutil.rmtree(directory)
                except PermissionError:
                    # A previous IDE import can still hold a generated buildSrc JAR.
                    # Keep ownership recognizable so the next preparation can retry.
                    if directory.is_dir():
                        (directory / marker).write_bytes(original_marker)
                    print(f"Legacy generated files still in use; cleanup deferred: {directory}", flush=True)
        if not any(parent.iterdir()):
            parent.rmdir()


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
        candidates = [WORKSPACE / "skiko/dependencies/skia" / lock["skia"] / filename]
        candidates += sorted((SKIKO_VENDOR / "prepared").glob(f"*/skiko/dependencies/skia/{lock['skia']}/{filename}"))
        previous = next((path for path in candidates
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
    inputs = [ROOT / "upstream/lock.json", ROOT / "upstream/skiko-native-files.json", ROOT / "upstream/adaptation-origins.json", ROOT / "scripts/skiko_adapt.py", ROOT / "scripts/upstream_versions.py", Path(__file__)]
    inputs += [ROOT / "scripts/prepare_sdl.py", ROOT / "upstream/sdl.json"]
    inputs += [ROOT.parent / name for name in ["gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat"]]
    # Maintained Kotlin/C++ and build scripts are compiled directly by Gradle.
    # Only changes to their paths require preparing shadow removal again.
    source_inventory = sorted(path.relative_to(ROOT).as_posix()
                              for module in (ROOT / "adaptation").iterdir() if module.is_dir() and module.name != "samples"
                              for path in (module / "src").rglob("*") if path.is_file())
    input_hashes = {}
    for path in sorted(inputs, key=lambda p: p.relative_to(ROOT.parent).as_posix()):
        relative = path.relative_to(ROOT.parent).as_posix()
        input_hashes[relative] = digest(path)
        fingerprint.update(relative.encode("utf-8") + b"\0" + path.read_bytes())
    effective = {"versions": lock["versions"], "libraries": profile["libraries"], "java": profile["java"],
                 "gradleProperties": profile["gradleProperties"], "sourceInventory": source_inventory}
    fingerprint.update(json.dumps(effective, sort_keys=True).encode("utf-8"))
    checked_vendor_path(WORKSPACE)
    marker = WORKSPACE / ".rachel-prepared.json"
    previous = json.loads(marker.read_text(encoding="utf-8")) if marker.is_file() else {}
    if WORKSPACE.exists() and not previous:
        raise ValueError(f"Unrecognized working tree preserved at {WORKSPACE}; source preparation will not overwrite it")
    if previous.get("fingerprint") != fingerprint.hexdigest():
        # Sibling staging paths have the same depth as workspace, so generated
        # relative adaptation/build-script paths remain valid after synchronization.
        with tempfile.TemporaryDirectory(prefix=".workspace-", dir=SKIKO_VENDOR) as staging_directory, \
                tempfile.TemporaryDirectory(prefix=".merge-inputs-", dir=SKIKO_VENDOR) as merge_directory:
            staging = Path(staging_directory)
            merge_inputs = Path(merge_directory)
            checked_vendor_path(staging)
            checked_vendor_path(merge_inputs)
            unpack(sources["skiko-upstream"], staging)
            for name in ["skiko-merge-base", "skiko-reference"]:
                unpack(sources[name], merge_inputs / name, set(files))
            for name in files:
                upstream = staging / name
                if not upstream.resolve().is_relative_to(staging.resolve()):
                    raise ValueError(f"Unsafe manifest path: {name}")
                base = merge_inputs / "skiko-merge-base" / name
                port = merge_inputs / "skiko-reference" / name
                if not port.exists():
                    if upstream.exists() and (not base.is_file() or base.read_bytes() != upstream.read_bytes()):
                        raise ValueError(f"Upstream changed a file removed by the port: {name}")
                    upstream.unlink(missing_ok=True)
                elif not base.exists():
                    if upstream.exists() and upstream.read_bytes() != port.read_bytes():
                        raise ValueError(f"Both upstreams added {name}")
                    upstream.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(port, upstream)
                else:
                    result = subprocess.run(["git", "merge-file", "-p", "-L", "UPSTREAM", "-L", "BASE", "-L", "PORT", str(upstream), str(base), str(port)], capture_output=True)
                    text = result.stdout.decode("utf-8").replace("\r\n", "\n")
                    if result.returncode < 0 or result.returncode > 127:
                        raise ValueError(result.stderr.decode("utf-8", errors="replace"))
                    if result.returncode:
                        text = adapter.resolve_merge(name, text)
                    upstream.write_text(text, encoding="utf-8", newline="\n")
            adapter.adapt(staging, lock)
            apply_build_profile(staging, ROOT.parent, profile)
            generated_files = sync_workspace(staging, previous)
        write_json(marker, {"schema": 2, "fingerprint": fingerprint.hexdigest(), "files": generated_files,
                            "sources": lock["sources"], "versions": lock["versions"]})
    if skia_archive:
        filename, cached = skia_archive
        archive = WORKSPACE / "skiko/dependencies/skia" / lock["skia"] / filename
        if not archive.is_file() or digest(archive) != digest(cached):
            archive.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(cached, archive)
    state = {"schema": 2, "build": "vendor/skiko/workspace/skiko", "fingerprint": fingerprint.hexdigest(),
             "inputs": input_hashes, "sourceInventory": source_inventory, "versions": lock["versions"],
             "catalogSha256": profile["catalogSha256"], "java": profile["java"]}
    write_json(SKIKO_VENDOR / "state.json", state)
    cleanup_legacy_sources()
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
    with preparation_lock():
        sources = {name: fetch(name, entry, args.offline) for name, entry in lock["sources"].items()}
        prepare(lock, sources, prepare_skia(lock, args.offline))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
