"""Build the audited SDL3 source as a static SDK for the current desktop host."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shlex
import shutil
import subprocess
import tomllib
import urllib.request
from sync_upstream import ROOT, VENDOR, digest, host_skia_key, unpack


def windows_environment():
    """Use the same VS_PATH/vcvars64 convention as BuildDesktopNativeTask."""
    environment = dict(os.environ)
    if shutil.which("cl") and environment.get("INCLUDE") and environment.get("LIB"):
        return environment
    visual_studio = environment.get("VS_PATH") or environment.get("SKIKO_VSBT_PATH")
    if not visual_studio:
        raise ValueError("Static SDL requires VS_PATH or an initialized MSVC environment")
    vcvars = Path(visual_studio).resolve() / "VC/Auxiliary/Build/vcvars64.bat"
    if not vcvars.is_file():
        raise ValueError(f"Missing MSVC environment script: {vcvars}")
    helper = VENDOR / "build/sdl-static/msvc-environment.cmd"
    helper.parent.mkdir(parents=True, exist_ok=True)
    helper.write_text('@echo off\ncall "%RACHEL_SDL_VCVARS%" >nul\nif errorlevel 1 exit /b %errorlevel%\nset\n', encoding="utf-8")
    environment["RACHEL_SDL_VCVARS"] = str(vcvars)
    # Capture internally: environment values may contain secrets and must never be logged.
    result = subprocess.run(["cmd.exe", "/d", "/c", str(helper)], env=environment, capture_output=True)
    if result.returncode:
        raise ValueError("Cannot initialize the MSVC environment for static SDL")
    for line in result.stdout.decode("mbcs", errors="replace").splitlines():
        key, separator, value = line.partition("=")
        if separator and key:
            environment[key] = value
    if not shutil.which("ninja", path=environment.get("PATH")):
        ninja = Path(visual_studio) / "Common7/IDE/CommonExtensions/Microsoft/CMake/Ninja"
        if (ninja / "ninja.exe").is_file():
            environment["PATH"] = str(ninja) + os.pathsep + environment.get("PATH", "")
    return environment


def portable_link_options(tokens):
    """Only system dependencies leave the producer; the SDL archive is embedded."""
    result = []
    for token in tokens:
        if token.startswith("-L") or token in ("-lSDL3", "-lSDL3-static", "-mwindows"):
            continue
        if token == "-pthread":
            token = "-lpthread"
        if re.search(r"[A-Za-z]:[\\/]|/(?:Users|home|tmp|private|Volumes)/", token):
            raise ValueError(f"Non-portable SDL link dependency: {token}")
        result.append(token)
    return result


def static_link_options(prefix, environment):
    pkg_config = shutil.which("pkg-config", path=environment.get("PATH"))
    if pkg_config:
        pkg_environment = dict(environment)
        pkg_environment["PKG_CONFIG_PATH"] = str(prefix / "lib/pkgconfig") + os.pathsep + environment.get("PKG_CONFIG_PATH", "")
        text = subprocess.check_output([pkg_config, "--static", "--libs", "sdl3"], env=pkg_environment, text=True)
    else:
        pc = (prefix / "lib/pkgconfig/sdl3.pc").read_text(encoding="utf-8")
        requires = re.search(r"^Requires\.private:[ \t]*(.*)$", pc, re.MULTILINE)
        if requires and requires[1].strip():
            raise ValueError("pkg-config is required to resolve SDL's private dependencies")
        text = " ".join(re.findall(r"^Libs(?:\.private)?:[ \t]*(.*)$", pc, re.MULTILINE))
        # Directory flags are producer inputs, never consumer linker options.
        text = re.sub(r'-L(?:"[^"]*"|\S+)', "", text)
    return portable_link_options(shlex.split(text))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", action="store_true", help="Fetch audited headers/source only")
    parser.add_argument("--build", action="store_true", help="Build a static host SDK, including on Windows")
    parser.add_argument("--jobs", type=int, default=2)
    args = parser.parse_args()
    if args.jobs < 1:
        parser.error("--jobs must be positive")
    lock = json.loads((ROOT / "upstream/sdl.json").read_text(encoding="utf-8"))
    version = tomllib.loads((ROOT.parent / "libs.versions.toml").read_text(encoding="utf-8"))["versions"]["sdl"]
    if version != lock["version"]:
        raise ValueError("SDL source release changed; audit upstream/sdl.json before rebuilding")
    entry = lock["source"]
    VENDOR.mkdir(parents=True, exist_ok=True)
    archive = VENDOR / f"sdl-source-{version}.zip"
    if not archive.is_file():
        temporary = archive.with_suffix(".part")
        with urllib.request.urlopen(entry["url"], timeout=60) as source, temporary.open("wb") as target:
            shutil.copyfileobj(source, target)
        temporary.replace(archive)
    if digest(archive) != entry["sha256"]:
        raise ValueError(f"SDL source SHA-256 mismatch: {archive}")
    source = VENDOR / f"sdl-source-{version}"
    unpack(archive, source)
    print(f"Audited SDL {version}: {source}", flush=True)
    if not args.build or args.source:
        return
    targets = {"windows-Release-x64": "mingw_x64", "linux-Release-x64": "linux_x64", "macos-Release-arm64": "macos_arm64"}
    target = targets[host_skia_key()]
    prefix = VENDOR / "sdl-static" / target
    profile_file = prefix / "rachel-sdl.json"
    archive_name = "SDL3-static.lib" if target == "mingw_x64" else "libSDL3.a"
    profile = {"version": version, "target": target, "archive": archive_name, "linkage": "static",
               "sourceSha256": entry["sha256"], "scriptSha256": digest(Path(__file__))}
    if profile_file.is_file():
        previous = json.loads(profile_file.read_text(encoding="utf-8"))
        if all(previous.get(key) == value for key, value in profile.items()) and (prefix / "lib" / archive_name).is_file():
            print(f"Static SDL SDK ready: {prefix}", flush=True)
            return
    environment = windows_environment() if target == "mingw_x64" else dict(os.environ)
    build = VENDOR / "build/sdl-static" / target
    options = [
        f"-DCMAKE_INSTALL_PREFIX={prefix}", "-DCMAKE_INSTALL_LIBDIR=lib", "-DCMAKE_BUILD_TYPE=Release",
        "-DSDL_SHARED=OFF", "-DSDL_STATIC=ON", "-DCMAKE_POSITION_INDEPENDENT_CODE=ON",
        "-DSDL_TEST_LIBRARY=OFF", "-DSDL_TESTS=OFF", "-DSDL_EXAMPLES=OFF",
    ]
    if target == "mingw_x64":
        options += ["-G", "Ninja", "-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded"]
    elif target == "macos_arm64":
        options += ["-DCMAKE_OSX_ARCHITECTURES=arm64", "-DCMAKE_OSX_DEPLOYMENT_TARGET=12.0"]
    subprocess.run(["cmake", "-S", str(source), "-B", str(build), *options], env=environment, check=True)
    subprocess.run(["cmake", "--build", str(build), "--parallel", str(args.jobs)], env=environment, check=True)
    subprocess.run(["cmake", "--install", str(build)], env=environment, check=True)
    library = prefix / "lib" / archive_name
    if not library.is_file() or not library.read_bytes().startswith(b"!<arch>\n"):
        raise ValueError(f"CMake did not produce a static SDL archive: {library}")
    profile["linkerOpts"] = static_link_options(prefix, environment)
    profile_file.write_text(json.dumps(profile, indent=2) + "\n", encoding="utf-8")
    print(f"Static SDL SDK built: {prefix}", flush=True)


if __name__ == "__main__":
    main()
