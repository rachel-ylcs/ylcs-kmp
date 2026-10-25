"""Source preparation must reject stubs, version relabeling and tracked local edits."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import sync_compose


class ComposeSourcesTest(unittest.TestCase):
    def fixture(self, directory, sources=None, version="1.13.0-beta01"):
        root = directory / "native-compose"
        (root / "upstream").mkdir(parents=True)
        lock = {"repository": "JetBrains/compose-multiplatform-core", "commit": "a" * 40,
                "group": "test.native", "version": version,
                "modules": {"ui-unit": {"alias": "native-compose-ui-unit", "redirectTargets": ["mingw_x64"],
                                          "sourceSets": {"nativeMain": sources or ["src/nonJvmMain/kotlin"]}}}}
        (root / "upstream/compose.json").write_text(json.dumps(lock))
        (directory / "libs.versions.toml").write_text('[versions]\ncmp="1.13.0-beta01"\n[libraries]\nnative-compose-ui-unit={module="test.native:ui-unit",version.ref="cmp"}\n')
        return root

    def test_stubs_and_parent_paths_cannot_enter_the_active_source_selection(self):
        for directory in ["src/commonStubsMain/kotlin", "../outside/kotlin", "src/linuxx64StubsMain/kotlin"]:
            with self.subTest(directory=directory), tempfile.TemporaryDirectory() as temporary:
                root = self.fixture(Path(temporary), [directory])
                with patch.object(sync_compose, "ROOT", root), self.assertRaisesRegex(ValueError, "Unaudited upstream source path"):
                    sync_compose.load_lock()

    def test_root_upgrade_cannot_relabel_old_source_as_a_new_release(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = self.fixture(Path(temporary), version="1.12.0")
            with patch.object(sync_compose, "ROOT", root), self.assertRaisesRegex(ValueError, "main project does not need a downgrade"):
                sync_compose.load_lock()

    def test_tracked_local_changes_stop_checkout_before_any_update(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            vendor = root / "vendor/compose"
            workspace = vendor / "workspace"
            (workspace / ".git").mkdir(parents=True)
            source = workspace / "local.kt"
            source.write_text("preserve this local edit")
            responses = [subprocess.CompletedProcess([], 0, "https://github.com/JetBrains/compose-multiplatform-core.git\n"),
                         subprocess.CompletedProcess([], 1, "")]
            with patch.multiple(sync_compose, ROOT=root, VENDOR=vendor, WORKSPACE=workspace), \
                    patch.object(sync_compose, "git", side_effect=responses) as git:
                with self.assertRaisesRegex(ValueError, "local tracked edits"):
                    sync_compose.checkout({"repository": "JetBrains/compose-multiplatform-core"}, offline=False)
                self.assertEqual(git.call_count, 2)
            self.assertEqual(source.read_text(), "preserve this local edit")
