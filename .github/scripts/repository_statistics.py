#!/usr/bin/env python3
"""Generate repository size, architecture, asset, quality and history reports."""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tomllib
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
SCOPES = ("ylcs-app", "ylcs-module", "build-logic", "ylcs-docs", "native-compose/adaptation")
EXCLUDED_DIRS = {"vendor", "vendors", "upstream", "upstreams", "third_party", "third-party"}
TOOL_MODULES = {"build-tool", "lint", "dokka"}
PLACEHOLDERS = {".gitkeep", ".keep", ".placeholder", "placeholder", ".empty"}
LONG_FILE_THRESHOLD = 1000
TODO_RE = re.compile(r"\b(TODO|FIXME|XXX|HACK)\b")
SEMVER_RE = re.compile(r"^(?:v|m|release[-_]?|rel[-_]?)?(?P<major>\d+)\.(?P<minor>\d+)\.(?P<patch>\d+)(?P<suffix>[-+][0-9A-Za-z.-]+)?$", re.IGNORECASE)

LANGUAGE_EXTENSIONS = {
    "Kotlin": {".kt", ".kts"},
    "C++": {".c", ".cc", ".cpp", ".cxx", ".h", ".hh", ".hpp", ".hxx", ".def"},
    "Objective-C++": {".m", ".mm"},
    "Java": {".java"},
    "Swift": {".swift"},
    "Python": {".py"},
    "Rust": {".rs"},
    "CMake": {".cmake"},
    "Shell": {".sh", ".bash", ".zsh"},
    "Web": {".html", ".htm", ".js", ".mjs", ".cjs", ".css", ".scss", ".ts", ".tsx", ".jsx"},
    "Groovy": {".gradle"},
    "YAML": {".yml", ".yaml"},
    "JSON": {".json"},
    "TOML": {".toml"},
    "XML": {".xml", ".plist", ".storyboard", ".xib"},
    "SQL": {".sql"},
    "Markdown": {".md", ".mdx"},
    "Config": {".properties", ".conf", ".ini", ".cfg", ".pro", ".proguard"},
}
COMMENT_RULES = {
    "Kotlin": (("//",), ("/*", "*/")), "C++": (("//", ";"), ("/*", "*/")),
    "Objective-C++": (("//",), ("/*", "*/")), "Java": (("//",), ("/*", "*/")),
    "Swift": (("//",), ("/*", "*/")), "Rust": (("//",), ("/*", "*/")),
    "Web": (("//",), ("/*", "*/")), "Groovy": (("//",), ("/*", "*/")),
    "Python": (("#",), None), "Shell": (("#",), None), "YAML": (("#",), None),
    "CMake": (("#",), None), "TOML": (("#",), None), "Config": (("#", ";"), None),
    "SQL": (("--",), ("/*", "*/")), "XML": ((), ("<!--", "-->")),
    "Markdown": ((), ("<!--", "-->")), "JSON": ((), None),
}
IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".ico", ".icns", ".svg", ".avif"}
FONT_EXTENSIONS = {".ttf", ".otf", ".woff", ".woff2"}
AUDIO_EXTENSIONS = {".mp3", ".wav", ".ogg", ".flac", ".m4a", ".aac"}
VIDEO_EXTENSIONS = {".mp4", ".webm", ".mov", ".mkv"}
BINARY_EXTENSIONS = {".dll", ".so", ".dylib", ".a", ".lib", ".jar", ".aar", ".klib", ".bin", ".exe", ".framework", ".class", ".wasm", ".zip", ".7z", ".gz", ".pdf"}


