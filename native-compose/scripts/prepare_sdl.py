"""Build the audited SDL3 source as a static SDK for the current desktop host."""
import argparse
import json
import os
from pathlib import Path
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
    # Kotlin/Native invokes ld directly; pkg-config may emit compiler-driver flags.
    linker_tokens = [part for token in tokens for part in (token[4:].split(",") if token.startswith("-Wl,") else [token])]
    for token in linker_tokens:
        if token == "-Xlinker":
            continue
        if token.startswith("-L") or token in ("-lSDL3", "-lSDL3-static", "-mwindows"):
            continue
        if token == "-pthread":
            token = "-lpthread"
        if re.search(r"[A-Za-z]:[\\/]|/(?:Users|home|tmp|private|Volumes)/", token):
            raise ValueError(f"Non-portable SDL link dependency: {token}")
        result.append(token)
    return result


def static_link_options(prefix, environment):
    pc = (prefix / "lib/pkgconfig/sdl3.pc").read_text(encoding="utf-8")
    requires = re.findall(r"^Requires(?:\.private)?:[ \t]*(.*)$", pc, re.MULTILINE)
    if any(value.strip() for value in requires):
        pkg_config = shutil.which("pkg-config", path=environment.get("PATH")) or shutil.which("pkgconf", path=environment.get("PATH"))
        if not pkg_config:
            raise ValueError("pkg-config is required to resolve SDL's private dependencies")
        pkg_environment = dict(environment)
        pkg_environment["PKG_CONFIG_PATH"] = str(prefix / "lib/pkgconfig") + os.pathsep + environment.get("PKG_CONFIG_PATH", "")
        text = subprocess.check_output([pkg_config, "--static", "--libs", "sdl3"], env=pkg_environment, text=True)
    else:
        # Standalone metadata needs no resolver. Windows runners may expose an
        # unrelated, broken Strawberry Perl pkg-config.BAT before the real tools.
        text = " ".join(re.findall(r"^Libs(?:\.private)?:[ \t]*(.*)$", pc, re.MULTILINE))
        # Directory flags are producer inputs, never consumer linker options.
        text = re.sub(r'-L(?:"[^"]*"|\S+)', "", text)
    return portable_link_options(shlex.split(text))


def linux_native_home(location_file=None):
    """Use the distribution selected by this Gradle build, including native.home overrides."""
    if location_file is None:
        state = json.loads((ROOT / "vendor/skiko/state.json").read_text(encoding="utf-8"))
        location_file = ROOT / state["build"] / "build/konan.txt"
        if not location_file.is_file():
            subprocess.run([str(ROOT.parent / "gradlew"), "--configure-on-demand", "--max-workers=2",
                            "-Pnative.compose=true", ":skiko:downloadKotlinNativeDistribution"], cwd=ROOT.parent, check=True)
            state = json.loads((ROOT / "vendor/skiko/state.json").read_text(encoding="utf-8"))
            location_file = ROOT / state["build"] / "build/konan.txt"
    return Path(location_file.read_text(encoding="utf-8").strip()).resolve()


