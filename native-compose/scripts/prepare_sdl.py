"""Fetch audited SDL inputs; optionally build a shared Linux/macOS SDK."""
import argparse
import json
import platform
import subprocess
import urllib.request
import shutil
import tomllib
from sync_upstream import ROOT, VENDOR, digest, unpack


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", action="store_true", help="Fetch only source, including on Windows")
    parser.add_argument("--build", action="store_true", help="Build/install the Linux or macOS shared SDK; Windows uses the audited prebuilt SDK")
    parser.add_argument("--jobs", type=int, default=2, help="CMake build parallelism")
    args = parser.parse_args()
    if args.jobs < 1:
        parser.error("--jobs must be positive")
    lock = json.loads((ROOT / "upstream/sdl.json").read_text(encoding="utf-8"))
    version = tomllib.loads((ROOT.parent / "libs.versions.toml").read_text(encoding="utf-8"))["versions"]["sdl"]
    if version != lock["version"]:
        raise ValueError(f"SDL version changed: catalog={version}, audited lock={lock['version']}. Review upstream/sdl.json first.")
    VENDOR.mkdir(parents=True, exist_ok=True)
    for kind in (["windows", "source"] if platform.system() == "Windows" and not args.source else ["source"]):
        entry = lock[kind]
        archive = VENDOR / f"sdl-{kind}-{lock['version']}.zip"
        if not archive.exists():
            with urllib.request.urlopen(entry["url"], timeout=60) as response, archive.with_suffix(".part").open("wb") as target:
                shutil.copyfileobj(response, target)
            archive.with_suffix(".part").replace(archive)
        if digest(archive) != entry["sha256"]:
            raise ValueError(f"SDL SHA-256 mismatch: {archive}")
        destination = VENDOR / f"sdl-{kind}-{lock['version']}"
        unpack(archive, destination)
        print(f"SDL {lock['version']}: {destination}")
    if args.build and platform.system() != "Windows":
        if platform.system() not in ("Linux", "Darwin"):
            raise ValueError("SDL SDK builds require Linux or macOS")
        suffix = "macos" if platform.system() == "Darwin" else "linux"
        build = ROOT / "build" / f"sdl-{suffix}"
        prefix = VENDOR / f"sdl-{suffix}"
        options = [
            f"-DCMAKE_INSTALL_PREFIX={prefix}", "-DCMAKE_INSTALL_LIBDIR=lib",
            "-DCMAKE_BUILD_TYPE=Release", "-DSDL_SHARED=ON", "-DSDL_STATIC=OFF",
            "-DSDL_TEST_LIBRARY=OFF", "-DSDL_TESTS=OFF", "-DSDL_EXAMPLES=OFF",
        ]
        if suffix == "macos":
            options += ["-DCMAKE_OSX_ARCHITECTURES=arm64", "-DCMAKE_INSTALL_NAME_DIR=@rpath"]
        subprocess.run(["cmake", "-S", str(VENDOR / f"sdl-source-{version}"), "-B", str(build), *options], check=True)
        subprocess.run(["cmake", "--build", str(build), "--parallel", str(args.jobs)], check=True)
        subprocess.run(["cmake", "--install", str(build)], check=True)


if __name__ == "__main__":
    main()
