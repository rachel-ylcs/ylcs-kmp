"""Prepare one pinned, sparse Compose checkout and its Native-only Gradle build."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import tomllib
from sync_upstream import preparation_lock
from upstream_versions import main_profile, sync_wrappers

ROOT = Path(__file__).resolve().parents[1]
VENDOR = ROOT / "vendor/compose"
WORKSPACE = VENDOR / "workspace"
BUILD = WORKSPACE / "native-desktop"
TARGETS = {"mingw_x64", "linux_x64", "macos_arm64"}


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def load_lock():
    lock = json.loads((ROOT / "upstream/compose.json").read_text(encoding="utf-8"))
    catalog = tomllib.loads((ROOT.parent / "libs.versions.toml").read_text(encoding="utf-8"))
    if lock["version"] != catalog["versions"]["cmp"]:
        raise ValueError("CMP version changed; review upstream/compose.json and pin its matching source commit. The main project does not need a downgrade.")
    if lock["repository"] != "JetBrains/compose-multiplatform-core" or not re.fullmatch(r"[a-f0-9]{40}", lock["commit"]):
        raise ValueError("Invalid Compose source identity")
    if not lock.get("ciModules", lock["modules"]) or not set(lock.get("ciModules", lock["modules"])).issubset(lock["modules"]):
        raise ValueError("Compose CI stage references an unknown module")
    for name, module in lock["modules"].items():
        if not re.fullmatch(r"ui-[a-z]+(?:-[a-z]+)*", name):
            raise ValueError(f"Invalid Compose module name: {name}")
        library = catalog["libraries"][module["alias"]]
        if library["module"] != f"{lock['group']}:{name}" or library["version"] != {"ref": "cmp"}:
            raise ValueError(f"Compose Native coordinates changed for {name}; review the source lock")
        if not module["redirectTargets"] or not set(module["redirectTargets"]).issubset(TARGETS):
            raise ValueError(f"Invalid Compose redirect targets: {name}")
        metadata_targets = set(module.get("metadataTargets", module["redirectTargets"]))
        if not set(module["redirectTargets"]).issubset(metadata_targets) or not metadata_targets.issubset(TARGETS):
            raise ValueError(f"Invalid Compose metadata coverage: {name}")
        for source_set, directories in module["sourceSets"].items():
            if source_set not in {"commonMain", "nativeMain", "windowsMain", "linuxMain", "macosMain"}:
                raise ValueError(f"Unaudited source set: {name}/{source_set}")
            for directory in directories:
                parts = PurePosixPath(directory).parts
                if len(parts) != 3 or parts[0] != "src" or parts[2] != "kotlin" or parts[1] not in {"commonMain", "nonAndroidMain", "nonJvmMain", "skikoMain", "nativeMain", "macosMain"}:
                    raise ValueError(f"Unaudited upstream source path: {name}/{directory}")
    return lock


def checked_path(path):
    resolved = path.resolve()
    if not VENDOR.resolve().is_relative_to(ROOT.resolve()) or not resolved.is_relative_to(VENDOR.resolve()) or resolved == VENDOR.resolve():
        raise ValueError(f"Generated Compose path escapes vendor/compose: {path}")
    return resolved


def git(*arguments, check=True):
    return subprocess.run(["git", "-c", f"safe.directory={WORKSPACE.resolve().as_posix()}", "-c", "core.fsmonitor=false",
                           "-C", str(WORKSPACE), *arguments], check=check, capture_output=True, text=True, timeout=300)


def checkout(lock, offline):
    checked_path(WORKSPACE)
    if not (WORKSPACE / ".git").is_dir():
        if WORKSPACE.exists() and any(WORKSPACE.iterdir()):
            raise ValueError(f"Unrecognized Compose working directory preserved: {WORKSPACE}")
        if offline:
            raise ValueError("Offline Compose checkout is missing")
        WORKSPACE.mkdir(parents=True, exist_ok=True)
        git("init")
        git("config", "core.autocrlf", "false")
        git("remote", "add", "origin", f"https://github.com/{lock['repository']}.git")
        git("config", "remote.origin.promisor", "true")
        git("config", "remote.origin.partialclonefilter", "blob:none")
    origin = git("remote", "get-url", "origin").stdout.strip()
    if origin != f"https://github.com/{lock['repository']}.git":
        raise ValueError("Compose checkout has an unexpected origin; preserved without modification")
    if git("diff", "--quiet", check=False).returncode or git("diff", "--cached", "--quiet", check=False).returncode:
        raise ValueError("Compose checkout has local tracked edits; preserve them in adaptation before preparing sources")
    current = git("rev-parse", "--verify", "HEAD", check=False).stdout.strip()
    selected = f"{lock['commit']}\n" + "\n".join(sorted(lock["modules"])) + "\n"
    marker = VENDOR / "checkout.txt"
    if current == lock["commit"] and marker.is_file() and marker.read_text(encoding="utf-8") == selected:
        return
    if offline:
        raise ValueError("Compose checkout does not match the current source selection; prepare it online first")
    git("sparse-checkout", "init", "--cone")
    git("sparse-checkout", "set", *[f"compose/ui/{name}" for name in lock["modules"]])
    if git("cat-file", "-e", f"{lock['commit']}^{{commit}}", check=False).returncode:
        print(f"Fetching Compose {lock['version']} at {lock['commit']}", flush=True)
        git("fetch", "--filter=blob:none", "--depth=1", "origin", lock["commit"])
    git("checkout", "--detach", lock["commit"])
    if git("rev-parse", "HEAD").stdout.strip() != lock["commit"]:
        raise ValueError("Compose checkout did not select its locked commit")
    marker.write_text(selected, encoding="utf-8", newline="\n")


def write(path, text):
    checked_path(path)
    if not path.is_file() or path.read_text(encoding="utf-8") != text:
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_name(path.name + ".tmp")
        checked_path(temporary)
        temporary.write_text(text, encoding="utf-8", newline="\n")
        temporary.replace(path)


def prepare(lock, offline):
    checkout(lock, offline)
    profile = main_profile(ROOT.parent)
    if not (WORKSPACE / "LICENSE.txt").is_file():
        raise ValueError("Pinned Compose checkout is missing its source license")
    for name, module in lock["modules"].items():
        for directories in module["sourceSets"].values():
            for directory in directories:
                source = WORKSPACE / f"compose/ui/{name}/{directory}"
                if not source.is_dir() or not any(source.rglob("*.kt")):
                    raise ValueError(f"Selected Compose source directory is empty: {source}")
    templates = ROOT / "scripts/templates"
    inputs = [ROOT / "upstream/compose.json", ROOT / "upstream/compose-origins.json", Path(__file__),
              ROOT / "scripts/upstream_versions.py", ROOT / "scripts/sync_upstream.py",
              templates / "compose-ui.settings.gradle.kts.template", templates / "compose-ui.build.gradle.kts.template"]
    inputs += [ROOT.parent / name for name in ["gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat"]]
    projects = []
    for name in lock["modules"]:
        projects += [f'if ("{name}" in selectedModules) {{', f'    include(":{name}")', f'    project(":{name}").apply {{',
                     f'        projectDir = rootDir.resolve("../compose/ui/{name}").canonicalFile',
                     f'        buildFileName = projectDir.toPath().relativize(repositoryDirectory.resolve("native-compose/adaptation/org.jetbrains.compose.ui_{name}/build.gradle.kts").toPath()).toString()', "    }", "}"]
    settings = (templates / "compose-ui.settings.gradle.kts.template").read_text(encoding="utf-8").replace("__PROJECTS__", "\n".join(projects))
    build = (templates / "compose-ui.build.gradle.kts.template").read_text(encoding="utf-8").replace("__JAVA_VERSION__", str(profile["java"]))
    write(BUILD / "settings.gradle.kts", settings)
    write(BUILD / "build.gradle.kts", build)
    properties = {"kotlin.mpp.applyDefaultHierarchyTemplate": "false", **profile["gradleProperties"]}
    write(BUILD / "gradle.properties", "\n".join(f"{key}={value}" for key, value in properties.items()) + "\n")
    wrapper = BUILD / "gradle/wrapper/gradle-wrapper.properties"
    if not wrapper.is_file():
        write(wrapper, "")
    sync_wrappers(BUILD, ROOT.parent)
    hashes = {path.relative_to(ROOT.parent).as_posix(): digest(path) for path in inputs}
    state = {"schema": 1, "build": "vendor/compose/workspace/native-desktop", "commit": lock["commit"],
             "inputs": hashes, "catalogSha256": profile["catalogSha256"], "version": lock["version"],
             "modules": list(lock["modules"]), "java": profile["java"]}
    write(VENDOR / "state.json", json.dumps(state, indent=4) + "\n")
    print(f"Prepared Compose UI {lock['version']}: {', '.join(lock['modules'])} -> {state['build']}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--check-updates", action="store_true")
    args = parser.parse_args()
    lock = load_lock()
    if args.check_updates:
        result = subprocess.run(["git", "ls-remote", f"https://github.com/{lock['repository']}.git", lock["ref"], lock["ref"] + "^{}"],
                                check=True, capture_output=True, text=True)
        print(f"Compose: locked={lock['commit']} remote={result.stdout.strip()}")
        return
    VENDOR.mkdir(parents=True, exist_ok=True)
    with preparation_lock(VENDOR):
        prepare(lock, args.offline)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        sys.exit(str(error) + (f"\n{error.stderr}" if isinstance(error, subprocess.CalledProcessError) else ""))