def linux_cmake_options(native_home, environment):
    """Build against Kotlin/Native's libc ABI, while retaining host desktop development headers."""
    properties = (native_home / "konan/konan.properties").read_text(encoding="utf-8")
    names = {}
    for key in ["toolchainDependency.linux_x64", "llvm.linux_x64.user"]:
        match = re.search(rf"(?m)^{re.escape(key)}[ \t]*=[ \t]*([A-Za-z0-9_.-]+)[ \t]*$", properties)
        if not match:
            raise ValueError(f"Cannot resolve {key} from the selected Kotlin/Native distribution")
        names[key] = match[1]
    dependencies = Path(environment.get("KONAN_DATA_DIR", Path.home() / ".konan")) / "dependencies"
    toolchain = dependencies / names["toolchainDependency.linux_x64"]
    llvm = dependencies / names["llvm.linux_x64.user"]
    sysroot = toolchain / "x86_64-unknown-linux-gnu/sysroot"
    compiler = llvm / "bin/clang"
    ar = toolchain / "bin/x86_64-unknown-linux-gnu-ar"
    ranlib = toolchain / "bin/x86_64-unknown-linux-gnu-ranlib"
    required = [compiler, ar, ranlib, sysroot / "usr/include/features.h"]
    if not all(path.is_file() for path in required):
        # A real CInterop invocation prepares the same audited dependencies used
        # by the subsequent SDL bindings, even on a fresh runner with no LLVM cache.
        bootstrap = VENDOR / "build/sdl-static/bootstrap"
        bootstrap.mkdir(parents=True, exist_ok=True)
        definition = bootstrap / "toolchain.def"
        definition.write_text("headers = stdint.h\npackage = love.yinlin.nativecompose.toolchain\n", encoding="utf-8")
        subprocess.run([str(native_home / "bin/cinterop"), "-def", str(definition), "-target", "linux_x64",
                        "-o", str(bootstrap / "toolchain.klib")], env=environment, check=True)
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise ValueError(f"Kotlin/Native Linux C toolchain is incomplete: {missing}")
    # -idirafter lets SDL find X11/Wayland/audio headers without selecting the
    # runner's newer glibc headers ahead of Kotlin/Native's sysroot.
    options = [f"-DCMAKE_C_COMPILER={compiler}", "-DCMAKE_C_COMPILER_TARGET=x86_64-unknown-linux-gnu",
               f"-DCMAKE_C_COMPILER_EXTERNAL_TOOLCHAIN={toolchain}", f"-DCMAKE_SYSROOT={sysroot}",
               f"-DCMAKE_AR={ar}", f"-DCMAKE_RANLIB={ranlib}",
               "-DCMAKE_C_FLAGS=-idirafter /usr/include -idirafter /usr/include/x86_64-linux-gnu"]
    return options, names


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", action="store_true", help="Fetch audited headers/source only")
    parser.add_argument("--build", action="store_true", help="Build a static host SDK, including on Windows")
    parser.add_argument("--jobs", type=int, default=2)
    parser.add_argument("--konan-home-file", type=Path, help="Gradle's selected Kotlin/Native distribution location (Linux)")
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
    profile = {"version": version, "target": target, "archive": archive_name, "linkage": "static", "buildType": "Release",
               "sourceSha256": entry["sha256"], "scriptSha256": digest(Path(__file__))}
    environment = windows_environment() if target == "mingw_x64" else dict(os.environ)
    native_options = []
    if target == "linux_x64":
        native_options, profile["nativeToolchain"] = linux_cmake_options(linux_native_home(args.konan_home_file), environment)
    if profile_file.is_file():
        previous = json.loads(profile_file.read_text(encoding="utf-8"))
        if all(previous.get(key) == value for key, value in profile.items()) and (prefix / "lib" / archive_name).is_file():
            print(f"Static SDL SDK ready: {prefix}", flush=True)
            return
    # Keep the old host-libc CMake cache separate from the compiler/sysroot ABI.
    build = VENDOR / "build/sdl-static" / ("linux_x64-konan" if target == "linux_x64" else target)
    options = [
        f"-DCMAKE_INSTALL_PREFIX={prefix}", "-DCMAKE_INSTALL_LIBDIR=lib", "-DCMAKE_BUILD_TYPE=Release",
        "-DSDL_SHARED=OFF", "-DSDL_STATIC=ON", "-DCMAKE_POSITION_INDEPENDENT_CODE=ON",
        "-DSDL_TEST_LIBRARY=OFF", "-DSDL_TESTS=OFF", "-DSDL_EXAMPLES=OFF",
        *native_options,
    ]
    if target == "mingw_x64":
        options += ["-G", "Ninja", "-DCMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded"]
    elif target == "macos_arm64":
        options += ["-DCMAKE_OSX_ARCHITECTURES=arm64", "-DCMAKE_OSX_DEPLOYMENT_TARGET=12.0"]
    subprocess.run(["cmake", "-S", str(source), "-B", str(build), *options], env=environment, check=True)
    subprocess.run(["cmake", "--build", str(build), "--config", "Release", "--parallel", str(args.jobs)], env=environment, check=True)
    subprocess.run(["cmake", "--install", str(build), "--config", "Release"], env=environment, check=True)
    library = prefix / "lib" / archive_name
    if not library.is_file() or not library.read_bytes().startswith(b"!<arch>\n"):
        raise ValueError(f"CMake did not produce a static SDL archive: {library}")
    profile["linkerOpts"] = static_link_options(prefix, environment)
    profile_file.write_text(json.dumps(profile, indent=2) + "\n", encoding="utf-8")
    print(f"Static SDL SDK built: {prefix}", flush=True)


if __name__ == "__main__":
    main()
