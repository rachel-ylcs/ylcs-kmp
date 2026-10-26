"""Reproduce the exact Navigation3 release sources from its hash-locked official Maven source JAR."""
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VENDOR = ROOT / "vendor/navigation3"
WORKSPACE = VENDOR / "workspace"


def checked(path):
    if path.is_symlink() or not (ROOT / "vendor").resolve().is_relative_to(ROOT.resolve()) or not VENDOR.resolve().is_relative_to((ROOT / "vendor").resolve()) or not path.resolve().is_relative_to(VENDOR.resolve()):
        raise ValueError(f"Navigation3 generated path escapes vendor: {path}")
    return path


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None


def write(path, data):
    checked(path)
    if path.is_file() and path.read_bytes() == data:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = checked(path.with_name(path.name + ".tmp"))
    temporary.write_bytes(data)
    temporary.replace(path)


def prepare_navigation3(module, offline):
    version = module["version"]
    archive = module["sourceArchive"]
    expected_url = f"https://repo.maven.apache.org/maven2/org/jetbrains/androidx/navigation3/navigation3-ui-macosarm64/{version}/navigation3-ui-macosarm64-{version}-sources.jar"
    if archive["url"] != expected_url or not re.fullmatch(r"\d+\.\d+\.\d+(?:-[A-Za-z0-9.]+)?", version) or not re.fullmatch(r"[a-f0-9]{64}", archive["sha256"]):
        raise ValueError("Review Navigation3's official source coordinates and SHA-256")
    cached = checked(VENDOR / f"navigation3-ui-{version}-sources.jar")
    if not cached.is_file():
        if offline:
            raise ValueError(f"Offline Navigation3 source archive is missing: {cached}")
        with urllib.request.urlopen(expected_url, timeout=60) as response:
            data = response.read(16 * 1024 * 1024 + 1)
        if len(data) > 16 * 1024 * 1024 or hashlib.sha256(data).hexdigest() != archive["sha256"]:
            raise ValueError("Navigation3 source archive size/hash mismatch; cache preserved")
        write(cached, data)
    if digest(cached) != archive["sha256"]:
        raise ValueError("Navigation3 cached source hash mismatch; cache preserved")
    files = {}
    license_name = "META-INF/org/jetbrains/androidx/navigation3/navigation3-ui/LICENSE.txt"
    with zipfile.ZipFile(cached) as source:
        for entry in source.infolist():
            parts = PurePosixPath(entry.filename).parts
            if ".." in parts or "\\" in entry.filename or PurePosixPath(entry.filename).is_absolute() or (entry.external_attr >> 16) & 0o170000 == 0o120000:
                raise ValueError("Unsafe Navigation3 source entry")
            if entry.filename.startswith("commonMain/") and entry.filename.endswith(".kt"):
                relative = "workspace/navigation3-ui/src/commonMain/kotlin/" + entry.filename.removeprefix("commonMain/")
            elif entry.filename == license_name:
                relative = "workspace/navigation3-ui/LICENSE.txt"
            else:
                continue
            if entry.file_size > 2 * 1024 * 1024 or relative in files:
                raise ValueError("Navigation3 source entry is too large or duplicated")
            files[relative] = source.read(entry)
    if len(files) < 2 or "workspace/navigation3-ui/LICENSE.txt" not in files:
        raise ValueError("Navigation3 common sources/license are missing")
    record = checked(VENDOR / "source-state.json")
    previous = json.loads(record.read_text(encoding="utf-8"))["files"] if record.is_file() else {}
    for relative, expected in previous.items():
        if not relative.startswith("workspace/navigation3-ui/") or ".." in PurePosixPath(relative).parts or "\\" in relative or digest(checked(VENDOR / relative)) not in {expected, None}:
            raise ValueError(f"Navigation3 local source edits preserved: {relative}")
    existing_sources = {path.relative_to(VENDOR).as_posix() for path in (WORKSPACE / "navigation3-ui/src").rglob("*.kt")}
    if existing_sources - files.keys() - previous.keys():
        raise ValueError("Unrecognized Navigation3 Kotlin sources preserved; move maintained sources to adaptation")
    for relative, data in files.items():
        target = checked(VENDOR / relative)
        if relative not in previous and target.exists() and target.read_bytes() != data:
            raise ValueError(f"Unrecognized Navigation3 source preserved: {relative}")
    # Record both pending writes and removals before changing the generated source tree.
    hashes = {path: hashlib.sha256(data).hexdigest() for path, data in files.items()}
    write(record, (json.dumps({"url": expected_url, "sha256": archive["sha256"], "files": {**previous, **hashes}}, indent=4) + "\n").encode())
    for relative in previous.keys() - files.keys():
        checked(VENDOR / relative).unlink(missing_ok=True)
    for relative, data in files.items():
        write(VENDOR / relative, data)
    write(record, (json.dumps({"url": expected_url, "sha256": archive["sha256"], "files": hashes}, indent=4) + "\n").encode())
