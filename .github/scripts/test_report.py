"""Collect Gradle JUnit results and publish a summary plus a standalone HTML report.

Uses only Python's standard library. Exit codes from Gradle remain authoritative;
missing or unreadable reports are reported separately from test failures.
"""

import argparse
from collections import defaultdict
from datetime import datetime, timezone
import html
import json
import os
from pathlib import Path
import re
import sys
from urllib.parse import quote
import xml.etree.ElementTree as ET


PLATFORMS = {
    "androidTest": ("Android", "test-android"),
    "iosTest": ("iOS", "test-ios"),
    "desktopTest": ("Desktop · Windows", "test-desktop"),
    "jsTest": ("Web JS", "test-js"),
    "wasmJsTest": ("Web Wasm", "test-wasmJs"),
    "windowsTest": ("Native · Windows", "test-windows"),
    "linuxTest": ("Native · Linux", "test-linux"),
    "macosTest": ("Native · macOS", "test-macos"),
}
STATUS = {
    "passed": "通过", "failed": "测试失败", "error": "执行或报告失败",
    "empty": "无测试结果", "skipped": "未触发", "cancelled": "已取消",
    "missing": "缺少报告",
}
SUMMARY_LIMIT = 900_000  # GitHub accepts at most 1 MiB per step summary.
LOG_LIMIT = 4 * 1024 * 1024
XML_LIMIT = 32 * 1024 * 1024
TEST_TASKS = {
    "androidTest": {"testAndroidHostTest"}, "iosTest": {"iosSimulatorArm64Test"},
    "desktopTest": {"desktopTest", "test"}, "jsTest": {"jsBrowserTest", "jsNodeTest"},
    "wasmJsTest": {"wasmJsBrowserTest", "wasmJsNodeTest"},
    "windowsTest": {"windowsTest"}, "linuxTest": {"linuxTest"}, "macosTest": {"macosTest"},
}


def totals(cases):
    result = {"total": len(cases), "passed": 0, "failed": 0, "skipped": 0, "seconds": 0.0}
    for case in cases:
        result[case["status"]] += 1
        result["seconds"] += case["seconds"]
    return result


def metadata():
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    sha = os.environ.get("GITHUB_SHA", "")
    server = os.environ.get("GITHUB_SERVER_URL", "https://github.com").rstrip("/")
    run = os.environ.get("GITHUB_RUN_ID", "")
    return {
        "repository": repository, "sha": sha, "server": server,
        "branch": os.environ.get("GITHUB_HEAD_REF") or os.environ.get("GITHUB_REF_NAME", ""),
        "run_url": f"{server}/{repository}/actions/runs/{run}" if repository and run else "",
        "created": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }


class Sources:
    def __init__(self, root, meta):
        self.root = Path(root).resolve()
        self.meta = meta
        self.files = defaultdict(list)
        self.texts = {}
        for directory, dirs, files in os.walk(self.root / "ylcs-module"):
            dirs[:] = [d for d in dirs if d not in {
                "build", ".gradle", ".git", "cpp", "node_modules", "resources", "composeResources",
            }]
            for name in files:
                path = Path(directory) / name
                if path.suffix in {".kt", ".java"} and "src" in path.parts:
                    self.files[name].append(path)

    def text(self, path):
        if path not in self.texts:
            self.texts[path] = path.read_text(encoding="utf-8", errors="replace")
        return self.texts[path]

    def choose(self, filename, module, classname):
        candidates = []
        for path in self.files.get(filename, []):
            relative = path.relative_to(self.root).as_posix()
            package = re.search(r"^\s*package\s+([\w.]+)", self.text(path), re.MULTILINE)
            package_matches = bool(package and ("." + package[1] + ".") in ("." + classname + "."))
            score = 2 * relative.startswith(module + "/") + package_matches
            candidates.append((score, relative, path))
        candidates.sort(key=lambda item: (-item[0], item[1]))
        if not candidates or (len(candidates) > 1 and candidates[0][0] == candidates[1][0]):
            return None
        return candidates[0][2]

    def location(self, path, line, kind):
        relative = path.relative_to(self.root).as_posix()
        if not 1 <= line <= len(self.text(path).splitlines()):
            return None
        meta = self.meta
        url = ""
        if meta["repository"] and meta["sha"]:
            url = f'{meta["server"]}/{meta["repository"]}/blob/{quote(meta["sha"], safe="")}/{quote(relative, safe="/")}#L{line}'
        return {"file": relative, "line": line, "kind": kind, "url": url}

    def locate(self, module, classname, test_name, stack):
        locations = []
        for match in re.finditer(r"([^\s/\\():]+\.(?:kt|java)):(\d+)", stack):
            path = self.choose(match[1], module, classname)
            location = self.location(path, int(match[2]), "exception") if path else None
            if location and location not in locations:
                locations.append(location)
        # Some Native/JS/Wasm reports have no Kotlin line information. Link the
        # test declaration explicitly as an entry point, never as a failing line.
        if not locations:
            class_name = classname.split(".")[-1].split("$")[0]
            path = self.choose(class_name + ".kt", module, classname)
            method = re.split(r"\[|\(", test_name)[0]
            if path and re.fullmatch(r"\w+", method):
                declarations = list(re.finditer(r"\bfun\s+" + re.escape(method) + r"\s*\(", self.text(path)))
                if len(declarations) == 1:
                    line = self.text(path)[:declarations[0].start()].count("\n") + 1
                    locations.append(self.location(path, line, "entry"))
        return [location for location in locations if location]


