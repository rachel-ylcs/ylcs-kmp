"""Extract and run the packaged Core samples from new directories containing spaces."""
import argparse
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import tempfile
import zipfile
from sync_upstream import ROOT, host_skia_key

HOST_TARGETS = {"windows-Release-x64": "mingw_x64", "linux-Release-x64": "linux_x64", "macos-Release-arm64": "macos_arm64"}


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
    samples = [(sample, ROOT / "vendor/build/samples" / sample / "distributions", sample, sample == "skiko-window")
               for sample in ["skiko-offscreen", "skiko-window"]]
    if args.include_showcase:
        samples.append(("native-compose", ROOT.parent / "ylcs-app/native-compose/build/distributions", "native_compose", True))
    for sample, distributions, binary_name, window in samples:
        archives = list(distributions.glob(f"{sample}-*-{args.target}-debug.zip"))
        # Gradle omits the version segment when project.version is unspecified.
        archives += list(distributions.glob(f"{sample}-{args.target}-debug.zip"))
        if len(archives) != 1:
            raise ValueError(f"Expected one Debug distribution for {sample}/{args.target}: {archives}")
        destination = Path(tempfile.mkdtemp(prefix=f"{sample} relocated ", dir=work))
        extract(archives[0], destination)
        executables = [destination / name for name in [binary_name, f"{binary_name}.exe", f"{binary_name}.kexe"] if (destination / name).is_file()]
        if len(executables) != 1:
            raise ValueError(f"Missing executable in {archives[0]}")
        executable = executables[0]
        if args.target != "mingw_x64" and not executable.stat().st_mode & 0o111:
            raise ValueError(f"Executable permissions lost in {archives[0]}")
        command = [str(executable)] + (["--smoke"] if window else [])
        if window and args.renderer == "software":
            command.append("--software")
        subprocess.run(command, cwd=destination, check=True, timeout=30)
        print(f"PASS: relocated {sample}/{args.target}: {destination}", flush=True)


if __name__ == "__main__":
    main()
