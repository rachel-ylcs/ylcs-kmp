#!/usr/bin/env python3
"""Render the README card from repository_statistics.py output and GitHub metadata."""

import argparse
import base64
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from html import escape
import json
import os
from pathlib import Path
from string import Template
from time import sleep
import tomllib
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode, urlsplit
from urllib.request import Request, urlopen

from playwright.sync_api import sync_playwright


ROOT = Path(__file__).resolve().parents[2]
ASSETS = Path(__file__).with_name("readme_status")
REPOSITORY = "rachel-ylcs/ylcs-kmp"
DOCS = "https://rachel-ylcs.github.io/ylcs-kmp"
CHINA_TIME = timezone(timedelta(hours=8))
WORKFLOWS = (
    ("Build", "build", "main"),
    ("Test", "test", "main"),
    ("Release", "release", None),
    ("Docs", "docs", "main"),
    ("Publish", "publish", None),
)
PLATFORMS = ("Android", "iOS", "JVM", "Windows", "Linux", "macOS", "JS", "Wasm", "Server", "Compose", "Kotlin")
MODULE_GROUPS = ("core", "foundation", "compose", "cs", "platform")
LANGUAGE_COLORS = ("#bca0ff", "#f4ad99", "#8fb0ff", "#79dbc9", "#f2ce81", "#ee9acb", "#8e819e")
ICONS = {
    "eye": '<path d="M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6-10-6-10-6Z"/><circle cx="12" cy="12" r="2.5"/>',
    "fork": '<circle cx="6" cy="5" r="2"/><circle cx="18" cy="5" r="2"/><circle cx="12" cy="19" r="2"/><path d="M6 7v2a3 3 0 0 0 3 3h6a3 3 0 0 0 3-3V7m-6 5v5"/>',
    "star": '<path d="m12 3 2.8 5.6 6.2.9-4.5 4.3 1 6.2-5.5-2.9L6.5 20l1-6.2L3 9.5l6.2-.9Z"/>',
    "pr": '<circle cx="6" cy="5" r="2"/><circle cx="6" cy="19" r="2"/><circle cx="18" cy="19" r="2"/><path d="M6 7v10m12 0v-6a4 4 0 0 0-4-4h-2m2-3-3 3 3 3"/>',
    "issue": '<circle cx="12" cy="12" r="9"/><path d="M12 8v4m0 4h.01"/>',
    "commit": '<circle cx="12" cy="12" r="4"/><path d="M2 12h6m8 0h6"/>',
    "tag": '<path d="M3 4v7l9 9 8-8-8-8H3Z"/><circle cx="8" cy="9" r="1"/>',
    "code": '<path d="m7.5 7-5 5 5 5m9-10 5 5-5 5m-3-13-3 16"/>',
    "gallery": '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
    "nav": '<circle cx="5" cy="18" r="2"/><circle cx="19" cy="6" r="2"/><path d="M7 18h8a3 3 0 1 0 0-6H9a3 3 0 1 1 0-6h8"/>',
    "folder": '<path d="M3 19h18V7h-9l-2-3H3v15Z"/>',
    "arrow": '<path d="M7 17 17 7M8 7h9v9"/>',
    "success": '<circle cx="12" cy="12" r="9"/><path d="m8 12 2.7 2.7 5.3-5.4"/>',
    "failure": '<circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/>',
    "neutral": '<circle cx="12" cy="12" r="9"/><path d="M8.5 12h7"/>',
}


def icon(name):
    return f'<svg class="line-icon" viewBox="0 0 24 24" aria-hidden="true">{ICONS[name]}</svg>'


def image_url(data, mime):
    return f"data:{mime};base64,{base64.b64encode(data).decode('ascii')}"


def image_file(path, mime):
    return image_url(path.read_bytes(), mime)


