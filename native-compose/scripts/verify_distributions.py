"""Extract and run the packaged Core samples from new directories containing spaces."""
import argparse
from pathlib import Path, PurePosixPath
import shutil
import struct
import subprocess
import tempfile
import zipfile
from sync_upstream import ROOT, host_skia_key

HOST_TARGETS = {"windows-Release-x64": "mingw_x64", "linux-Release-x64": "linux_x64", "macos-Release-arm64": "macos_arm64"}


def imported_windows_libraries(executable):
    """Read the PE import table without relying on a local MSVC installation."""
    data = executable.read_bytes()
    pe = struct.unpack_from("<I", data, 0x3C)[0]
    if data[pe:pe + 4] != b"PE\0\0":
        raise ValueError(f"Invalid Windows executable: {executable}")
    section_count = struct.unpack_from("<H", data, pe + 6)[0]
    optional_size = struct.unpack_from("<H", data, pe + 20)[0]
    optional = pe + 24
    magic = struct.unpack_from("<H", data, optional)[0]
    if magic != 0x20B:
        raise ValueError("Expected an x64 PE32+ executable")
    imports = struct.unpack_from("<I", data, optional + 112 + 8)[0]
    sections = []
    for index in range(section_count):
        size, address, raw_size, raw = struct.unpack_from("<IIII", data, optional + optional_size + index * 40 + 8)
        sections.append((address, max(size, raw_size), raw))

    def offset(address):
        for start, size, raw in sections:
            if start <= address < start + size:
                return raw + address - start
        raise ValueError("Invalid RVA in executable import table")

    result = []
    position = offset(imports)
    while any(struct.unpack_from("<IIIII", data, position)):
        name = offset(struct.unpack_from("<I", data, position + 12)[0])
        result.append(data[name:data.index(b"\0", name)].decode("ascii"))
        position += 20
    return result


def verify_static_sdl(executable, target):
    if target == "mingw_x64":
        libraries = imported_windows_libraries(executable)
    else:
        command = ["otool", "-L", str(executable)] if target == "macos_arm64" else ["readelf", "-d", str(executable)]
        libraries = subprocess.check_output(command, text=True).splitlines()
    if any("sdl3" in library.lower() for library in libraries):
        raise ValueError(f"Executable still depends on a dynamic SDL library: {libraries}")
    print(f"PASS: static SDL dependencies: {executable.name}", flush=True)


def extract(archive, destination):
    with zipfile.ZipFile(archive) as source:
        if sum(item.file_size for item in source.infolist()) > 1024 * 1024 * 1024:
            raise ValueError("Distribution exceeds the 1 GiB verification limit")
        for item in source.infolist():
            path = PurePosixPath(item.filename)
            if path.is_absolute() or any(part in ("..", ".") or ":" in part or "\\" in part for part in path.parts):
                raise ValueError(f"Unsafe distribution entry: {item.filename}")
            output = destination.joinpath(*path.parts)
            if not output.resolve().is_relative_to(destination.resolve()) or (item.external_attr >> 16) & 0o170000 == 0o120000:
                raise ValueError(f"Unsafe distribution path: {item.filename}")
            if item.is_dir():
                output.mkdir(parents=True, exist_ok=True)
            else:
                output.parent.mkdir(parents=True, exist_ok=True)
                with source.open(item) as src, output.open("wb") as dst:
                    shutil.copyfileobj(src, dst)
                mode = (item.external_attr >> 16) & 0o777
                if mode:
                    output.chmod(mode)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", choices=list(HOST_TARGETS.values()), required=True)
    parser.add_argument("--renderer", choices=["gpu", "software"], default="software")
    parser.add_argument("--include-showcase", action="store_true")
    args = parser.parse_args()
    if HOST_TARGETS[host_skia_key()] != args.target:
        parser.error("Run distributions on their matching host")
    work = ROOT / "vendor/build/distribution-verification"
    work.mkdir(parents=True, exist_ok=True)
    samples = [("skiko", ROOT / "vendor/build/samples/skiko/distributions", "skiko", "debug")]
    if args.include_showcase:
        samples.append(("native-compose", ROOT.parent / "ylcs-app/native-compose/build/distributions", "native_compose", "release"))
    for sample, distributions, binary_name, build_type in samples:
        archives = list(distributions.glob(f"{sample}-*-{args.target}-{build_type}.zip"))
        # Gradle omits the version segment when project.version is unspecified.
        archives += list(distributions.glob(f"{sample}-{args.target}-{build_type}.zip"))
        if len(archives) != 1:
            raise ValueError(f"Expected one {build_type} distribution for {sample}/{args.target}: {archives}")
        destination = Path(tempfile.mkdtemp(prefix=f"{sample} relocated ", dir=work))
        extract(archives[0], destination)
        dynamic_sdl = [path for path in destination.rglob("*") if path.is_file() and "sdl3" in path.name.lower() and any(suffix in path.name.lower() for suffix in [".dll", ".so", ".dylib"])]
        if dynamic_sdl:
            raise ValueError(f"Static SDL distribution contains dynamic SDL libraries: {dynamic_sdl}")
        executables = [destination / name for name in [binary_name, f"{binary_name}.exe", f"{binary_name}.kexe"] if (destination / name).is_file()]
        if len(executables) != 1:
            raise ValueError(f"Missing executable in {archives[0]}")
        executable = executables[0]
        verify_static_sdl(executable, args.target)
        if args.target != "mingw_x64" and not executable.stat().st_mode & 0o111:
            raise ValueError(f"Executable permissions lost in {archives[0]}")
        command = [str(executable), "--smoke"]
        if args.renderer == "software":
            command.append("--software")
        subprocess.run(command, cwd=destination, check=True, timeout=30)
        print(f"PASS: relocated {sample}/{args.target}: {destination}", flush=True)


if __name__ == "__main__":
    main()
