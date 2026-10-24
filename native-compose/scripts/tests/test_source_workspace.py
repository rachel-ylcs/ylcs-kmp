"""Workspace synchronization must preserve caches and reject unrelated paths."""
from pathlib import Path
import os
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import sync_upstream


class SourceWorkspaceTest(unittest.TestCase):
    def test_updates_managed_sources_without_replacing_the_tree_or_build_cache(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            vendor = root / "vendor/skiko"
            staging = vendor / ".workspace-fixture"
            workspace = vendor / "workspace"
            for directory in [staging, workspace / "skiko/build"]:
                directory.mkdir(parents=True)
            (staging / "unchanged.kt").write_text("same")
            (staging / "new.kt").write_text("new")
            unchanged = workspace / "unchanged.kt"
            unchanged.write_text("same")
            os.utime(unchanged, ns=(1_000_000_000, 1_000_000_000))
            timestamp = unchanged.stat().st_mtime_ns
            (workspace / "removed.kt").write_text("old generated source")
            (workspace / "skiko/build/cached-output").write_text("keep compiled output")
            (workspace / "developer-note").write_text("keep unrelated file")
            with patch.multiple(sync_upstream, ROOT=root, SKIKO_VENDOR=vendor, WORKSPACE=workspace):
                files = sync_upstream.sync_workspace(staging, {"files": ["unchanged.kt", "removed.kt"]})
            self.assertEqual(files, ["new.kt", "unchanged.kt"])
            self.assertEqual(unchanged.stat().st_mtime_ns, timestamp)
            self.assertFalse((workspace / "removed.kt").exists())
            self.assertEqual((workspace / "new.kt").read_text(), "new")
            self.assertEqual((workspace / "skiko/build/cached-output").read_text(), "keep compiled output")
            self.assertEqual((workspace / "developer-note").read_text(), "keep unrelated file")

    def test_a_previous_manifest_cannot_remove_files_outside_the_workspace(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            vendor = root / "vendor/skiko"
            staging = vendor / ".workspace-fixture"
            staging.mkdir(parents=True)
            outside = root / "outside.kt"
            outside.write_text("preserved")
            with patch.multiple(sync_upstream, ROOT=root, SKIKO_VENDOR=vendor, WORKSPACE=vendor / "workspace"), \
                    self.assertRaisesRegex(ValueError, "Unsafe generated source path"):
                sync_upstream.sync_workspace(staging, {"files": ["../../../outside.kt"]})
            self.assertEqual(outside.read_text(), "preserved")

    def test_legacy_cleanup_removes_only_recognized_generated_copies(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            vendor = root / "vendor/skiko"
            old_prepared = vendor / "prepared" / ("a" * 16)
            old_source = vendor / "sources" / ("b" * 64)
            unrecognized = vendor / "prepared" / ("c" * 16)
            workspace = vendor / "workspace"
            for directory in [old_prepared, old_source, unrecognized, workspace]:
                directory.mkdir(parents=True)
            (old_prepared / ".rachel-prepared.json").write_text("{}")
            (old_source / ".rachel-extracted").write_text("audited source")
            (workspace / "source.kt").write_text("active")
            archive = vendor / "skiko-upstream.zip"
            archive.write_bytes(b"cached archive")
            with patch.multiple(sync_upstream, ROOT=root, SKIKO_VENDOR=vendor, WORKSPACE=workspace):
                sync_upstream.cleanup_legacy_sources()
            self.assertFalse(old_prepared.exists())
            self.assertFalse(old_source.exists())
            self.assertTrue(unrecognized.is_dir())
            self.assertEqual((workspace / "source.kt").read_text(), "active")
            self.assertEqual(archive.read_bytes(), b"cached archive")