def github(path, **params):
    url = "https://api.github.com" + path
    if params:
        url += "?" + urlencode(params)
    headers = {"Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"}
    if token := os.environ.get("GITHUB_TOKEN"):
        headers["Authorization"] = f"Bearer {token}"

    for attempt in range(3):
        try:
            with urlopen(Request(url, headers=headers), timeout=15) as response:
                return json.load(response)
        except HTTPError as error:
            if error.code not in (429, 500, 502, 503, 504) or attempt == 2:
                raise
        except (URLError, TimeoutError):
            if attempt == 2:
                raise
        sleep(2 ** attempt)


def github_pages(path):
    page = 1
    while True:
        batch = github(path, per_page=100, page=page)
        yield from batch
        if len(batch) < 100:
            break
        page += 1


def open_count(repository, kind):
    result = github("/search/issues", q=f"repo:{repository} is:{kind} is:open", per_page=1)
    if result["incomplete_results"]:
        raise RuntimeError(f"GitHub search returned an incomplete count of open {kind} items")
    return result["total_count"]


def latest_release(repo_path):
    try:
        return github(f"{repo_path}/releases/latest")["published_at"]
    except HTTPError as error:
        if error.code != 404:
            raise
        return None


def workflow_tile(repo_path, workflow):
    name, filename, branch = workflow
    filters = {"status": "completed", "per_page": 1}
    if branch:
        filters["branch"] = branch
    runs = github(f"{repo_path}/actions/workflows/{filename}.yml/runs", **filters)["workflow_runs"]
    conclusion = runs[0]["conclusion"] if runs else None
    style, label = {
        "success": ("success", "Passed"),
        "failure": ("failure", "Failed"),
    }.get(conclusion, ("neutral", conclusion.replace("_", " ").capitalize() if conclusion else "No run"))
    return (
        f'<div class="pipeline"><span class="field">{name}</span>'
        f'<span class="state {style}">{icon(style)}{escape(label)}</span></div>'
    )


def metric(title, glyph, color, count):
    return (
        f'<div><span class="icon {color}">{icon(glyph)}</span>'
        f'<div><span class="metric-name">{title}</span><strong class="number">{count:,}</strong></div></div>'
    )


def date_metric(title, glyph, timestamp):
    value = "—"
    if timestamp:
        local = datetime.fromisoformat(timestamp).astimezone(CHINA_TIME)
        value = f'<time datetime="{local.isoformat()}">{local:%Y-%m-%d %H:%M:%S}</time>'
    return (
        f'<div class="date"><span class="icon fork">{icon(glyph)}</span><div class="date-text">'
        f'<span class="metric-name">{title}</span><strong class="number">{value}</strong></div></div>'
    )


def avatar(person):
    parts = urlsplit(person["avatar_url"])
    if parts.scheme != "https" or parts.netloc != "avatars.githubusercontent.com":
        return None
    url = parts._replace(query="s=96").geturl()
    try:
        with urlopen(url, timeout=8) as response:
            mime = response.headers.get_content_type()
            if mime in ("image/png", "image/jpeg", "image/webp", "image/gif"):
                return image_url(response.read(), mime)
    except (URLError, TimeoutError):
        pass
    return None


def contributor(person):
    name = escape(person["login"])
    commits = person["contributions"]
    unit = "commit" if commits == 1 else "commits"
    source = avatar(person)
    head = f'<img class="avatar" src="{source}" alt="">' if source else f'<span class="avatar">{escape(person["login"][:1].upper())}</span>'
    return (
        f'<div class="person">{head}<div class="person-details">'
        f'<span class="person-name" title="{name}">{name}</span>'
        f'<span class="person-count">{commits:,} {unit}</span></div></div>'
    )