def git(*args: str, check: bool = False) -> str:
    result = subprocess.run(["git", *args], cwd=ROOT, text=True, encoding="utf-8", errors="replace", stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
    if check and result.returncode:
        raise RuntimeError(f"git {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout.strip() if result.returncode == 0 else ""


def parse_submodules() -> list[dict]:
    path = ROOT / ".gitmodules"
    if not path.is_file():
        return []
    entries, current = [], {}
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        section = re.match(r'\s*\[submodule\s+"([^"]+)"\]', line)
        if section:
            if current:
                entries.append(current)
            current = {"name": section.group(1)}
            continue
        item = re.match(r"\s*(path|url)\s*=\s*(.*?)\s*$", line)
        if item:
            current[item.group(1)] = item.group(2)
    if current:
        entries.append(current)
    return entries


def excluded_path(relative: str, submodule_paths: list[str]) -> bool:
    if any(part.lower() in EXCLUDED_DIRS for part in Path(relative).parts):
        return True
    return any(relative == item or relative.startswith(item + "/") for item in submodule_paths)


def collect_files(submodules: list[dict]) -> list[Path]:
    tracked = git("ls-files", "-co", "--exclude-standard", "-z")
    if tracked:
        candidates = [ROOT / name for name in tracked.split("\0") if name and any(name == scope or name.startswith(scope + "/") for scope in SCOPES)]
    else:
        candidates = []
        for scope in SCOPES:
            base = ROOT / scope
            if base.exists():
                for directory, dirs, names in os.walk(base):
                    dirs[:] = [name for name in dirs if name != ".git" and name.lower() not in EXCLUDED_DIRS]
                    candidates.extend(Path(directory) / name for name in names)
    submodule_paths = [item.get("path", "").strip("/") for item in submodules if item.get("path")]
    candidates = [path for path in candidates if path.is_file() and not path.is_symlink() and not excluded_path(path.relative_to(ROOT).as_posix(), submodule_paths)]
    if candidates:
        names = [path.relative_to(ROOT).as_posix() for path in candidates]
        payload = ("\0".join(names) + "\0").encode("utf-8", errors="surrogateescape")
        result = subprocess.run(["git", "check-ignore", "--no-index", "-z", "--stdin"], cwd=ROOT, input=payload, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, check=False)
        ignored = {value.decode("utf-8", errors="surrogateescape") for value in result.stdout.split(b"\0") if value}
        candidates = [path for path in candidates if path.relative_to(ROOT).as_posix() not in ignored]
    return sorted(set(candidates), key=lambda path: path.relative_to(ROOT).as_posix().lower())


def language_for(path: Path, is_text: bool = True) -> str | None:
    if path.name.lower() == "cmakelists.txt":
        return "CMake"
    for language, extensions in LANGUAGE_EXTENSIONS.items():
        if path.suffix.lower() in extensions:
            return language
    return "Other text" if is_text else None


def is_text_file(path: Path) -> bool:
    try:
        return b"\0" not in path.read_bytes()[:8192]
    except OSError:
        return False


def count_lines(content: str, language: str, suffix: str) -> dict:
    markers, block = COMMENT_RULES.get(language, ((), None))
    if suffix == ".def":
        markers, block = (";",), None
    block_start, block_end = block if block else (None, None)
    in_block, multiline_quote = False, None
    result = {"code": 0, "comment": 0, "blank": 0, "total": 0}
    for line in content.splitlines():
        has_code = has_comment = False
        index = 0
        while index < len(line):
            if in_block:
                has_comment = True
                end = line.find(block_end, index)
                if end < 0:
                    index = len(line)
                    break
                index = end + len(block_end)
                in_block = False
                continue
            if multiline_quote:
                has_code = True
                end = line.find(multiline_quote, index)
                if end < 0:
                    index = len(line)
                    break
                index = end + len(multiline_quote)
                multiline_quote = None
                continue
            marker = next((item for item in markers if line.startswith(item, index)), None)
            if marker:
                has_comment = True
                break
            if block_start and line.startswith(block_start, index):
                has_comment = True
                in_block = True
                index += len(block_start)
                continue
            char = line[index]
            if char in {"'", '"'}:
                has_code = True
                triple = char * 3
                if line.startswith(triple, index):
                    end = line.find(triple, index + 3)
                    if end < 0:
                        multiline_quote = triple
                        index = len(line)
                    else:
                        index = end + 3
                else:
                    index += 1
                    while index < len(line):
                        if line[index] == "\\":
                            index += 2
                        elif line[index] == char:
                            index += 1
                            break
                        else:
                            index += 1
                continue
            if not char.isspace():
                has_code = True
            index += 1
        if has_code:
            result["code"] += 1
        elif has_comment:
            result["comment"] += 1
        else:
            result["blank"] += 1
    result["total"] = result["code"] + result["comment"] + result["blank"]
    return result


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def module_directories(files: list[Path]) -> list[Path]:
    return sorted({path.parent for path in files if path.name in {"build.gradle.kts", "build.gradle"}}, key=lambda path: (len(path.parts), path.as_posix()), reverse=True)


def module_for(path: Path, modules: list[Path]) -> str:
    for module in modules:
        try:
            path.relative_to(module)
            return module.relative_to(ROOT).as_posix()
        except ValueError:
            pass
    return path.relative_to(ROOT).as_posix().split("/", 1)[0]


def aggregate(items: list[dict]) -> dict:
    line_counts, total_bytes = Counter(), 0
    for item in items:
        total_bytes += item["bytes"]
        line_counts.update(item["lines"])
    return {
        "files": len(items), "bytes": total_bytes,
        "lines": {key: line_counts[key] for key in ("code", "comment", "blank", "total")},
        "average_lines_per_file": round(line_counts["total"] / len(items), 1) if items else 0,
    }


def classify_asset(path: Path) -> str:
    suffix, parts = path.suffix.lower(), {part.lower() for part in path.parts}
    if suffix == ".conf" and path.parent.name.lower() == "config":
        return "Compose stability configuration"
    if suffix in {".pro", ".proguard"}:
        return "ProGuard rules"
    if "composeresources" in parts or "resources" in parts or "res" in parts:
        if suffix in IMAGE_EXTENSIONS | FONT_EXTENSIONS | AUDIO_EXTENSIONS | VIDEO_EXTENSIONS | {".xml", ".json", ".ttf", ".otf"}:
            return "Android / Compose resources"
    if suffix in IMAGE_EXTENSIONS:
        return "Images"
    if suffix in FONT_EXTENSIONS:
        return "Fonts"
    if suffix in AUDIO_EXTENSIONS:
        return "Audio"
    if suffix in VIDEO_EXTENSIONS:
        return "Video"
    if suffix in BINARY_EXTENSIONS:
        return "Binary / libraries"
    if suffix in {".xml", ".json", ".plist", ".pbxproj", ".xcscheme", ".xcconfig"}:
        return "Structured resources / configuration"
    if suffix in {".toml", ".yaml", ".yml", ".properties", ".ini", ".conf", ".cfg"}:
        return "Configuration"
    return "Other assets"


def parse_gradle_module(module: str, gradle_file: Path, files: list[dict]) -> dict:
    content = gradle_file.read_text(encoding="utf-8", errors="replace")
    kmp = bool(re.search(r"org\.jetbrains\.kotlin\.multiplatform|kotlinMultiplatform|kotlin\s*\(\s*['\"]multiplatform['\"]\s*\)|\bmultiplatform\s*\{", content))
    native_template = bool(re.search(r"\b(?:nativeLibrary|nativeExecutable)\s*\{", content))
    template = "multiplatform" if re.search(r"\bmultiplatform\s*\{", content) else ("native" if native_template else ("custom" if kmp else "non-KMP"))

    def assigned(name: str, default: bool) -> bool:
        match = re.search(r"\b" + re.escape(name) + r"\s*=\s*(true|false)\b", content, re.IGNORECASE)
        return match.group(1).lower() == "true" if match else default

    native_default = assigned("nativeDesktopTarget", False)
    android = bool(re.search(r"androidLibrary|androidApplication|com\.android\.kotlin\.multiplatform\.library", content))
    if template == "multiplatform":
        targets = {
            "android": android, "ios": assigned("iosTarget", True), "desktop": assigned("desktopTarget", True),
            "js": assigned("webTarget", True), "wasm": assigned("webTarget", True),
            "windows": assigned("windowsTarget", native_default), "linux": assigned("linuxTarget", native_default),
            "macos": assigned("macosTarget", native_default),
        }
    else:
        targets = {"android": android, "ios": False, "desktop": False, "js": False, "wasm": False, "windows": False, "linux": False, "macos": False}
        for platform, source_names in {
            "ios": ("iosMain", "iosArm64Main"), "desktop": ("desktopMain",), "js": ("jsMain",),
            "wasm": ("wasmJsMain",), "windows": ("windowsMain",), "linux": ("linuxMain",), "macos": ("macosMain",),
        }.items():
            targets[platform] = any(item["path"].startswith(module + "/src/" + name + "/") for item in files for name in source_names)
        if native_template:
            for platform in ("windows", "linux", "macos"):
                targets[platform] = assigned(platform + "Target", native_default) or targets[platform]

    source_sets = sorted({item["path"].split("/src/", 1)[1].split("/", 1)[0] for item in files if item["path"].startswith(module + "/src/")})
    test_sets = {
        "common": ("commonTest",), "android": ("androidUnitTest", "androidHostTest", "androidTest"),
        "ios": ("iosTest", "iosSimulatorArm64Test", "iosArm64Test"), "desktop": ("desktopTest",),
        "js": ("jsTest",), "wasm": ("wasmJsTest",), "windows": ("windowsTest",),
        "linux": ("linuxTest",), "macos": ("macosTest",),
    }
    tests = {
        platform: any(item["path"].startswith(module + "/src/" + name + "/") for item in files for name in names)
        for platform, names in test_sets.items()
    }
    relationships, pattern = [], re.compile(r"\b([A-Za-z][A-Za-z0-9]*(?:Main|Test)(?:List)?)\s*\(\s*([A-Za-z][A-Za-z0-9]*(?:Main|Test))\s*\)")
    for match in pattern.finditer(content):
        pair = {"source_set": match.group(1), "depends_on": match.group(2)}
        if pair not in relationships:
            relationships.append(pair)
    return {
        "path": module, "kmp": kmp,
        "native": any(targets[name] for name in ("windows", "linux", "macos")) or native_template,
        "template": template, "targets": targets, "tests": tests,
        "source_sets": source_sets, "source_set_relationships": relationships,
        "source_files": sum(1 for item in files if item["module"] == module and item["text"]),
    }


def parse_version_catalog() -> dict:
    path = ROOT / "libs.versions.toml"
    if not path.is_file():
        return {"present": False}
    try:
        catalog = tomllib.loads(path.read_text(encoding="utf-8"))
    except (OSError, tomllib.TOMLDecodeError) as error:
        return {"present": True, "error": str(error)}
    versions, plugins, libraries = catalog.get("versions", {}), catalog.get("plugins", {}), catalog.get("libraries", {})
    return {
        "present": True,
        "versions": [{"alias": key, "value": value} for key, value in sorted(versions.items())],
        "plugins": [{"alias": key, **value} for key, value in sorted(plugins.items())],
        "libraries": [{"alias": key, **value} for key, value in sorted(libraries.items())],
        "counts": {"versions": len(versions), "plugins": len(plugins), "libraries": len(libraries)},
    }


def parse_workflows() -> list[dict]:
    directory = ROOT / ".github" / "workflows"
    if not directory.is_dir():
        return []
    result = []
    for path in sorted([*directory.glob("*.yml"), *directory.glob("*.yaml")]):
        content = path.read_text(encoding="utf-8", errors="replace")
        name_match = re.search(r"(?m)^name:\s*(.*?)\s*$", content)
        triggers = [trigger for trigger in ("push", "pull_request", "pull_request_target", "workflow_dispatch", "workflow_call", "schedule", "release") if re.search(r"(?m)^(?:  )?" + re.escape(trigger) + r":", content)]
        jobs, lines, in_jobs, current = [], content.splitlines(), False, None
        for line in lines:
            if line.strip() == "jobs:":
                in_jobs = True
                continue
            if in_jobs and line and not line.startswith((" ", "\t")):
                break
            if not in_jobs:
                continue
            job_match = re.match(r"^  ([A-Za-z0-9_-]+):\s*$", line)
            if job_match:
                current = {"id": job_match.group(1), "runners": [], "actions": []}
                jobs.append(current)
                continue
            if current:
                runner = re.match(r"^\s+ runs-on:\s*(.+?)\s*$", line)
                action = re.match(r"^\s+ uses:\s*([^\s]+)", line)
                if runner:
                    current["runners"].append(runner.group(1))
                if action:
                    current["actions"].append(action.group(1))
        result.append({
            "file": path.relative_to(ROOT).as_posix(),
            "name": name_match.group(1).strip("'\"") if name_match else path.stem,
            "triggers": triggers, "jobs": jobs,
            "runners": sorted({runner for job in jobs for runner in job["runners"]}),
            "actions": sorted({action for job in jobs for action in job["actions"]}),
        })
    return result


def parse_semver(tag: str) -> tuple | None:
    match = SEMVER_RE.fullmatch(tag)
    if match is None:
        return None
    return (int(match.group("major")), int(match.group("minor")), int(match.group("patch")), (match.group("suffix") or "").lower())


def version_sort_key(version: tuple) -> tuple:
    major, minor, patch, suffix = version
    return (major, minor, patch, 1 if not suffix else 0, suffix)


def github_releases() -> dict:
    repository, token = os.environ.get("GITHUB_REPOSITORY", ""), os.environ.get("GITHUB_TOKEN", "")
    if not repository:
        return {}
    headers = {"Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    releases = {}
    for page in range(1, 6):
        request = urllib.request.Request(f"https://api.github.com/repos/{repository}/releases?per_page=100&page={page}", headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=10) as response:
                batch = json.loads(response.read().decode("utf-8"))
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
            break
        if not batch:
            break
        for release in batch:
            tag = release.get("tag_name")
            if tag:
                releases[tag] = {
                    "name": release.get("name") or tag, "published_at": release.get("published_at"),
                    "created_at": release.get("created_at"), "url": release.get("html_url"),
                    "draft": release.get("draft", False), "prerelease": release.get("prerelease", False),
                }
        if len(batch) < 100:
            break
    return releases


def git_history(catalog: dict) -> dict:
    releases, tags, grouped = github_releases(), git("tag", "--list").splitlines(), {}
    for tag in tags:
        version = parse_semver(tag)
        if version is None:
            continue
        commit = git("rev-list", "-n", "1", tag)
        if not commit:
            continue
        commit_date = git("show", "-s", "--format=%cI", commit)
        entry = {"tag": tag, "version": ".".join(map(str, version[:3])) + version[3], "sort": version, "commit": commit, "commit_date": commit_date, "release": releases.get(tag, {})}
        grouped.setdefault((version, commit), []).append(entry)
    versions = []
    for (version, commit), aliases in grouped.items():
        aliases.sort(key=lambda item: (bool(item["release"].get("published_at")), item["tag"].lower().startswith("v"), item["tag"].lower().startswith("m"), item["tag"]), reverse=True)
        selected = aliases[0]
        versions.append({
            "version": selected["version"], "tag": selected["tag"], "aliases": sorted(item["tag"] for item in aliases),
            "commit": commit, "commit_date": selected["commit_date"],
            "release_date": selected["release"].get("published_at") or selected["commit_date"],
            "release_url": selected["release"].get("url"), "unreleased": False, "_sort": version,
        })
    versions.sort(key=lambda item: version_sort_key(item["_sort"]))
    head, head_date = git("rev-parse", "HEAD"), ""
    if head:
        head_date = git("show", "-s", "--format=%cI", head)
    version_value = next((str(item["value"]) for item in catalog.get("versions", []) if item["alias"] == "rachel"), "unknown")
    if head:
        versions.append({
            "version": version_value, "tag": "main", "aliases": ["main"], "commit": head,
            "commit_date": head_date, "release_date": None, "release_url": None, "unreleased": True,
            "_sort": parse_semver(version_value) or (9999, 0, 0, ""),
        })

    comparisons = []
    for previous, current in zip(versions, versions[1:]):
        old, new = previous["commit"], current["commit"]
        if old == new:
            continue
        name_status = git("diff", "--name-status", "--find-renames", old, new, "--", *SCOPES).splitlines()
        added = deleted = modified = renamed = 0
        for line in name_status:
            status = line.split("\t", 1)[0]
            if status.startswith("A"):
                added += 1
            elif status.startswith("D"):
                deleted += 1
            elif status.startswith("R"):
                renamed += 1
            else:
                modified += 1
        numstat = git("diff", "--numstat", "--find-renames", old, new, "--", *SCOPES).splitlines()
        added_lines = deleted_lines = 0
        for line in numstat:
            fields = line.split("\t", 2)
            if len(fields) == 3 and fields[0].isdigit() and fields[1].isdigit():
                added_lines += int(fields[0])
                deleted_lines += int(fields[1])
        patch = git("diff", "--no-ext-diff", "--unified=0", old, new, "--", *SCOPES)
        added_code_lines = deleted_code_lines = 0
        current_language = None
        for patch_line in patch.splitlines():
            if patch_line.startswith("--- a/"):
                current_language = language_for(Path(patch_line[6:]), True)
            elif patch_line.startswith("+++ b/"):
                current_language = language_for(Path(patch_line[6:]), True)
            elif current_language and patch_line.startswith("+") and not patch_line.startswith("+++"):
                source_line = patch_line[1:].strip()
                markers = COMMENT_RULES.get(current_language, ((), None))[0]
                if source_line and not source_line.startswith(markers) and not source_line.startswith(("*", "*/", "<!--", "-->")):
                    added_code_lines += 1
            elif current_language and patch_line.startswith("-") and not patch_line.startswith("---"):
                source_line = patch_line[1:].strip()
                markers = COMMENT_RULES.get(current_language, ((), None))[0]
                if source_line and not source_line.startswith(markers) and not source_line.startswith(("*", "*/", "<!--", "-->")):
                    deleted_code_lines += 1

        commit_dates = git("log", "--format=%cI", f"{old}..{new}").splitlines()
        comparisons.append({
            "from": previous["version"] + (" Unreleased" if previous["unreleased"] else ""),
            "to": current["version"] + (" Unreleased" if current["unreleased"] else ""),
            "from_tag": previous["tag"], "to_tag": current["tag"],
            "added_files": added, "deleted_files": deleted, "modified_files": modified, "renamed_files": renamed,
            "changed_files": added + deleted + modified + renamed,
            "added_lines": added_lines, "deleted_lines": deleted_lines, "net_lines": added_lines - deleted_lines,
            "added_code_lines": added_code_lines, "deleted_code_lines": deleted_code_lines,
            "net_code_lines": added_code_lines - deleted_code_lines,
            "commit_count": len(commit_dates), "first_commit_date": min(commit_dates) if commit_dates else None,
            "last_commit_date": max(commit_dates) if commit_dates else None, "release_date": current["release_date"],
        })
    for item in versions:
        item.pop("_sort", None)
    return {"tag_count": len(tags), "semantic_tag_count": sum(len(items) for items in grouped.values()), "version_count": len(versions), "versions": versions, "comparisons": comparisons}


def collect_statistics() -> dict:
    submodules = parse_submodules()
    paths, gradle_modules = collect_files(submodules), None
    gradle_modules = module_directories(paths)
    file_rows = []
    language_rows, scope_rows, module_rows, asset_rows = defaultdict(list), defaultdict(list), defaultdict(list), defaultdict(list)
    todo_entries, empty_files, large_files = [], [], []
    duplicate_groups = defaultdict(list)
    text_files = 0

    for path in paths:
        relative = path.relative_to(ROOT).as_posix()
        try:
            size = path.stat().st_size
        except OSError:
            continue
        is_text = is_text_file(path)
        language = language_for(path, is_text)
        content = ""
        line_counts = {"code": 0, "comment": 0, "blank": 0, "total": 0}
        if is_text:
            text_files += 1
            content = path.read_text(encoding="utf-8", errors="replace")
            if language:
                line_counts = count_lines(content, language, path.suffix.lower())
            for line_number, line in enumerate(content.splitlines(), 1):
                for marker in TODO_RE.findall(line):
                    todo_entries.append({"marker": marker, "path": relative, "line": line_number, "text": line.strip()[:240]})
            if not content.strip():
                empty_files.append({"path": relative, "bytes": size, "kind": "whitespace-only" if size else "zero-byte"})
        elif size == 0:
            empty_files.append({"path": relative, "bytes": 0, "kind": "zero-byte"})

        module = module_for(path, gradle_modules)
        scope = next((name for name in SCOPES if relative == name or relative.startswith(name + "/")), "other")
        digest = sha256_file(path) if size else None
        row = {
            "path": relative, "scope": scope, "module": module, "language": language or "Binary",
            "extension": path.suffix.lower() or "[none]", "bytes": size, "lines": line_counts,
            "text": is_text, "sha256": digest,
        }
        file_rows.append(row)
        scope_rows[scope].append(row)
        module_rows[module].append(row)
        if language:
            language_rows[language].append(row)
            if line_counts["code"] > LONG_FILE_THRESHOLD:
                large_files.append({"path": relative, "code_lines": line_counts["code"], "total_lines": line_counts["total"]})
        asset_rows[classify_asset(path)].append(row)
        if digest:
            duplicate_groups[digest].append({"path": relative, "bytes": size})

    languages = [{"language": name, **aggregate(items)} for name, items in sorted(language_rows.items(), key=lambda item: sum(row["lines"]["code"] for row in item[1]), reverse=True)]
    scopes = [{"scope": name, **aggregate(items)} for name, items in sorted(scope_rows.items())]
    modules = [{"module": name, **aggregate(items)} for name, items in sorted(module_rows.items(), key=lambda item: sum(row["lines"]["code"] for row in item[1]), reverse=True)]
    assets = []
    for category, items in sorted(asset_rows.items(), key=lambda item: sum(row["bytes"] for row in item[1]), reverse=True):
        unique_sizes = {}
        for row in items:
            if row["sha256"]:
                unique_sizes[row["sha256"]] = max(unique_sizes.get(row["sha256"], 0), row["bytes"])
        assets.append({"category": category, "files": len(items), "unique_files": len(unique_sizes), "bytes": sum(row["bytes"] for row in items), "unique_bytes": sum(unique_sizes.values())})
    duplicates = [{"sha256": digest, "files": entries, "count": len(entries), "bytes": entries[0]["bytes"]} for digest, entries in duplicate_groups.items() if len(entries) > 1]
    duplicates.sort(key=lambda item: (item["count"], item["bytes"]), reverse=True)

    # Git does not store empty directories; this finds tracked placeholder-only directories.
    directory_files = defaultdict(list)
    for row in file_rows:
        parent = Path(row["path"]).parent
        while str(parent) not in {"", "."}:
            directory_files[parent.as_posix()].append(row)
            parent = parent.parent
    placeholder_dirs = [directory for directory, entries in directory_files.items() if entries and all(Path(item["path"]).name.lower() in PLACEHOLDERS or item["bytes"] == 0 for item in entries)]

    kmp_modules = []
    for module_dir in gradle_modules:
        relative_module = module_dir.relative_to(ROOT).as_posix()
        if not relative_module.startswith("ylcs-module/"):
            continue
        if relative_module.removeprefix("ylcs-module/").split("/", 1)[0] in TOOL_MODULES:
            continue
        gradle_file = next((module_dir / name for name in ("build.gradle.kts", "build.gradle") if (module_dir / name).is_file()), None)
        if gradle_file:
            kmp_modules.append(parse_gradle_module(relative_module, gradle_file, file_rows))
    kmp_modules.sort(key=lambda item: item["path"])
    target_names = ("android", "ios", "desktop", "js", "wasm", "windows", "linux", "macos")
    target_counts = {name: sum(1 for item in kmp_modules if item["targets"][name]) for name in target_names}
    test_counts = {name: sum(1 for item in kmp_modules if item["tests"][name]) for name in ("common", "android", "ios", "desktop", "js", "wasm", "windows", "linux", "macos")}

    catalog = parse_version_catalog()
    plugin_usage, dependency_usage, coordinates = Counter(), Counter(), Counter()
    for module_dir in gradle_modules:
        build_file = next((module_dir / name for name in ("build.gradle.kts", "build.gradle") if (module_dir / name).is_file()), None)
        if not build_file:
            continue
        content = build_file.read_text(encoding="utf-8", errors="replace")
        for alias in re.findall(r"libs\.plugins\.([A-Za-z0-9_.-]+)", content):
            plugin_usage[alias] += 1
        for alias in re.findall(r"libs\.([A-Za-z0-9_.-]+)", content):
            if not alias.startswith("plugins."):
                dependency_usage[alias] += 1
        for coordinate in re.findall(r"['\"]([A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[^'\"]+)['\"]", content):
            coordinates[coordinate] += 1

    workflows, history = parse_workflows(), git_history(catalog)
    total_lines = Counter()
    for row in file_rows:
        total_lines.update(row["lines"])
    kmp_count = sum(1 for item in kmp_modules if item["kmp"])
    native_count = sum(1 for item in kmp_modules if item["native"])
    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "repository": os.environ.get("GITHUB_REPOSITORY", "rachel-ylcs/ylcs-kmp"),
        "revision": git("rev-parse", "HEAD"),
        "branch": os.environ.get("GITHUB_REF_NAME", git("branch", "--show-current")),
        "scope": list(SCOPES),
        "summary": {
            "files": len(file_rows), "text_files": text_files, "binary_files": len(file_rows) - text_files,
            "bytes": sum(row["bytes"] for row in file_rows), "code_lines": total_lines["code"],
            "comment_lines": total_lines["comment"], "blank_lines": total_lines["blank"], "total_lines": total_lines["total"],
            "gradle_modules": len(gradle_modules), "ylcs_module_modules": len(kmp_modules),
            "kmp_modules": kmp_count, "native_modules": native_count,
        },
        "languages": languages, "scopes": scopes, "modules": modules, "assets": assets,
        "quality": {
            "todo_counts": dict(Counter(item["marker"] for item in todo_entries)), "todo_total": len(todo_entries),
            "todo_entries": todo_entries, "empty_files": empty_files,
            "placeholder_only_directories": sorted(placeholder_dirs), "duplicate_groups": duplicates,
            "duplicate_file_count": sum(item["count"] for item in duplicates),
            "duplicate_bytes": sum(item["bytes"] * (item["count"] - 1) for item in duplicates),
            "large_source_files": sorted(large_files, key=lambda item: item["code_lines"], reverse=True),
            "large_file_threshold_code_lines": LONG_FILE_THRESHOLD,
        },
        "kmp": {
            "module_count": len(kmp_modules), "kmp_count": kmp_count, "native_count": native_count,
            "kmp_rate": round(100 * kmp_count / len(kmp_modules), 1) if kmp_modules else 0,
            "native_rate": round(100 * native_count / len(kmp_modules), 1) if kmp_modules else 0,
            "target_counts": target_counts, "test_counts": test_counts, "modules": kmp_modules,
        },
        "build": {
            "submodules": submodules, "workflows": workflows, "workflow_count": len(workflows),
            "version_catalog": catalog,
            "gradle_plugin_usage": [{"alias": key, "module_references": value} for key, value in plugin_usage.most_common()],
            "dependency_alias_usage": [{"alias": key, "references": value} for key, value in dependency_usage.most_common()],
            "external_coordinates": [{"coordinate": key, "references": value} for key, value in coordinates.most_common()],
        },
        "git": history,
    }


def fmt_bytes(value: int) -> str:
    units, number = ("B", "KB", "MB", "GB", "TB"), float(value)
    for unit in units:
        if number < 1024 or unit == units[-1]:
            return f"{number:.1f} {unit}" if unit != "B" else f"{int(number)} B"
        number /= 1024
    return f"{value} B"


def markdown_report(data: dict) -> str:
    summary = data["summary"]
    lines = [
        "# Repository Statistics", "", f"> Generated: {data['generated_at']}  ",
        f"> Revision: {data['revision'][:12]} · Branch: {data['branch']}", "",
        "## Overview", "", "| Metric | Value |", "|---|---:|",
        f"| Files | {summary['files']:,} |", f"| Text / binary files | {summary['text_files']:,} / {summary['binary_files']:,} |",
        f"| Repository size (scoped files) | {fmt_bytes(summary['bytes'])} |",
        f"| Code / comment / blank lines | {summary['code_lines']:,} / {summary['comment_lines']:,} / {summary['blank_lines']:,} |",
        f"| Gradle modules | {summary['gradle_modules']:,} |",
        f"| KMP modules in ylcs-module | {summary['kmp_modules']:,} / {summary['ylcs_module_modules']:,} |",
        "", "## Code by language", "", "| Language | Files | Code | Comments | Blank | Total | Size |", "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for item in data["languages"]:
        row = item["lines"]
        lines.append(f"| {item['language']} | {item['files']:,} | {row['code']:,} | {row['comment']:,} | {row['blank']:,} | {row['total']:,} | {fmt_bytes(item['bytes'])} |")
    lines.extend(["", "## Scope breakdown", "", "| Scope | Files | Code lines | Total lines | Size |", "|---|---:|---:|---:|---:|"])
    for item in data["scopes"]:
        lines.append(f"| {item['scope']} | {item['files']:,} | {item['lines']['code']:,} | {item['lines']['total']:,} | {fmt_bytes(item['bytes'])} |")
    lines.extend(["", "## Largest modules", "", "| Module | Files | Code lines | Size |", "|---|---:|---:|---:|"])
    for item in data["modules"][:20]:
        lines.append(f"| {item['module']} | {item['files']:,} | {item['lines']['code']:,} | {fmt_bytes(item['bytes'])} |")
    lines.extend([
        "", "## KMP module matrix", "",
        f"- KMP modules: **{data['kmp']['kmp_count']} / {data['kmp']['module_count']}** ({data['kmp']['kmp_rate']}%)",
        f"- Native-enabled modules: **{data['kmp']['native_count']} / {data['kmp']['module_count']}** ({data['kmp']['native_rate']}%)",
        "", "| Platform | Modules | Test source sets |", "|---|---:|---:|",
    ])
    for platform, count in data["kmp"]["target_counts"].items():
        lines.append(f"| {platform} | {count} | {data['kmp']['test_counts'].get(platform, 0)} |")
    lines.extend(["", "## Resources and assets", "", "| Category | Files | Unique files | Size | Unique size |", "|---|---:|---:|---:|---:|"])
    for item in data["assets"]:
        lines.append(f"| {item['category']} | {item['files']:,} | {item['unique_files']:,} | {fmt_bytes(item['bytes'])} | {fmt_bytes(item['unique_bytes'])} |")
    quality = data["quality"]
    lines.extend([
        "", "## Quality indicators", "",
        f"- TODO markers: **{quality['todo_total']}** ({', '.join(f'{key}: {value}' for key, value in quality['todo_counts'].items()) or 'none'})",
        f"- Empty / whitespace-only files: **{len(quality['empty_files'])}**",
        f"- Placeholder-only directories: **{len(quality['placeholder_only_directories'])}**",
        f"- Duplicate-content groups: **{len(quality['duplicate_groups'])}** ({quality['duplicate_file_count']} files; {fmt_bytes(quality['duplicate_bytes'])} redundant bytes)",
        f"- Source files over {quality['large_file_threshold_code_lines']:,} code lines: **{len(quality['large_source_files'])}**",
        "", "## Build and dependencies", "",
        f"- Git submodules: **{len(data['build']['submodules'])}**",
        f"- GitHub Actions workflows: **{data['build']['workflow_count']}**",
        f"- Version catalog aliases: **{data['build']['version_catalog'].get('counts', {}).get('versions', 0)} versions, {data['build']['version_catalog'].get('counts', {}).get('plugins', 0)} plugins, {data['build']['version_catalog'].get('counts', {}).get('libraries', 0)} libraries",
        "", "### Workflows", "", "| Workflow | Triggers | Runners | Jobs |", "|---|---|---|---:|",
    ])
    for workflow in data["build"]["workflows"]:
        lines.append(f"| {workflow['file']} | {', '.join(workflow['triggers'])} | {', '.join(workflow['runners'])} | {len(workflow['jobs'])} |")
    lines.extend(["", "## Release history", "", "| From | To | Commits | Files + / − / ~ | Lines + / − | Code + / − | Net code | Commit range | Release date |", "|---|---|---:|---:|---:|---:|---:|---|---|"])
    for item in data["git"]["comparisons"]:
        commit_range = f"{item['first_commit_date'] or '—'} → {item['last_commit_date'] or '—'}"
        file_changes = f"{item['added_files']} / {item['deleted_files']} / {item['modified_files'] + item['renamed_files']}"
        line_changes = f"{item['added_lines']:,} / {item['deleted_lines']:,}"
        code_changes = f"{item['added_code_lines']:,} / {item['deleted_code_lines']:,}"
        lines.append(f"| {item['from']} | {item['to']} | {item['commit_count']} | {file_changes} | {line_changes} | {code_changes} | {item['net_code_lines']:+,} | {commit_range} | {item['release_date'] or 'Unreleased'} |")
    if not data["git"]["comparisons"]:
        lines.append("| — | — | 0 | 0 / 0 / 0 | 0 / 0 | 0 / 0 | 0 | — | — |")
    lines.extend(["", "---", "", "Generated by .github/scripts/repository_statistics.py."])
    return "\n".join(lines) + "\n"


def html_report(data: dict) -> str:
    payload = json.dumps(data, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/")
    template = r"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Repository Statistics</title>
<style>
:root{color-scheme:light dark;--bg:#f5f7fb;--panel:#fff;--text:#172033;--muted:#68738a;--line:#e5e9f1;--accent:#4969d8;--bar:#7890ed}
@media(prefers-color-scheme:dark){:root{--bg:#111827;--panel:#1b2638;--text:#edf2fb;--muted:#a8b3c7;--line:#344157;--accent:#91a8ff;--bar:#6d83d7}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:14px/1.55 system-ui,-apple-system,"Segoe UI",sans-serif}
main{max-width:1440px;margin:auto;padding:28px 24px 64px}.top{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;flex-wrap:wrap}h1{font-size:28px;margin:0 0 4px}.muted{color:var(--muted)}h2{font-size:18px;margin:0 0 16px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px;margin:22px 0}.card,.panel{background:var(--panel);border:1px solid var(--line);border-radius:14px;padding:18px;box-shadow:0 2px 8px #00000008}.metric{font-size:27px;font-weight:700;letter-spacing:-.6px}.metric-label{color:var(--muted);font-size:12px}.columns{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,420px),1fr));gap:16px;margin:16px 0}.panel{min-width:0;margin:16px 0}.bar-list{display:grid;gap:10px}.bar-row{display:grid;grid-template-columns:minmax(100px,1.2fr) minmax(100px,3fr) 72px;gap:12px;align-items:center}.bar-label{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.track{height:10px;background:var(--line);border-radius:10px;overflow:hidden}.fill{height:100%;background:var(--bar);border-radius:10px}.bar-value{text-align:right;font-variant-numeric:tabular-nums;color:var(--muted)}table{border-collapse:collapse;width:100%;font-size:13px}th,td{text-align:left;padding:9px 10px;border-bottom:1px solid var(--line);vertical-align:top}th{color:var(--muted);font-weight:600;white-space:nowrap}tbody tr:hover{background:color-mix(in srgb,var(--accent) 7%,transparent)}.scroll{overflow:auto;max-height:560px}.controls{display:flex;gap:10px;flex-wrap:wrap;margin:0 0 12px}input,select{background:var(--panel);color:var(--text);border:1px solid var(--line);border-radius:8px;padding:9px 11px;min-width:180px}.pill{display:inline-block;border:1px solid var(--line);border-radius:20px;padding:2px 8px;color:var(--muted);font-size:11px}.details{color:var(--muted);font-size:12px}.empty{padding:18px;color:var(--muted)}footer{color:var(--muted);font-size:12px;margin-top:30px}code{overflow-wrap:anywhere}
@media(max-width:640px){main{padding:18px 12px 40px}.bar-row{grid-template-columns:90px 1fr 58px;gap:7px}.metric{font-size:23px}}
</style></head><body><main>
<header class="top"><div><h1>Repository Statistics</h1><div class="muted" id="meta"></div></div><span class="pill">Generated report · interactive</span></header>
<section class="grid" id="metrics"></section>
<div class="columns"><section class="panel"><h2>Code lines by language</h2><div class="bar-list" id="language-chart"></div></section><section class="panel"><h2>Largest modules by code lines</h2><div class="bar-list" id="module-chart"></div></section></div>
<div class="columns"><section class="panel"><h2>KMP platform coverage</h2><div class="bar-list" id="kmp-chart"></div></section><section class="panel"><h2>Resource footprint</h2><div class="bar-list" id="asset-chart"></div></section></div>
<section class="panel"><h2>Modules</h2><div class="controls"><input id="module-search" type="search" placeholder="Filter modules…"><select id="module-scope"><option value="">All scopes</option></select><select id="module-sort"><option value="code">Sort: code lines</option><option value="files">Sort: files</option><option value="bytes">Sort: size</option><option value="name">Sort: name</option></select></div><div class="scroll"><table><thead><tr><th>Module</th><th>Files</th><th>Code lines</th><th>Total lines</th><th>Size</th></tr></thead><tbody id="module-table"></tbody></table></div></section>
<section class="panel"><h2>KMP modules and source sets</h2><div class="scroll"><table><thead><tr><th>Module</th><th>Template</th><th>KMP</th><th>Native</th><th>Targets</th><th>Tests</th><th>Source sets</th></tr></thead><tbody id="kmp-table"></tbody></table></div></section>
<div class="columns"><section class="panel"><h2>Quality indicators</h2><div id="quality"></div></section><section class="panel"><h2>Build and dependency inventory</h2><div id="build"></div></section></div>
<section class="panel"><h2>Release history</h2><div class="scroll"><table><thead><tr><th>From</th><th>To</th><th>Commits</th><th>Files + / − / ~</th><th>Code + / −</th><th>Net code</th><th>Commit range</th><th>Release date</th></tr></thead><tbody id="history-table"></tbody></table></div></section>
<footer id="footer"></footer></main><script>
const DATA=__DATA__;
const fmt=n=>Number(n||0).toLocaleString();
const bytes=n=>{const u=["B","KB","MB","GB","TB"];let i=0,v=Number(n||0);while(v>=1024&&i<u.length-1){v/=1024;i++;}return (i?v.toFixed(1):Math.round(v))+" "+u[i];};
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
document.getElementById("meta").textContent=DATA.repository+" · "+DATA.branch+" · "+DATA.revision.slice(0,12)+" · "+new Date(DATA.generated_at).toLocaleString();
const s=DATA.summary,metrics=[["Files",fmt(s.files)],["Code lines",fmt(s.code_lines)],["Repository footprint",bytes(s.bytes)],["Gradle modules",fmt(s.gradle_modules)],["KMP modules",fmt(s.kmp_modules)+" / "+fmt(s.ylcs_module_modules)],["Native-enabled modules",fmt(s.native_modules)]];
document.getElementById("metrics").innerHTML=metrics.map(x=>'<div class="card"><div class="metric">'+esc(x[1])+'</div><div class="metric-label">'+esc(x[0])+'</div></div>').join("");
function bars(target,items,label,value,format){const max=Math.max(1,...items.map(value));document.getElementById(target).innerHTML=items.length?items.map(item=>'<div class="bar-row" title="'+esc(label(item))+': '+esc(format(value(item)))+'"><div class="bar-label">'+esc(label(item))+'</div><div class="track"><div class="fill" style="width:'+Math.max(0,value(item)/max*100)+'%"></div></div><div class="bar-value">'+esc(format(value(item)))+'</div></div>').join(""):'<div class="empty">No data</div>';}
bars("language-chart",DATA.languages.slice(0,12),x=>x.language,x=>x.lines.code,fmt);
bars("module-chart",DATA.modules.slice(0,12),x=>x.module.split("/").slice(-2).join("/"),x=>x.lines.code,fmt);
bars("kmp-chart",Object.entries(DATA.kmp.target_counts).map(x=>({name:x[0],count:x[1]})),x=>x.name,x=>x.count,fmt);
bars("asset-chart",DATA.assets.slice(0,10),x=>x.category,x=>x.bytes,bytes);
const scopeSelect=document.getElementById("module-scope");
[...new Set(DATA.modules.map(x=>x.module.split("/")[0]))].sort().forEach(x=>scopeSelect.insertAdjacentHTML("beforeend",'<option value="'+esc(x)+'">'+esc(x)+'</option>'));
function renderModules(){let rows=[...DATA.modules];const q=document.getElementById("module-search").value.toLowerCase(),scope=scopeSelect.value,sort=document.getElementById("module-sort").value;rows=rows.filter(x=>x.module.toLowerCase().includes(q)&&(!scope||x.module.startsWith(scope+"/")||x.module===scope));rows.sort((a,b)=>sort==="name"?a.module.localeCompare(b.module):sort==="files"?b.files-a.files:sort==="bytes"?b.bytes-a.bytes:b.lines.code-a.lines.code);document.getElementById("module-table").innerHTML=rows.map(x=>'<tr><td><code>'+esc(x.module)+'</code></td><td>'+fmt(x.files)+'</td><td>'+fmt(x.lines.code)+'</td><td>'+fmt(x.lines.total)+'</td><td>'+bytes(x.bytes)+'</td></tr>').join("")||'<tr><td colspan="5" class="empty">No matching modules</td></tr>';}
["module-search","module-scope","module-sort"].forEach(id=>document.getElementById(id).addEventListener(id==="module-search"?"input":"change",renderModules));renderModules();
document.getElementById("kmp-table").innerHTML=DATA.kmp.modules.map(x=>'<tr><td><code>'+esc(x.path)+'</code></td><td>'+esc(x.template)+'</td><td>'+(x.kmp?"✓":"—")+'</td><td>'+(x.native?"✓":"—")+'</td><td>'+Object.entries(x.targets).filter(y=>y[1]).map(y=>esc(y[0])).join(", ")+'</td><td>'+Object.entries(x.tests).filter(y=>y[1]).map(y=>esc(y[0])).join(", ")+'</td><td>'+esc(x.source_sets.join(", "))+'</td></tr>').join("")||'<tr><td colspan="7" class="empty">No KMP modules detected</td></tr>';
const q=DATA.quality;
document.getElementById("quality").innerHTML='<p><b>'+fmt(q.todo_total)+'</b> TODO markers · '+Object.entries(q.todo_counts).map(x=>esc(x[0])+" "+fmt(x[1])).join(" · ")+'</p><p>'+fmt(q.empty_files.length)+' empty/whitespace files</p><p>'+fmt(q.placeholder_only_directories.length)+' placeholder-only directories</p><p>'+fmt(q.duplicate_groups.length)+' duplicate groups · '+fmt(q.duplicate_file_count)+' files · '+bytes(q.duplicate_bytes)+' redundant bytes</p><p>'+fmt(q.large_source_files.length)+' source files exceed '+fmt(q.large_file_threshold_code_lines)+' code lines</p>';
const b=DATA.build,cat=b.version_catalog;
document.getElementById("build").innerHTML='<p>'+fmt(b.submodules.length)+' submodules · '+fmt(b.workflow_count)+' workflows</p><p>Version catalog: '+fmt(cat.counts?.versions)+' versions · '+fmt(cat.counts?.plugins)+' plugins · '+fmt(cat.counts?.libraries)+' libraries</p><p>'+fmt(b.gradle_plugin_usage.length)+' Gradle plugin aliases referenced · '+fmt(b.dependency_alias_usage.length)+' dependency aliases referenced</p><p>'+fmt(b.external_coordinates.length)+' literal external coordinates</p><details><summary>Workflow runners and actions</summary>'+b.workflows.map(w=>'<p><b>'+esc(w.name)+'</b> — '+esc(w.runners.join(", ")||"runner not detected")+'<br><span class="details">'+esc(w.file)+' · '+esc(w.triggers.join(", "))+'</span></p>').join("")+'</details>';
document.getElementById("history-table").innerHTML=DATA.git.comparisons.map(x=>'<tr><td>'+esc(x.from)+'</td><td>'+esc(x.to)+'</td><td>'+fmt(x.commit_count)+'</td><td>'+fmt(x.added_files)+' / '+fmt(x.deleted_files)+' / '+fmt(x.modified_files+x.renamed_files)+'</td><td>'+fmt(x.added_code_lines)+' / '+fmt(x.deleted_code_lines)+'</td><td>'+fmt(x.net_code_lines)+'</td><td>'+esc((x.first_commit_date||"—")+" → "+(x.last_commit_date||"—"))+'</td><td>'+esc(x.release_date||"Unreleased")+'</td></tr>').join("")||'<tr><td colspan="8" class="empty">No adjacent version comparisons available</td></tr>';
document.getElementById("footer").textContent="Generated from "+DATA.scope.join(", ")+". Vendor/upstream directories, Git submodules and gitignored paths are excluded.";
</script></body></html>"""
    return template.replace("__DATA__", payload)


def write_reports(data: dict, output: Path, summary_path: Path | None) -> None:
    output.mkdir(parents=True, exist_ok=True)
    json_path, markdown_path, html_path = output / "statistic.json", output / "statistic.md", output / "statistic.html"
    json_path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    markdown_path.write_text(markdown_report(data), encoding="utf-8")
    html_path.write_text(html_report(data), encoding="utf-8")
    if summary_path:
        summary_path.parent.mkdir(parents=True, exist_ok=True)
        summary_path.write_text(markdown_report(data), encoding="utf-8")
    print(f"Reports written to {output}")
    for path in (markdown_path, json_path, html_path):
        display = path.relative_to(ROOT) if path.is_relative_to(ROOT) else path
        print(f" - {display}: {path.stat().st_size:,} bytes")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=ROOT / "build" / "statistic")
    parser.add_argument("--step-summary", type=Path)
    args = parser.parse_args()
    data = collect_statistics()
    output = args.output_dir if args.output_dir.is_absolute() else ROOT / args.output_dir
    write_reports(data, output, args.step_summary)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