def collect(root, source_root, platform, outcome, output, meta):
    root, output = Path(root), Path(output)
    sources = Sources(source_root, meta)
    cases, diagnostics, report_count = [], [], 0
    if outcome == "skipped":
        outcome = "unknown"
        diagnostics.append("测试命令未执行；请检查环境准备步骤。")
    reports = sorted((root / "ylcs-module").glob("**/build/test-results/**/TEST-*.xml"))
    for report in reports:
        relative = report.relative_to(root).as_posix()
        module, suffix = relative.split("/build/test-results/", 1)
        task = suffix.split("/", 1)[0]
        if task not in TEST_TASKS[platform]:
            continue
        report_count += 1
        try:
            if report.stat().st_size > XML_LIMIT:
                raise ValueError("XML 超过 32 MiB，未读取")
            suite = ET.parse(report).getroot()
            for case in suite.iter("testcase"):
                issues = [*case.findall("failure"), *case.findall("error")]
                skipped = case.find("skipped")
                stack = "\n\n".join(issue.text or issue.get("message", "") for issue in issues)
                name, classname = case.get("name", "未命名用例"), case.get("classname", suite.get("name", ""))
                try:
                    seconds = max(0.0, float(case.get("time", "0")))
                    if not seconds < float("inf"):
                        seconds = 0.0
                except ValueError:
                    seconds = 0.0
                cases.append({
                    "module": module, "task": task, "class": classname, "name": name,
                    "status": "failed" if issues else "skipped" if skipped is not None else "passed",
                    "seconds": seconds, "message": "\n".join(issue.get("message", "") for issue in issues),
                    "stack": stack, "type": ", ".join(issue.get("type", "") for issue in issues),
                    "output": "\n".join(case.findtext(tag, "") for tag in ("system-out", "system-err")).strip(),
                    "locations": sources.locate(module, classname, name, stack) if issues else [],
                })
        except (ET.ParseError, OSError, ValueError) as error:
            diagnostics.append(f"{relative}: {error}")
    log_path = output / f"gradle-{platform}.log"
    log = ""
    if log_path.exists():
        with log_path.open("rb") as stream:
            size = log_path.stat().st_size
            prefix = stream.read(2)
            encoding = {b"\xff\xfe": "utf-16-le", b"\xfe\xff": "utf-16-be"}.get(prefix, "utf-8-sig")
            stream.seek(max(2 if encoding.startswith("utf-16") else 0, size - LOG_LIMIT))
            log = stream.read().decode(encoding, errors="replace")
        if size > LOG_LIMIT:
            log = "[仅嵌入日志末尾 4 MiB；完整日志保存在该平台 Artifact 中]\n" + log
    result = {
        "schema": 1, "platform": platform, "label": PLATFORMS[platform][0],
        "outcome": outcome, "meta": meta, "cases": cases, "totals": totals(cases),
        "reports": report_count,
        "diagnostics": diagnostics, "log": log,
    }
    output.mkdir(parents=True, exist_ok=True)
    (output / f"results-{platform}.json").write_text(json.dumps(result, ensure_ascii=False), encoding="utf-8")
    return result


