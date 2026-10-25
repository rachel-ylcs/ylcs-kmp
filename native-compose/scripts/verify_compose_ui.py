"""Audit/assemble Native UI Maven files and require real library test reports."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import tomllib
import xml.etree.ElementTree as ET
import zipfile
from sync_compose import ROOT, TARGETS, load_lock
from merge_repositories import merge as merge_skiko
from sync_upstream import load_lock as load_skiko_lock
from verify_repository import verify_module as verify_skiko_module

SUFFIXES = {"mingw_x64": "mingwx64", "linux_x64": "linuxx64", "macos_arm64": "macosarm64"}
TESTS = {"UiUtilTest": 5, "NativeRuntimeTest": 2, "UiGeometryTest": 3, "UiUnitTest": 4,
         "UiGraphicsTest": 6, "UiTextTest": 8, "NativeLocaleTest": 3, "UiSkikoTest": 10, "NativeSkikoBackendTest": 5}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify_files(metadata, directory):
    for variant in metadata["variants"]:
        for entry in variant.get("files", []):
            path = (directory / entry["url"]).resolve()
            require(path.is_relative_to(directory.resolve()), "Maven file path escapes its module")
            require(path.is_file() and path.stat().st_size == entry["size"], f"Missing or wrong-sized Maven artifact: {path}")
            for algorithm in ["sha256", "sha512"]:
                if algorithm in entry:
                    with path.open("rb") as source:
                        require(hashlib.file_digest(source, algorithm).hexdigest() == entry[algorithm], f"Maven checksum mismatch: {path}")


def verify(repository, target, lock):
    suffix = SUFFIXES[target]
    version = lock["version"]
    group = repository / lock["group"].replace(".", "/")
    catalog = tomllib.loads((ROOT.parent / "libs.versions.toml").read_text(encoding="utf-8"))
    if any(lock["modules"][name].get("requiresSkiko") for name in lock["ciModules"]):
        verify_skiko_module(repository, target, load_skiko_lock(), "skiko")
    for artifact in lock["ciModules"]:
        root = group / artifact / version
        native = group / f"{artifact}-{suffix}" / version
        metadata = json.loads((root / f"{artifact}-{version}.module").read_text(encoding="utf-8"))
        target_metadata = json.loads((native / f"{artifact}-{suffix}-{version}.module").read_text(encoding="utf-8"))
        for component, directory in [(metadata, root), (target_metadata, native)]:
            require(component["component"]["group"] == lock["group"] and component["component"]["version"] == version, "Wrong Native UI coordinates")
            verify_files(component, directory)
        variants = [v for v in metadata["variants"] if v.get("attributes", {}).get("org.jetbrains.kotlin.native.target") == target]
        require(any(v.get("available-at", {}).get("module") == f"{artifact}-{suffix}" for v in variants), f"Root metadata omits {artifact}/{target}")
        with zipfile.ZipFile(native / f"{artifact}-{suffix}-{version}.klib") as archive:
            manifests = [name for name in archive.namelist() if name.endswith("/manifest")]
            require(manifests, f"No Native manifest in {artifact}")
            for name in manifests:
                text = archive.read(name).decode("utf-8")
                require(f"native_targets={target}" in text, f"Wrong KLIB target: {artifact}")
                require(f"compiler_version={catalog['versions']['kotlin']}" in text, f"Wrong KLIB compiler: {artifact}")
                identity = next(line.split("=", 1)[1] for line in text.splitlines() if line.startswith("unique_name=")).replace("\\:", ":")
                require(identity == f"org.jetbrains.compose.ui:{artifact}", f"Native UI KLIB must retain the official identity: {identity}")
                for line in text.splitlines():
                    if line.startswith(("linkerOpts=", "libraryPaths=")):
                        require(not re.search(r"[A-Za-z]:[\\/]|/(?:Users|home|workspace|tmp|private|Volumes)/", line), "Producer path leaked into KLIB")
        with zipfile.ZipFile(native / f"{artifact}-{suffix}-{version}-sources.jar") as sources:
            require("META-INF/licenses/LICENSE.txt" in sources.namelist(), "Compose source license is missing")
            require("META-INF/provenance/compose.json" in sources.namelist(), "Compose source identity is missing")
            require(not any("StubsMain/" in name for name in sources.namelist()), "Stub sources leaked into Native UI publication")
            require(any(name.endswith(".kt") for name in sources.namelist()), "Native UI sources are empty")
        dependencies = [d for v in target_metadata["variants"] for d in v.get("dependencies", [])]
        exports = {"ui-unit": ["ui-geometry"], "ui-graphics": ["ui-unit"], "ui-text": ["ui-graphics", "ui-unit"], "ui-skiko": ["ui-graphics", "ui-text"]}
        for dependency in exports.get(artifact, []):
            require(any(d.get("group") == lock["group"] and d.get("module") in {dependency, f"{dependency}-{suffix}"}
                        and d.get("version", {}).get("requires") == version for d in dependencies), f"{artifact} must expose its matching Native {dependency} dependency")
        require(not any(d.get("group") == "org.jetbrains.skiko" for d in dependencies), f"{artifact} must publish its Native Skiko dependency directly")
    print(f"PASS: {len(lock['ciModules'])} Native UI Maven modules / {target} / {version}")


def verify_tests(directory):
    reports = list(directory.glob("**/test-results/**/TEST-*.xml"))
    require(reports, f"No Native UI JUnit reports: {directory}")
    found = {}
    for report in reports:
        suite = ET.parse(report).getroot()
        name = suite.attrib.get("name", "").rsplit(".", 1)[-1]
        if name not in TESTS:
            continue
        require(name not in found, f"Duplicate Native UI test report: {name}")
        require(int(suite.attrib.get("tests", 0)) == TESTS[name], f"Expected {TESTS[name]} tests in {name}")
        require(all(int(suite.attrib.get(key, 0)) == 0 for key in ["errors", "failures", "skipped"]), f"Failed or skipped tests in {name}")
        found[name] = TESTS[name]
    require(found == TESTS, f"Missing Native UI test classes: {TESTS.keys() - found.keys()}")
    print(f"PASS: {sum(found.values())} Native UI / Runtime library regressions")


def merge(inputs, output, lock):
    require(set(inputs) == TARGETS, "All three host repositories are required")
    require(not output.exists(), "Assembly destination already exists; choose a fresh directory")
    group = Path(lock["group"].replace(".", "/"))
    selected = [(inputs["macos_arm64"], artifact) for artifact in lock["ciModules"]]
    selected += [(inputs[target], f"{artifact}-{SUFFIXES[target]}") for target in sorted(TARGETS) for artifact in lock["ciModules"]]
    for repository, artifact in selected:
        directory = repository / group / artifact
        require((directory / lock["version"]).is_dir(), f"Missing publication: {directory}")
        require(not any(path.is_symlink() for path in [directory, *directory.rglob("*")]), "Symlink in Native UI publication")
    if any(lock["modules"][name].get("requiresSkiko") for name in lock["ciModules"]):
        merge_skiko(inputs, output, load_skiko_lock(), artifacts=["skiko"])
    for repository, artifact in selected:
        shutil.copytree(repository / group / artifact, output / group / artifact)
    for target in sorted(TARGETS):
        verify(output, target, lock)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=ROOT / "vendor/build/compose-ui/repository")
    parser.add_argument("--target", choices=sorted(TARGETS))
    parser.add_argument("--tests", type=Path)
    parser.add_argument("--input", action="append", default=[], metavar="TARGET=REPOSITORY")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    lock = load_lock()
    if args.input:
        inputs = {}
        for value in args.input:
            target, separator, repository = value.partition("=")
            if not separator or target not in TARGETS or target in inputs:
                parser.error(f"Invalid or duplicate repository input: {value}")
            inputs[target] = Path(repository)
        if args.output is None:
            parser.error("--input requires --output")
        merge(inputs, args.output, lock)
    if args.target:
        verify(args.repository, args.target, lock)
    if args.tests:
        verify_tests(args.tests)
    if not (args.input or args.target or args.tests):
        parser.error("Select a Maven target audit, test-report audit or repository assembly")


if __name__ == "__main__":
    main()