def language_fields(languages):
    entries = sorted(
        ((row["language"], row["lines"]["code"]) for row in languages if row["lines"]["code"]),
        key=lambda entry: entry[1],
        reverse=True,
    )
    total = sum(lines for _, lines in entries)
    if not total:
        return {"gradient": "#493e58 0% 100%", "leading_percent": "—", "leading_language": "No code", "legend": ""}
    if len(entries) > len(LANGUAGE_COLORS):
        split = len(LANGUAGE_COLORS) - 1
        entries = entries[:split] + [("Other", sum(lines for _, lines in entries[split:]))]

    start, stops, legend = 0.0, [], []
    for (name, lines), color in zip(entries, LANGUAGE_COLORS):
        share = lines / total * 100
        end = start + share
        stops.append(f"{color} {start:.8f}% {end:.8f}%")
        label = f"{share:.1f}%" if share >= 0.1 else "&lt;0.1%"
        legend.append(
            f'<li><i class="swatch" style="--swatch:{color}"></i><span>{escape(name)}</span>'
            f'<span class="language-count">{lines:,}</span><strong>{label}</strong></li>'
        )
        start = end
    return {
        "gradient": ", ".join(stops),
        "leading_percent": f"{entries[0][1] / total:.1%}",
        "leading_language": escape(entries[0][0]),
        "legend": "".join(legend),
    }


def format_size(size):
    for unit, divisor in (("GiB", 1024**3), ("MiB", 1024**2), ("KiB", 1024)):
        if size >= divisor:
            return f"{size / divisor:.1f} {unit}"
    return f"{size:,} B"


def tree_children(tree):
    leaves, groups = [], []
    for name, subtree in tree.items():
        if subtree:
            groups.append(
                f'<div class="module-group"><span class="module-group-label">{escape(name)}</span>'
                f'{tree_children(subtree)}</div>'
            )
        else:
            leaves.append(f'<li>{escape(name)}</li>')
    leaf_list = f'<ul class="tree-leaves">{"".join(leaves)}</ul>' if leaves else ""
    return f'<div class="tree-children">{leaf_list}{"".join(groups)}</div>'


def module_rows(modules):
    tree = {}
    for module in modules:
        node = tree
        for part in module["path"].removeprefix("ylcs-module/").split("/"):
            node = node.setdefault(part, {})
    order = dict.fromkeys((*MODULE_GROUPS, *tree))
    rows = []
    for name in order:
        if name not in tree:
            continue
        subtree = tree[name]
        glyph = icon("folder" if subtree else "code")
        label = f'<span class="tree-row-label">{glyph}{escape(name)}</span>'
        children = tree_children(subtree) if subtree else ""
        rows.append(f'<li class="tree-row">{label}{children}</li>')
    return "".join(rows)


def links():
    github_mark = image_file(ASSETS / "github-mark-white.svg", "image/svg+xml")
    guide = image_file(ROOT / "ylcs-docs/docs/assets/favicon.svg", "image/svg+xml")
    yinlin = image_file(ROOT / "ylcs-app/app/global/src/commonMain/composeResources/drawable/img_logo.webp", "image/webp")
    destinations = (
        ("GitHub 仓库", f"https://github.com/{REPOSITORY}", "github", github_mark, ""),
        ("Rachel 框架开发向导", DOCS, "rachel", guide, ""),
        ("Rachel 模块 API 合集", f"{DOCS}/overrides/dokka", "code", None, "#72d8f5"),
        ("UI 画廊", f"{DOCS}/overrides/gallery", "gallery", None, "#ffba83"),
        ("现代导航", f"{DOCS}/overrides/modern-screen", "nav", None, "#80e1b5"),
        ("银临茶舍官网", "https://yinlin.love", "yinlin", yinlin, ""),
        ("银临茶舍网页版", "https://web.yinlin.love", "yinlin", yinlin, ""),
    )
    cards = []
    for title, url, glyph, source, color in destinations:
        if source:
            visual = f'<span class="link-glyph {glyph}" aria-hidden="true"><img src="{source}" alt=""></span>'
        else:
            visual = f'<span class="link-glyph" style="--glyph-color:{color}" aria-hidden="true">{icon(glyph)}</span>'
        cards.append(
            f'<a class="link-card" href="{url}">{visual}<span class="link-title">{title}</span>'
            f'<span class="link-url">{url}</span><span class="link-arrow">{icon("arrow")}</span></a>'
        )
    return "".join(cards)


