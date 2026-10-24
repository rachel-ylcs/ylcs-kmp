"""Exercise artifact aggregation and archive boundaries without a Native toolchain."""
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from merge_repositories import TARGETS, merge
from verify_distributions import extract


class PublicationToolsTest(unittest.TestCase):
    def test_all_hosts_keep_their_binaries_and_only_the_apple_root(self):
        with tempfile.TemporaryDirectory() as work:
            work = Path(work)
            inputs = {}
            for target, suffix in TARGETS.items():
                repository = work / target
                inputs[target] = repository
                for module in ["skiko", f"skiko-{suffix}"]:
                    directory = repository / "test/group" / module / "1.0"
                    directory.mkdir(parents=True)
                    (directory / "artifact").write_text(target)
            output = merge(inputs, work / "merged", {"group": "test.group", "portVersion": "1.0"})
            self.assertEqual((output / "test/group/skiko/1.0/artifact").read_text(), "macos_arm64")
            for target, suffix in TARGETS.items():
                self.assertEqual((output / f"test/group/skiko-{suffix}/1.0/artifact").read_text(), target)
            with self.assertRaisesRegex(ValueError, "non-empty"):
                merge(inputs, output, {"group": "test.group", "portVersion": "1.0"})

    def test_missing_host_fails_before_creating_a_repository(self):
        with tempfile.TemporaryDirectory() as work:
            output = Path(work) / "merged"
            with self.assertRaisesRegex(ValueError, "Exactly"):
                merge({"mingw_x64": Path(work) / "windows"}, output, {})
            self.assertFalse(output.exists())

    def test_archive_cannot_write_outside_its_destination(self):
        with tempfile.TemporaryDirectory() as work:
            archive = Path(work) / "unsafe.zip"
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("../outside", "unexpected")
            with self.assertRaisesRegex(ValueError, "Unsafe"):
                extract(archive, Path(work) / "unpacked")
            self.assertFalse((Path(work) / "outside").exists())

    def test_executable_mode_survives_zip_extraction(self):
        with tempfile.TemporaryDirectory() as work:
            archive = Path(work) / "sample.zip"
            entry = zipfile.ZipInfo("sample.kexe")
            entry.external_attr = 0o100755 << 16
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr(entry, b"native fixture")
            extract(archive, Path(work) / "unpacked")
            executable = Path(work) / "unpacked/sample.kexe"
            self.assertEqual(executable.read_bytes(), b"native fixture")
            if sys.platform != "win32":
                self.assertTrue(executable.stat().st_mode & 0o111)


if __name__ == "__main__":
    unittest.main()
