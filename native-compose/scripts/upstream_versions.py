"""Apply the main build's shared dependency and tool versions to disposable forks."""
from pathlib import Path
import hashlib
import re
import shutil
import tomllib


def library_versions(catalog):
    result = {}
    for library in catalog.get("libraries", {}).values():
        module = library.get("module") or f"{library.get('group', '')}:{library.get('name', '')}"
        version = library.get("version")
        if isinstance(version, dict):
            version = catalog["versions"].get(version.get("ref"))
        if isinstance(version, str):
            if module in result and result[module] != version:
                raise ValueError(f"Conflicting main catalog versions for {module}")
            result[module] = version
    return result


def main_profile(repository):
    catalog_file = repository / "libs.versions.toml"
    catalog = tomllib.loads(catalog_file.read_text(encoding="utf-8"))
    settings = (repository / "settings.gradle.kts").read_text(encoding="utf-8")
    toolchain = re.search(r"\bjvm\s*\{\s*version\s*=\s*(\d+)", settings)
    if not toolchain:
        raise ValueError("Cannot read repository { rachel { jvm { version = ... } } } from root settings")
    properties_file = repository / "gradle.properties"
    properties = properties_file.read_text(encoding="utf-8") if properties_file.is_file() else ""
    shared_properties = {}
    for name in ["kotlin.native.ignoreDisabledTargets"]:
        value = re.search(rf"(?m)^{re.escape(name)}[ \t]*=[ \t]*([^\r\n]*)", properties)
        if value:
            shared_properties[name] = value[1].strip()
    return {
        "versions": catalog["versions"],
        "libraries": library_versions(catalog),
        "java": int(toolchain[1]),
        "catalogSha256": hashlib.sha256(catalog_file.read_bytes()).hexdigest(),
        "gradleProperties": shared_properties,
    }


def align_catalog(path, profile, aliases=None):
    """Override only shared coordinates/explicit version aliases, retaining upstream-only tools."""
    text = path.read_text(encoding="utf-8")
    catalog = tomllib.loads(text)
    overrides = {upstream: profile["versions"][main] for upstream, main in (aliases or {}).items()}
    for library in catalog.get("libraries", {}).values():
        module = library.get("module") or f"{library.get('group', '')}:{library.get('name', '')}"
        reference = library.get("version", {})
        if module in profile["libraries"] and isinstance(reference, dict) and "ref" in reference:
            alias = reference["ref"]
            selected = profile["libraries"][module]
            if alias in overrides and overrides[alias] != selected:
                raise ValueError(f"Conflicting version override for {alias} in {path}")
            overrides[alias] = selected
    for alias, version in overrides.items():
        text, count = re.subn(rf'(?m)^{re.escape(alias)}\s*=\s*"[^"]+"\s*$',
                              f'{alias} = "{version}"', text)
        if count != 1:
            raise ValueError(f"Upstream catalog drift: expected one version alias {alias} in {path}")
    path.write_text(text, encoding="utf-8", newline="\n")


def sync_wrappers(fork, repository):
    """Every fork wrapper uses the root distribution, checksums and launcher files."""
    properties = list(fork.rglob("gradle-wrapper.properties"))
    for file in properties:
        if file.parent.name != "wrapper" or file.parent.parent.name != "gradle":
            continue
        build = file.parents[2]
        for relative in ["gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat"]:
            source = repository / relative
            if source.is_file():
                destination = build / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, destination)


def apply_build_profile(fork, repository, profile):
    sync_wrappers(fork, repository)
    properties_file = fork / "skiko/gradle.properties"
    text = properties_file.read_text(encoding="utf-8")
    for name, value in profile.get("gradleProperties", {}).items():
        line = f"{name}={value}"
        text, count = re.subn(rf"(?m)^{re.escape(name)}[^\r\n]*", line, text)
        if not count:
            text += "\n" + line + "\n"
    properties_file.write_text(text, encoding="utf-8", newline="\n")
    build_src = fork / "skiko/buildSrc/build.gradle.kts"
    with build_src.open("a", encoding="utf-8", newline="\n") as output:
        output.write(f'\n// Rachel: share the main build toolchain.\nkotlin {{ jvmToolchain({profile["java"]}) }}\n')