def repository_fields(repository):
    repo_path = f"/repos/{repository}"
    with ThreadPoolExecutor(max_workers=8) as executor:
        metadata = executor.submit(github, repo_path)
        topics = executor.submit(github, f"{repo_path}/topics")
        people = executor.submit(lambda: list(github_pages(f"{repo_path}/contributors")))
        tags = executor.submit(lambda: sum(1 for _ in github_pages(f"{repo_path}/tags")))
        issues = executor.submit(open_count, repository, "issue")
        prs = executor.submit(open_count, repository, "pr")
        commit = executor.submit(github, f"{repo_path}/commits/main")
        release = executor.submit(latest_release, repo_path)
        workflows = list(executor.map(lambda item: workflow_tile(repo_path, item), WORKFLOWS))
        contributors = sorted(people.result(), key=lambda person: person["contributions"], reverse=True)
        profiles = list(executor.map(contributor, contributors))
        counts = metadata.result()
        activity = (
            metric("Stars", "star", "star", counts["stargazers_count"])
            + metric("Forks", "fork", "fork", counts["forks_count"])
            + metric("Issues", "issue", "star", issues.result())
            + metric("PRs", "pr", "watch", prs.result())
            + metric("Tags", "tag", "fork", tags.result())
            + metric("Watch", "eye", "watch", counts["subscribers_count"])
            + date_metric("Commit", "commit", commit.result()["commit"]["committer"]["date"])
            + date_metric("Release", "tag", release.result())
        )
        names = topics.result()["names"]

    topic_links = "".join(
        f'<li><a class="topic" href="https://github.com/topics/{quote(name, safe="")}">{escape(name)}</a></li>'
        for name in names
    )
    placeholders = '<div aria-hidden="true"></div>' * max(0, 15 - len(profiles))
    return {
        "workflows": "".join(workflows),
        "activity": activity,
        "contributors": "".join(profiles) + placeholders,
        "contributor_count": len(contributors),
        "topics": topic_links,
    }


def render_card(stats, repository, output, html_output=None, browser_path=None):
    summary = stats["summary"]
    catalog = tomllib.loads((ROOT / "libs.versions.toml").read_text(encoding="utf-8"))
    fields = {
        **repository_fields(repository),
        **language_fields(stats["languages"]),
        "banner": image_file(ROOT / "ylcs-docs/docs/assets/title.png", "image/png"),
        "platforms": image_file(ASSETS / "platforms.webp", "image/webp"),
        "platform_items": "".join(
            f'<li class="platform-logo" style="--platform:{index}" aria-label="{name}" title="{name}"></li>'
            for index, name in enumerate(PLATFORMS)
        ),
        "version": escape(catalog["versions"]["rachel"]),
        "files": f"{summary['files']:,}",
        "code_lines": f"{summary['code_lines']:,}",
        "modules": f"{summary['gradle_modules']:,}",
        "size": format_size(summary["bytes"]),
        "links": links(),
        "folder_icon": icon("folder"),
        "module_tree": module_rows(stats["kmp"]["modules"]),
    }
    html = Template((ASSETS / "card.html").read_text(encoding="utf-8")).substitute(fields)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(executable_path=browser_path)
        page = browser.new_page(viewport={"width": 960, "height": 1200}, device_scale_factor=2)
        page.set_content(html, wait_until="load")
        page.evaluate("document.fonts.ready")
        page.evaluate("(src) => { const img = new Image(); img.src = src; return img.decode(); }", fields["platforms"])
        if not page.locator("img").evaluate_all("(images) => images.every(image => image.complete && image.naturalWidth > 0)"):
            raise RuntimeError("The card contains a broken image")
        png = page.locator(".card").screenshot(animations="disabled")
        browser.close()

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(png)
    if html_output:
        html_output.parent.mkdir(parents=True, exist_ok=True)
        html_output.write_text(html, encoding="utf-8")
    print(f"Generated {output}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stats", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY", REPOSITORY))
    parser.add_argument("--html", type=Path, help="Also save the rendered HTML for local preview")
    parser.add_argument("--browser", help="Optional path to a local Chromium-based browser")
    args = parser.parse_args()
    stats = json.loads(args.stats.read_text(encoding="utf-8"))
    render_card(stats, args.repository, args.output, args.html, args.browser)


if __name__ == "__main__":
    main()
