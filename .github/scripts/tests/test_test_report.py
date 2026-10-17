"""Regression tests for report accounting, source links and failure-safe output."""

import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch


SCRIPT = Path(__file__).resolve().parents[1] / "test_report.py"
SPEC = importlib.util.spec_from_file_location("test_report", SCRIPT)
report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(report)


class TestReport(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.module = "ylcs-module/foundation/example"
        self.output = self.root / "out"
        self.meta = {"repository": "example/project", "sha": "a" * 40, "server": "https://github.com",
                     "branch": "main", "run_url": "https://github.com/example/project/actions/runs/1", "created": "2026-10-17T00:00:00Z"}
        self.source = self.root / self.module / "src/commonTest/kotlin/demo/SampleTest.kt"
        self.source.parent.mkdir(parents=True)
        self.source.write_text("package demo\n\nclass SampleTest {\n    fun fails() {\n        error(\"broken\")\n    }\n}\n", encoding="utf-8")

    def xml(self, text, name="TEST-demo.SampleTest.xml"):
        path = self.root / self.module / "build/test-results/desktopTest" / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path

    def collect(self, outcome="failure"):
        return report.collect(self.root, self.root, "desktopTest", outcome, self.output, self.meta)

    def failed(self):
        self.xml('''<testsuite name="demo.SampleTest" tests="1" failures="1">
          <testcase classname="demo.SampleTest" name="fails[desktop]" time="0.25">
            <failure type="AssertionError" message="expected A, got B">AssertionError: expected A, got B
    at demo.SampleTest.fails(SampleTest.kt:5)</failure>
          </testcase></testsuite>''')
        item = self.collect()
        item["status"] = report.status(item)
        return item

    def combined(self, item):
        return {"meta": self.meta, "platforms": [item], "totals": item["totals"], "diagnostics": []}

    def test_counts_failures_errors_skips_and_case_output(self):
        self.xml('''<testsuite tests="4" failures="1" errors="1" skipped="1">
          <testcase name="ok" classname="demo.SampleTest" time="0.1"><system-out>hello</system-out></testcase>
          <testcase name="skip" classname="demo.SampleTest"><skipped/></testcase>
          <testcase name="fail" classname="demo.SampleTest"><failure type="AssertionError">A &lt; B</failure></testcase>
          <testcase name="error" classname="demo.SampleTest" time="NaN"><error type="RuntimeException">oops</error></testcase>
          </testsuite>''')
        item = self.collect()
        self.assertEqual({k: item["totals"][k] for k in ("total", "passed", "failed", "skipped")},
                         {"total": 4, "passed": 1, "failed": 2, "skipped": 1})
        self.assertEqual(item["cases"][0]["output"], "hello")
        self.assertEqual(item["cases"][2]["stack"], "A < B")
        self.assertEqual(item["cases"][3]["seconds"], 0)

    def test_reports_from_other_platforms_are_not_counted(self):
        self.failed()
        old_report = self.root / self.module / "build/test-results/windowsTest/TEST-old.xml"
        old_report.parent.mkdir(parents=True)
        old_report.write_text('<testsuite><testcase name="old"/></testsuite>', encoding="utf-8")
        self.assertEqual(self.collect()["totals"]["total"], 1)

    def test_jvm_failure_links_to_exact_commit_and_actual_line(self):
        case = self.failed()["cases"][0]
        location = case["locations"][0]
        self.assertEqual(location["kind"], "exception")
        self.assertEqual(location["line"], 5)
        self.assertIn("/blob/" + "a" * 40 + "/", location["url"])
        self.assertTrue(location["url"].endswith("SampleTest.kt#L5"))

    def test_unknown_native_source_links_only_to_test_entry(self):
        self.xml('''<testsuite><testcase classname="desktopTest.demo.SampleTest" name="fails[linux]">
          <failure>at demo.SampleTest#fails(Unknown Source)</failure></testcase></testsuite>''')
        location = self.collect()["cases"][0]["locations"][0]
        self.assertEqual((location["kind"], location["line"]), ("entry", 4))
        self.assertIn("非异常行", report.source_link(location))

    def test_missing_or_out_of_range_source_does_not_invent_failure_line(self):
        self.xml('''<testsuite><testcase classname="demo.MissingTest" name="fails">
          <failure>at demo.MissingTest.fails(SampleTest.kt:9999)</failure></testcase></testsuite>''')
        self.assertEqual(self.collect()["cases"][0]["locations"], [])

    def test_ambiguous_sources_are_not_guessed(self):
        duplicate = self.root / self.module / "src/desktopTest/kotlin/demo/SampleTest.kt"
        duplicate.parent.mkdir(parents=True)
        duplicate.write_text(self.source.read_text(encoding="utf-8"), encoding="utf-8")
        self.assertEqual(self.failed()["cases"][0]["locations"], [])

    def test_compilation_failure_with_no_xml_is_not_a_passing_zero(self):
        item = self.collect()
        self.assertEqual(report.status(item), "error")
        self.assertEqual(item["totals"]["total"], 0)
        item["outcome"] = "success"
        self.assertEqual(report.status(item), "empty")

    def test_skipped_command_in_a_started_job_is_an_execution_problem(self):
        item = self.collect("skipped")
        self.assertEqual(report.status(item), "error")
        self.assertTrue(item["diagnostics"])

    def test_windows_utf16_log_is_readable(self):
        self.output.mkdir()
        (self.output / "gradle-desktopTest.log").write_text("编译错误：测试环境失败", encoding="utf-16")
        self.assertEqual(self.collect()["log"], "编译错误：测试环境失败")

    def test_malformed_xml_preserves_other_results_and_reports_error(self):
        self.failed()
        self.xml("<testsuite><broken>", "TEST-broken.xml")
        item = self.collect()
        self.assertEqual(item["totals"]["failed"], 1)
        self.assertEqual(len(item["diagnostics"]), 1)
        self.assertEqual(report.status(item), "error")

    def test_merge_distinguishes_detect_skips_and_missing_results(self):
        self.failed()
        jobs = {job: {"result": "skipped"} for _, job in report.PLATFORMS.values()}
        jobs["test-desktop"] = {"result": "failure"}
        jobs["test-ios"] = {"result": "failure"}
        merged = report.merge(self.output, jobs, self.meta)
        platforms = {p["platform"]: p for p in merged["platforms"]}
        self.assertEqual(platforms["desktopTest"]["status"], "failed")
        self.assertEqual(platforms["androidTest"]["status"], "skipped")
        self.assertEqual(platforms["iosTest"]["status"], "missing")
        self.assertEqual(merged["totals"]["total"], 1)

    def test_duplicate_or_foreign_commit_reports_are_not_double_counted(self):
        self.failed()
        path = self.output / "results-desktopTest.json"
        (self.output / "nested").mkdir()
        (self.output / "nested/results-copy.json").write_bytes(path.read_bytes())
        merged = report.merge(self.output, {}, self.meta)
        self.assertEqual(merged["totals"]["total"], 1)
        self.assertEqual(len(merged["diagnostics"]), 1)
        other_meta = {**self.meta, "sha": "b" * 40}
        merged = report.merge(self.output, {}, other_meta)
        self.assertEqual(merged["totals"]["total"], 0)
        self.assertEqual(len(merged["diagnostics"]), 2)

    def test_annotations_escape_commands_and_limit_only_annotations(self):
        item = self.failed()
        item["cases"][0]["stack"] = "broken%\n::warning::injected"
        item["cases"] *= 12
        messages = list(report.annotations(item))
        self.assertEqual(len(messages), 11)
        self.assertIn("%25%0A::warning::injected", messages[0])
        self.assertNotIn("\n", messages[0])
        self.assertIn("file=ylcs-module/", messages[0])
        self.assertEqual(report.summary(self.combined(item)).count("<details>"), 12)

    def test_html_embeds_all_results_without_executable_test_text(self):
        item = self.failed()
        dangerous = '</script><script>window.injected=true</script>&"'
        item["cases"][0]["stack"] = dangerous
        report.publish(self.combined(item), self.output, write_html=True)
        text = (self.output / "index.html").read_text(encoding="utf-8")
        self.assertNotIn(dangerous, text)
        data = text.split('<script id="report-data" type="application/json">', 1)[1].split("</script>", 1)[0]
        self.assertEqual(json.loads(data)["platforms"][0]["cases"][0]["stack"], dangerous)
        self.assertIn("&lt;/script&gt;", (self.output / "summary.md").read_text(encoding="utf-8"))

    def test_summary_is_bounded_and_complete_data_is_retained(self):
        item = self.failed()
        item["cases"][0]["stack"] = "错误" * 400_000
        text = report.summary(self.combined(item))
        self.assertLess(len(text.encode("utf-8")), 1024 * 1024)
        self.assertIn("大小限制", text)
        self.assertEqual(len(item["cases"][0]["stack"]), 800_000)

    def test_cli_writes_failure_report_even_when_xml_is_invalid(self):
        self.xml("not xml")
        with patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": str(self.root / "github-summary.md")}), \
                patch.object(report.sys, "argv", [str(SCRIPT), "collect", "--root", str(self.root),
                                                 "--output", str(self.output), "--platform", "desktopTest", "--outcome", "failure"]), \
                contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(report.main(), 1)
        self.assertTrue((self.output / "results-desktopTest.json").exists())
        self.assertIn("执行与报告问题", (self.root / "github-summary.md").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
