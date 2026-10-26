"""Prepare resources from its own pinned Compose repository, without the upstream build plugins."""
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
RESOURCE_VENDOR = ROOT / "vendor/resources"
RESOURCE_WORKSPACE = RESOURCE_VENDOR / "workspace"
SOURCE_ROOT = "components/resources/library"
SHADOWS = [
    SOURCE_ROOT + "/src/nativeMain/kotlin/org/jetbrains/compose/resources/ResourceLock.native.kt",
    SOURCE_ROOT + "/src/nativeMain/kotlin/org/jetbrains/compose/resources/vector/xmldom/DomXmlParser.kt",
]
ENVIRONMENT = SOURCE_ROOT + "/src/commonMain/kotlin/org/jetbrains/compose/resources/ResourceEnvironment.kt"


def git(*args, check=True):
    return subprocess.run(["git", "-c", f"safe.directory={RESOURCE_WORKSPACE.resolve().as_posix()}", "-c", "core.fsmonitor=false",
                           "-C", str(RESOURCE_WORKSPACE), *args], check=check, capture_output=True, text=True, timeout=300)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None


def prepare_resource_sources(source, version, offline):
    if source["repository"] != "JetBrains/compose-multiplatform" or source["ref"] != f"v{version}" or not re.fullmatch(r"[a-f0-9]{40}", source["commit"]):
        raise ValueError("Review the Compose resources source identity and release before preparing it")
    if RESOURCE_WORKSPACE.is_symlink() or not RESOURCE_WORKSPACE.resolve().is_relative_to((ROOT / "vendor").resolve()):
        raise ValueError("Compose resources checkout escapes vendor")
    repository = f"https://github.com/{source['repository']}.git"
    if not (RESOURCE_WORKSPACE / ".git").is_dir():
        if RESOURCE_WORKSPACE.exists() and any(RESOURCE_WORKSPACE.iterdir()):
            raise ValueError("Unrecognized Compose resources workspace preserved")
        if offline:
            raise ValueError("Offline Compose resources checkout is missing")
        RESOURCE_WORKSPACE.mkdir(parents=True, exist_ok=True)
        git("init")
        git("config", "core.autocrlf", "false")
        git("remote", "add", "origin", repository)
        git("config", "remote.origin.promisor", "true")
        git("config", "remote.origin.partialclonefilter", "blob:none")
    if git("remote", "get-url", "origin").stdout.strip() != repository:
        raise ValueError("Unexpected Compose resources origin; workspace preserved")
    if git("diff", "--cached", "--quiet", check=False).returncode:
        raise ValueError("Compose resources has staged edits; workspace preserved")
    record = RESOURCE_VENDOR / "source-edits.json"
    if record.is_file():
        previous = json.loads(record.read_text(encoding="utf-8"))
        if previous["commit"] != git("rev-parse", "HEAD").stdout.strip():
            raise ValueError("Compose resources managed source commit changed; workspace preserved")
        for path, expected in previous["files"].items():
            # Accept the old shadow-only record and the resumable original/prepared plan.
            allowed = {expected["original"], expected["prepared"]} if isinstance(expected, dict) else ({expected, None} if path in SHADOWS else {expected})
            if path not in [*SHADOWS, ENVIRONMENT] or digest(RESOURCE_WORKSPACE / path) not in allowed:
                raise ValueError(f"Compose resources has local edits in {path}; workspace preserved")
        git("restore", "--source=HEAD", "--worktree", "--", *previous["files"])
    if git("diff", "--quiet", check=False).returncode:
        raise ValueError("Compose resources has local tracked edits; workspace preserved")
    if git("rev-parse", "--verify", "HEAD", check=False).stdout.strip() != source["commit"]:
        if offline:
            raise ValueError("Offline Compose resources checkout does not match its lock")
        git("sparse-checkout", "init", "--cone")
        git("sparse-checkout", "set", SOURCE_ROOT)
        git("fetch", "--filter=blob:none", "--depth=1", "origin", source["commit"])
        git("checkout", "--detach", source["commit"])
    if not (RESOURCE_WORKSPACE / "LICENSE.txt").is_file():
        raise ValueError("Compose resources source license is missing")
    adaptation = ROOT / "adaptation/org.jetbrains.compose.components_components-resources/src/nativeMain/kotlin/org/jetbrains/compose/resources"
    for relative in ["ResourceLock.native.kt", "vector/xmldom/DomXmlParser.native.kt"]:
        if not (adaptation / relative).is_file():
            raise ValueError(f"Compose resources replacement is missing: {relative}")
    hashes = {path: {"original": digest(RESOURCE_WORKSPACE / path), "prepared": None} for path in SHADOWS}
    if any(value["original"] is None for value in hashes.values()):
        raise ValueError("Compose resources upstream source selection drifted")
    environment = RESOURCE_WORKSPACE / ENVIRONMENT
    text = environment.read_text(encoding="utf-8")
    for before, after in [("import androidx.compose.ui.text.intl.Locale\n", "import androidx.compose.ui.platform.LocalLocale\n"),
                          ("val composeLocale = Locale.current", "val composeLocale = LocalLocale.current")]:
        if text.count(before) != 1:
            raise ValueError("Compose resources environment source drifted")
        text = text.replace(before, after, 1)
    hashes[ENVIRONMENT] = {"original": digest(environment), "prepared": hashlib.sha256(text.encode("utf-8")).hexdigest()}
    record.write_text(json.dumps({"commit": source["commit"], "files": hashes}, indent=4) + "\n", encoding="utf-8", newline="\n")
    environment.write_text(text, encoding="utf-8", newline="\n")
    for path in SHADOWS:
        (RESOURCE_WORKSPACE / path).unlink()
