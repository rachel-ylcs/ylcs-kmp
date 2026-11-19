from __future__ import annotations

import re
import shutil
import tomllib
from pathlib import Path

from mkdocs.config.defaults import MkDocsConfig
from mkdocs.structure.files import File, Files


ROOT = next(parent for parent in Path(__file__).resolve().parents if (parent / "libs.versions.toml").is_file())
DOCS_ASSETS = Path(__file__).resolve().parent / "docs" / "assets"
SHARED_ASSETS = {
    "assets/stylesheets/rachel.css": DOCS_ASSETS / "stylesheets" / "rachel.css",
    "assets/javascripts/rachel.js": DOCS_ASSETS / "javascripts" / "rachel.js",
}


def read_rachel_version() -> str:
    with (ROOT / "libs.versions.toml").open("rb") as stream:
        versions = tomllib.load(stream).get("versions", {})
    version = versions.get("rachel")
    if not version:
        raise ValueError("libs.versions.toml 缺少 versions.rachel")
    return str(version)


def on_config(config: MkDocsConfig) -> MkDocsConfig:
    config.extra["rachel_version"] = read_rachel_version()
    return config


def on_files(files: Files, config: MkDocsConfig) -> Files:
    if config.extra.get("shared_theme_assets"):
        for target, source in SHARED_ASSETS.items():
            if not any(file.src_uri == target for file in files):
                files.append(File.generated(config, target, abs_src_path=str(source)))
    return files


def on_page_markdown(markdown: str, *, config: MkDocsConfig, **_) -> str:
    version = str(config.extra["rachel_version"])
    return re.sub(r"\{\{\s*rachel_version\s*\}\}", version, markdown)


def on_post_build(config: MkDocsConfig) -> None:
    if config.extra.get("site_kind") == "landing":
        source = Path(__file__).resolve().parent / "landing" / "404.html"
        shutil.copyfile(source, Path(config.site_dir) / "404.html")