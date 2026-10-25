"""Exercise artifact aggregation and archive boundaries without a Native toolchain."""
from pathlib import Path
import json
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from merge_repositories import ARTIFACTS, TARGETS, merge
from skiko_adapt import prepare_notices
from verify_distributions import extract


class PublicationToolsTest(unittest.TestCase):
    def test_source_notices_follow_locked_inputs_without_a_manual_license_directory(self):
        with tempfile.TemporaryDirectory() as work:
            work = Path(work)
            upstream = work / "upstream-checkout"
            upstream.mkdir()
            (upstream / "skiko").mkdir()
            (upstream / "LICENSE").write_bytes(b"original upstream license\n")
            (upstream / "NOTICE").write_bytes(b"original upstream notice\n")
            own = work / "native-compose"
            (own / "upstream").mkdir(parents=True)
            origins = {"files": {"maintained.kt": "upstream.kt"}}
            (own / "upstream/adaptation-origins.json").write_text(json.dumps(origins))
            sdl = {"version": "future-sdl", "source": {"url": "https://example.invalid/sdl.zip", "sha256": "audited-sdl"}}
            (own / "upstream/sdl.json").write_text(json.dumps(sdl))
            lock = {"skia": "future-skia", "sources": {"skiko-upstream": {"repository": "JetBrains/skiko", "commit": "audited-commit"}}}
            prepare_notices(upstream, lock, own)
            notices = upstream / "skiko/rachel-notices"
            self.assertFalse((own / "licenses").exists())
            self.assertEqual((notices / "Apache-2.0.txt").read_bytes(), (upstream / "LICENSE").read_bytes())
            self.assertEqual((notices / "NOTICE").read_bytes(), (upstream / "NOTICE").read_bytes())
            self.assertEqual(json.loads((notices / "upstream-lock.json").read_text()), lock)
            text = (notices / "THIRD_PARTY_NOTICES.md").read_text()
            for identity in ["audited-commit", "future-skia", "future-sdl", "audited-sdl"]:
                self.assertIn(identity, text)

    def test_all_hosts_keep_their_binaries_and_only_the_apple_root(self):
        with tempfile.TemporaryDirectory() as work:
            work = Path(work)
            inputs = {}
            for target, suffix in TARGETS.items():
                repository = work / target
                inputs[target] = repository
                for module in [name for artifact in ARTIFACTS for name in [artifact, f"{artifact}-{suffix}"]]:
                    directory = repository / "test/group" / module / "1.0"
                    directory.mkdir(parents=True)
                    (directory / "artifact").write_text(target)
            output = merge(inputs, work / "merged", {"group": "test.group", "versions": {"skiko": "1.0"}})
            for artifact in ARTIFACTS:
                self.assertEqual((output / f"test/group/{artifact}/1.0/artifact").read_text(), "macos_arm64")
                for target, suffix in TARGETS.items():
                    self.assertEqual((output / f"test/group/{artifact}-{suffix}/1.0/artifact").read_text(), target)
            with self.assertRaisesRegex(ValueError, "non-empty"):
                merge(inputs, output, {"group": "test.group", "versions": {"skiko": "1.0"}})

    def test_missing_host_fails_before_creating_a_repository(self):
        with tempfile.TemporaryDirectory() as work:
            output = Path(work) / "merged"
            with self.assertRaisesRegex(ValueError, "Exactly"):
                merge({"mingw_x64": Path(work) / "windows"}, output, {})
            self.assertFalse(output.exists())

    def test_missing_extension_fails_before_copying_the_core(self):
        with tempfile.TemporaryDirectory() as work:
            work = Path(work)
            inputs = {target: work / target for target in TARGETS}
            for target, suffix in TARGETS.items():
                for module in ["skiko", f"skiko-{suffix}"]:
                    (inputs[target] / "test/group" / module / "1.0").mkdir(parents=True)
            with self.assertRaisesRegex(ValueError, "skiko-skottie"):
                merge(inputs, work / "merged", {"group": "test.group", "versions": {"skiko": "1.0"}})
            self.assertFalse((work / "merged").exists())

    def test_ui_can_assemble_only_its_required_core_without_a_skottie_publication(self):
        with tempfile.TemporaryDirectory() as work:
            work = Path(work)
            inputs = {target: work / target for target in TARGETS}
            for target, suffix in TARGETS.items():
                for module in ["skiko", f"skiko-{suffix}"]:
                    directory = inputs[target] / "test/group" / module / "1.0"
                    directory.mkdir(parents=True)
                    (directory / "artifact").write_text(target)
            output = merge(inputs, work / "merged", {"group": "test.group", "versions": {"skiko": "1.0"}}, artifacts=["skiko"])
            self.assertEqual((output / "test/group/skiko/1.0/artifact").read_text(), "macos_arm64")
            self.assertFalse((output / "test/group/skiko-skottie").exists())

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
