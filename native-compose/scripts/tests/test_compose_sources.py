"""Source preparation must reject stubs, version relabeling and tracked local edits."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import sync_compose
from compose_adapt import legacy_paragraph_bridges, replace_once


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

    def test_foundation_sources_cannot_be_shared_with_windows_or_linux(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = self.fixture(Path(temporary), ["src/darwinMain/kotlin"])
            with patch.object(sync_compose, "ROOT", root), self.assertRaisesRegex(ValueError, "Foundation sources must belong to macosMain"):
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

    def managed_fixture(self, directory):
        root = directory / "native-compose"
        vendor = root / "vendor/compose"
        workspace = vendor / "workspace"
        prefix = "compose/ui/ui-text/src/nativeMain/kotlin/androidx/compose/ui/text/"
        paths = [prefix + "Changed.kt", prefix + "Replaced.kt"]
        state = {"schema": 1, "commit": "a" * 40, "files": {}}
        for relative in paths:
            path = workspace / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            original = b"original upstream source\n"
            prepared = b"prepared source\n" if relative == paths[0] else None
            state["files"][relative] = {"original": hashlib.sha256(original).hexdigest(),
                                        "prepared": hashlib.sha256(prepared).hexdigest() if prepared else None}
            if prepared:
                path.write_bytes(prepared)
        (vendor / "source-edits.json").write_text(json.dumps(state))
        return root, vendor, workspace, paths

    def test_only_recorded_prepared_sources_can_be_restored(self):
        for interrupted in [False, True]:
            with self.subTest(interrupted=interrupted), tempfile.TemporaryDirectory() as temporary:
                root, vendor, workspace, paths = self.managed_fixture(Path(temporary))
                if interrupted:
                    (workspace / paths[0]).write_bytes(b"original upstream source\n")
                unrelated = workspace / "untouched.kt"
                unrelated.write_text("preserve unrelated source")
                with patch.multiple(sync_compose, ROOT=root, VENDOR=vendor, WORKSPACE=workspace), \
                        patch.object(sync_compose, "git", return_value=subprocess.CompletedProcess([], 0, "a" * 40 + "\n")) as git:
                    sync_compose.restore_prepared_sources()
                    self.assertEqual(git.call_count, 2)
                    git.assert_any_call("restore", "--source=HEAD", "--worktree", "--", *paths)
                self.assertEqual(unrelated.read_text(), "preserve unrelated source")

    def test_user_edits_to_a_managed_source_stop_before_any_restore(self):
        with tempfile.TemporaryDirectory() as temporary:
            root, vendor, workspace, paths = self.managed_fixture(Path(temporary))
            (workspace / paths[1]).write_text("preserve this user edit")
            before = (workspace / paths[0]).read_bytes()
            with patch.multiple(sync_compose, ROOT=root, VENDOR=vendor, WORKSPACE=workspace), \
                    patch.object(sync_compose, "git", return_value=subprocess.CompletedProcess([], 0, "a" * 40 + "\n")) as git:
                with self.assertRaisesRegex(ValueError, "local tracked edits"):
                    sync_compose.restore_prepared_sources()
                self.assertEqual(git.call_count, 1)
            self.assertEqual((workspace / paths[1]).read_text(), "preserve this user edit")
            self.assertEqual((workspace / paths[0]).read_bytes(), before)

    def test_managed_paths_cannot_escape_the_compose_checkout(self):
        with tempfile.TemporaryDirectory() as temporary:
            root, vendor, workspace, _ = self.managed_fixture(Path(temporary))
            with patch.multiple(sync_compose, ROOT=root, VENDOR=vendor, WORKSPACE=workspace):
                for relative in ["../outside.kt", "compose/ui/../../outside.kt", "/compose/ui/absolute.kt", ".git/config.kt"]:
                    with self.subTest(relative=relative), self.assertRaisesRegex(ValueError, "Invalid managed Compose source"):
                        sync_compose.checked_source(relative)

    def test_source_drift_is_rejected_instead_of_silently_skipping_a_fix(self):
        with self.assertRaisesRegex(ValueError, "Compose upstream drift"):
            replace_once("a renamed declaration", "old declaration", "replacement")

    def test_legacy_suppression_does_not_change_modern_paragraph_overloads(self):
        modern = "actual fun Paragraph(\n    defaultLocaleList: LocaleList,\n): Paragraph =\n    backend.create(defaultLocaleList)\n"
        legacy = "actual fun Paragraph(\n    text: String,\n): Paragraph =\n    backend.create(LocaleList.current)\n"
        existing = '@Suppress("DEPRECATION")\n' + legacy
        self.assertEqual(legacy_paragraph_bridges(modern), modern)
        self.assertEqual(legacy_paragraph_bridges(legacy), '@Suppress("DEPRECATION")\n' + legacy)
        self.assertEqual(legacy_paragraph_bridges(existing), existing)
