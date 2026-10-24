"""Read-only host toolchain diagnostics. Does not install tools or change the environment."""
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
from sync_upstream import ROOT, host_skia_key, load_lock


def main():
    lock = load_lock()
    print(f"Kotlin {lock['versions']['kotlin']}; Skiko {lock['versions']['skiko']}; Skia {lock['skia']}")
    failed = []
    try:
        print(f"Skia host asset: {host_skia_key()}")
    except ValueError as error:
        failed.append(str(error))
    print(f"Python {platform.python_version()}: {sys.executable}")
    for name in ["java", "git"]:
        path = shutil.which(name)
        print(f"{name}: {path or 'MISSING'}")
        if not path:
            failed.append(name)
    if shutil.which("java"):
        result = subprocess.run(["java", "-XshowSettings:properties", "-version"], capture_output=True, text=True)
        properties = {}
        for line in (result.stdout + result.stderr).splitlines():
            key, separator, value = line.strip().partition(" = ")
            if separator and key in ("java.version", "os.arch"):
                properties[key] = value
        print(f"Gradle JDK: {properties.get('java.version', 'UNKNOWN')}; architecture: {properties.get('os.arch', 'UNKNOWN')}")
        if result.returncode or properties.get("java.version", "").split(".")[0] != "25":
            failed.append("JDK 25")
        aliases = {"amd64": "x64", "x86_64": "x64", "arm64": "arm64", "aarch64": "arm64"}
        if aliases.get(properties.get("os.arch", "").lower()) != aliases.get(platform.machine().lower()):
            failed.append("JDK architecture must match the build host")
    if platform.system() == "Windows":
        # The Windows bridge uses the compiler returned by VS discovery (cl.exe).
        # clang-cl is needed only by the optional Linux-to-Windows cross toolchain.
        override = os.environ.get("SKIKO_VSBT_PATH") or os.environ.get("VS_PATH")
        finder = Path(os.environ.get("ProgramFiles(x86)", "C:/Program Files (x86)")) / "Microsoft Visual Studio/Installer/vswhere.exe"
        roots = [Path(override)] if override else []
        if finder.is_file():
            result = subprocess.run([str(finder), "-all", "-products", "*", "-requires", "Microsoft.VisualStudio.Component.VC.Tools.x86.x64", "-property", "installationPath"], capture_output=True, text=True, check=True)
            roots.extend(Path(line.strip()) for line in result.stdout.splitlines() if line.strip())
        compilers = [compiler for root in roots for compiler in root.glob("VC/Tools/MSVC/*/bin/Hostx64/x64/cl.exe")]
        print("MSVC x64: " + (", ".join(map(str, compilers)) or "MISSING (VS Build Tools: Desktop development with C++)"))
        if not compilers:
            failed.append("MSVC C++ toolset")
        sdk = Path(os.environ.get("ProgramFiles(x86)", "C:/Program Files (x86)")) / "Windows Kits/10/Lib"
        sdk_libraries = list(sdk.glob("*/um/x64/d3d12.lib"))
        print("Windows SDK: " + (", ".join(map(str, sdk_libraries)) or "MISSING"))
        if not sdk_libraries:
            failed.append("Windows SDK")
    elif platform.system() == "Linux":
        for name in ["g++", "ar", "nm", "pkg-config"]:
            path = shutil.which(name)
            print(f"{name}: {path or 'MISSING'}")
            if not path:
                failed.append(name)
        if shutil.which("pkg-config"):
            result = subprocess.run(["pkg-config", "--exists", "fontconfig", "gl"], check=False)
            if result.returncode:
                failed.append("fontconfig/GL development packages")
    elif platform.system() == "Darwin":
        if not shutil.which("xcrun"):
            failed.append("Xcode command line tools (xcrun)")
        else:
            for tool in ["clang++", "libtool", "ar", "nm", "lipo"]:
                result = subprocess.run(["xcrun", "--find", tool], capture_output=True, text=True)
                print(f"{tool}: {result.stdout.strip() or 'MISSING'}")
                if result.returncode:
                    failed.append(tool)
            result = subprocess.run(["xcrun", "--sdk", "macosx", "--show-sdk-path"], capture_output=True, text=True)
            print(f"macOS SDK: {result.stdout.strip() or 'MISSING'}")
            if result.returncode:
                failed.append("macOS SDK")
    print(f"Prepared source state: {(ROOT / 'vendor/state.json').is_file()}")
    if failed:
        print("Missing requirements: " + ", ".join(failed))
        return 1
    print("Host prerequisites found; compilation/linkage/runtime still require separate verification.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