def status(platform):
    if platform["outcome"] in {"skipped", "cancelled"}:
        return platform["outcome"]
    if platform["diagnostics"] or platform["outcome"] in {"failure", "unknown"} and not platform["totals"]["failed"]:
        return "error"
    if platform["totals"]["failed"]:
        return "failed"
    if not platform["reports"] or not platform["totals"]["total"]:
        return "empty"
    return "passed"


def merge(directory, jobs, meta):
    results, diagnostics = {}, []
    for path in sorted(Path(directory).rglob("results-*.json")):
        try:
            item = json.loads(path.read_text(encoding="utf-8"))
            platform = item["platform"]
            if item["schema"] != 1 or platform not in PLATFORMS:
                raise ValueError("未知报告格式或平台")
            if platform in results:
                raise ValueError(f"平台 {platform} 有重复报告，未重复计数")
            if item["meta"]["sha"] != meta["sha"] or item["meta"]["repository"] != meta["repository"]:
                raise ValueError("报告的提交或仓库与当前工作流不符")
            item["totals"] = totals(item["cases"])
            results[platform] = item
        except (KeyError, TypeError, ValueError, OSError) as error:
            diagnostics.append(f"{path.name}: {error}")
    platforms = []
    for key, (label, job) in PLATFORMS.items():
        job_result = jobs.get(job, {}).get("result")
        item = results.get(key)
        if item is None:
            item = {
                "platform": key, "label": label, "cases": [], "reports": 0,
                "totals": totals([]), "diagnostics": [], "log": "", "outcome": job_result or "unknown",
            }
            item["status"] = job_result if job_result in {"skipped", "cancelled"} else "missing"
            if item["status"] == "missing":
                item["diagnostics"].append("未收到该平台报告；请查看对应 Job 的编译、环境或报告上传日志。")
        else:
            if job_result in {"failure", "cancelled"}:
                item["outcome"] = job_result
            item["status"] = status(item)
        platforms.append(item)
    return {
        "meta": meta, "platforms": platforms, "diagnostics": diagnostics,
        "totals": totals([case for item in platforms for case in item["cases"]]),
    }


def markdown(value):
    return html.escape(str(value), quote=False).replace("|", "&#124;").replace("`", "&#96;").replace("\n", " ").replace("\r", "")


def source_link(location):
    label = f'{location["file"]}:{location["line"]}'
    text = f"[{markdown(label)}]({location['url']})" if location["url"] else markdown(label)
    return text + ("（测试入口，非异常行）" if location["kind"] == "entry" else "")


def summary(report):
    count = report["totals"]
    lines = [
        "## Rachel 单元测试", "",
        f'执行 **{count["total"]}** 次 · 通过 **{count["passed"]}** · 失败 **{count["failed"]}** · 跳过 **{count["skipped"]}**',
        "", "按平台执行次数统计，commonTest 会在不同平台重复执行。", "",
        "| 平台 | 状态 | 总数 | 通过 | 失败 | 跳过 |", "| --- | --- | ---: | ---: | ---: | ---: |",
    ]
    for item in report["platforms"]:
        t = item["totals"]
        lines.append(f'| {item["label"]} | {STATUS[item["status"]]} | {t["total"]} | {t["passed"]} | {t["failed"]} | {t["skipped"]} |')
    lines.extend(["", "### 模块统计", "", "| 平台 | 模块 | 总数 | 通过 | 失败 | 跳过 |", "| --- | --- | ---: | ---: | ---: | ---: |"])
    failures = []
    for item in report["platforms"]:
        modules = defaultdict(list)
        for case in item["cases"]:
            modules[case["module"]].append(case)
            if case["status"] == "failed":
                failures.append((item, case))
        for module, cases in sorted(modules.items()):
            t = totals(cases)
            lines.append(f'| {item["label"]} | {markdown(module)} | {t["total"]} | {t["passed"]} | {t["failed"]} | {t["skipped"]} |')
    notes = report["diagnostics"] + [f'{p["label"]}: {d}' for p in report["platforms"] for d in p["diagnostics"]]
    if notes:
        lines.extend(["", "### 执行与报告问题", "", *["- " + markdown(note) for note in notes]])
    lines.extend(["", "### 失败用例", ""])
    if not failures:
        lines.append("没有已记录的失败用例。未产生测试结果的平台请查看上方状态。")
    for item, case in failures:
        where = source_link(case["locations"][0]) if case["locations"] else "报告中无可用 Kotlin/Java 源码位置"
        lines.append(f'- **{item["label"]}** · {markdown(case["module"])} · {markdown(case["class"])}.{markdown(case["name"])} — {where}')
    lines.extend(["", "完整可搜索报告及 Gradle 日志请下载 **unit-test-report** Artifact 中的单个 **index.html**。", ""])
    text = "\n".join(lines)
    if len(text.encode("utf-8")) > SUMMARY_LIMIT:
        parts, size = [], 0
        for line in lines:
            size += len((line + "\n").encode("utf-8"))
            if size > SUMMARY_LIMIT - 1024:
                break
            parts.append(line)
        return "\n".join(parts) + "\n\n摘要达到大小限制；全部结果请下载 unit-test-report 中的 index.html。\n"
    for item, case in failures:
        stack = case["stack"] or case["message"]
        stack = html.escape(stack, quote=False)
        detail = f'\n<details><summary>{markdown(item["label"] + " · " + case["class"] + "." + case["name"])}</summary>\n\n<pre>{stack}</pre>\n\n</details>\n'
        if len((text + detail).encode("utf-8")) > SUMMARY_LIMIT:
            text += "\n摘要达到大小限制；其余完整异常保留在单文件 HTML 报告中。\n"
            break
        text += detail
    return text


