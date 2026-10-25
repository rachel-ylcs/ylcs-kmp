"""Native UI publication verification must reject missing evidence and unsafe artifacts."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_compose_ui import TESTS, merge, verify_files, verify_tests


class ComposePublicationsTest(unittest.TestCase):
    def test_missing_host_does_not_create_an_assembled_repository(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "assembled"
            with self.assertRaisesRegex(ValueError, "All three host"):
                merge({"mingw_x64": Path(temporary)}, output, {})
            self.assertFalse(output.exists())

    def test_publication_file_cannot_escape_its_module(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary) / "module"
            directory.mkdir()
            metadata = {"variants": [{"files": [{"url": "../outside.klib", "size": 0}]}]}
            with self.assertRaisesRegex(ValueError, "escapes its module"):
                verify_files(metadata, directory)

    def test_missing_or_skipped_native_tests_never_count_as_success(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            with self.assertRaisesRegex(ValueError, "No Native UI JUnit"):
                verify_tests(root)
            reports = root / "ui-util/test-results/windowsTest"
            reports.mkdir(parents=True)
            for name, count in TESTS.items():
                skipped = int(name == "NativeRuntimeTest")
                (reports / f"TEST-{name}.xml").write_text(f'<testsuite name="fixture.{name}" tests="{count}" failures="0" errors="0" skipped="{skipped}"/>')
            with self.assertRaisesRegex(ValueError, "Failed or skipped"):
                verify_tests(root)
