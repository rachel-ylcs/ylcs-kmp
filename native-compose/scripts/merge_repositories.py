"""Assemble three host publications, choosing exactly one canonical KMP root."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys
from sync_upstream import ROOT, load_lock

TARGETS = {"mingw_x64": "mingwx64", "linux_x64": "linuxx64", "macos_arm64": "macosarm64"}
ARTIFACTS = ["skiko", "skiko-skottie"]


def merge(inputs, output, lock):
    if set(inputs) != set(TARGETS):
        raise ValueError(f"Exactly these host publications are required: {', '.join(TARGETS)}")
    output = output.resolve()
    for directory in inputs.values():
        source = directory.resolve()
        if output.is_relative_to(source) or source.is_relative_to(output):
            raise ValueError("Input and output repositories must not overlap")
    if output.exists() and any(output.iterdir()):
        raise ValueError(f"Refusing to mix publications with a non-empty repository: {output}")
    group = Path(*lock["group"].split("."))
    modules = [(inputs["macos_arm64"], artifact) for artifact in ARTIFACTS]
    modules += [(inputs[target], f"{artifact}-{suffix}") for artifact in ARTIFACTS for target, suffix in TARGETS.items()]
    # Validate every input before copying anything. Do not let one host overwrite another's root.
    for repository, module in modules:
        directory = repository / group / module
        version = lock["versions"]["skiko"]
        if not (directory / version).is_dir():
            raise ValueError(f"Missing publication: {directory / version}")
        if any(path.is_symlink() for path in [directory, *directory.rglob("*")]):
            raise ValueError(f"Symlink in Maven publication: {directory}")
    for repository, module in modules:
        shutil.copytree(repository / group / module, output / group / module)
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", action="append", required=True, metavar="TARGET=REPOSITORY")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    inputs = {}
    for value in args.input:
        target, separator, directory = value.partition("=")
        if not separator or target in inputs or target not in TARGETS:
            parser.error(f"Invalid or duplicate --input: {value}")
        inputs[target] = Path(directory)
    output = merge(inputs, args.output, load_lock())
    for target in TARGETS:
        subprocess.run([sys.executable, str(ROOT / "scripts/verify_repository.py"), "--repository", str(output), "--target", target], check=True)
    print(json.dumps({"result": "PASS", "canonical_root": "macos_arm64", "targets": list(TARGETS), "repository": str(output)}, indent=2))


if __name__ == "__main__":
    main()