def command_escape(text, property_value=False):
    value = str(text).replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    return value.replace(":", "%3A").replace(",", "%2C") if property_value else value


def annotations(platform, limit=10):
    failures = [case for case in platform["cases"] if case["status"] == "failed"]
    for case in failures[:limit]:
        location = case["locations"][0] if case["locations"] else None
        title = f'{platform["label"]}: {case["class"]}.{case["name"]}'
        if location and location["kind"] == "entry":
            title += "（测试入口，非异常行）"
        properties = "title=" + command_escape(title, True)
        if location:
            properties += f',file={command_escape(location["file"], True)},line={location["line"]}'
        message = (case["stack"] or case["message"])[:6000]
        yield f"::error {properties}::{command_escape(message)}"
    if len(failures) > limit:
        yield f"其余 {len(failures) - limit} 个失败用例请查看测试摘要和单文件 HTML 报告。"


def publish(report, output, write_html=False):
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    text = summary(report)
    (output / "summary.md").write_text(text, encoding="utf-8")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as stream:
            stream.write(text)
    if write_html:
        data = json.dumps(report, ensure_ascii=False).replace("&", "\\u0026").replace("<", "\\u003c").replace(">", "\\u003e")
        template = Path(__file__).with_name("test-report.html").read_text(encoding="utf-8")
        (output / "index.html").write_text(template.replace("__REPORT_DATA__", data), encoding="utf-8")
        (output / "results.json").write_text(json.dumps(report, ensure_ascii=False), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("collect", "merge"))
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--source-root", type=Path)
    parser.add_argument("--output", type=Path, default=Path("build/test-report"))
    parser.add_argument("--input", type=Path, default=Path("build/test-input"))
    parser.add_argument("--platform", choices=PLATFORMS, default=os.environ.get("TEST_PLATFORM"))
    parser.add_argument("--outcome", default=os.environ.get("TEST_OUTCOME") or "unknown")
    args = parser.parse_args()
    meta = metadata()
    if args.mode == "collect":
        if args.platform not in PLATFORMS:
            parser.error("collect 需要 --platform 或 TEST_PLATFORM")
        item = collect(args.root, args.source_root or args.root, args.platform, args.outcome, args.output, meta)
        item["status"] = status(item)
        report = {"meta": meta, "platforms": [item], "totals": item["totals"], "diagnostics": []}
        publish(report, args.output)
        for annotation in annotations(item):
            print(annotation)
        print(f'{item["label"]}: {item["totals"]}; {STATUS[item["status"]]}')
        return 1 if item["diagnostics"] else 0
    jobs = json.loads(os.environ.get("TEST_JOB_RESULTS", "{}"))
    report = merge(args.input, jobs, meta)
    if os.environ.get("TEST_DOWNLOAD_OUTCOME") == "failure":
        report["diagnostics"].append("下载平台报告失败；下方统计可能不完整，请检查 Download Test Results。")
    publish(report, args.output, write_html=True)
    print(f'Combined report: {args.output / "index.html"}; {report["totals"]}')
    return 1 if report["diagnostics"] or any(p["status"] in {"missing", "error"} for p in report["platforms"]) else 0


if __name__ == "__main__":
    sys.exit(main())
