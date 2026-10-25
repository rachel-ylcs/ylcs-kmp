"""Verify source-build version overrides and wrapper reuse without downloading toolchains."""
from pathlib import Path
import json
import sys
import tempfile
import tomllib
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import sync_upstream
from upstream_versions import align_catalog, apply_build_profile, main_profile, sync_wrappers


class UpstreamVersionsTest(unittest.TestCase):
    def test_shared_native_warning_option_reaches_the_included_build_only(self):
        with tempfile.TemporaryDirectory() as work:
            repository, fork = Path(work) / "root", Path(work) / "fork"
            repository.mkdir()
            (repository / "libs.versions.toml").write_text('[versions]\nkotlin="2.5.0-Beta2"\n')
            (repository / "settings.gradle.kts").write_text('repository { rachel { jvm { version = 25 } } }')
            (repository / "gradle.properties").write_text('kotlin.native.ignoreDisabledTargets=true\nnotShared=placeholder\n')
            (fork / "skiko/buildSrc").mkdir(parents=True)
            (fork / "skiko/buildSrc/build.gradle.kts").write_text('plugins { kotlin("jvm") }\n')
            properties = fork / "skiko/gradle.properties"
            properties.write_text('kotlin.native.ignoreDisabledTargets=false\nexisting=value\n')
            profile = main_profile(repository)
            apply_build_profile(fork, repository, profile)
            compiler_setup = (fork / "skiko/buildSrc/build.gradle.kts").read_text()
            self.assertIn('"-Xexpect-actual-classes"', compiler_setup)
            self.assertIn('"-Xcollection-literals"', compiler_setup)
            self.assertEqual(profile["gradleProperties"], {"kotlin.native.ignoreDisabledTargets": "true"})
            self.assertEqual(properties.read_text(), 'kotlin.native.ignoreDisabledTargets=true\nexisting=value\n')

    def test_catalog_uses_shared_coordinates_and_retains_upstream_only_tools(self):
        with tempfile.TemporaryDirectory() as work:
            catalog = Path(work) / "dependencies.toml"
            catalog.write_text('[versions]\nskiko = "0.150.1"\nkotlin = "2.2.0"\nuniqueTool = "1.0"\n[libraries]\nskiko = { module="org.jetbrains.skiko:skiko", version.ref="skiko" }\n', encoding="utf-8")
            align_catalog(catalog, {"versions": {"kotlin": "2.5.0-Beta2"},
                                   "libraries": {"org.jetbrains.skiko:skiko": "0.156.0"}}, {"kotlin": "kotlin"})
            result = tomllib.loads(catalog.read_text())["versions"]
            self.assertEqual(result, {"skiko": "0.156.0", "kotlin": "2.5.0-Beta2", "uniqueTool": "1.0"})

    def test_all_fork_wrappers_share_the_root_distribution_and_launchers(self):
        with tempfile.TemporaryDirectory() as work:
            repository, fork = Path(work) / "root", Path(work) / "fork"
            names = ["gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat"]
            for name in names:
                path = repository / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(f"main {name}".encode())
            for build in [fork, fork / "skiko", fork / "sample"]:
                wrapper = build / names[0]
                wrapper.parent.mkdir(parents=True)
                wrapper.write_text("old version")
            sync_wrappers(fork, repository)
            for build in [fork, fork / "skiko", fork / "sample"]:
                for name in names:
                    self.assertEqual((build / name).read_bytes(), (repository / name).read_bytes())

    def test_root_dependency_upgrade_does_not_require_editing_the_source_lock(self):
        with tempfile.TemporaryDirectory() as work:
            repository = Path(work)
            native = repository / "native-compose"
            (native / "upstream").mkdir(parents=True)
            lock = {"group": "test.native", "versions": {"skiko": "0.156.0", "kotlin": "2.2.0"}}
            (native / "upstream/lock.json").write_text(json.dumps(lock))
            (repository / "libs.versions.toml").write_text('[versions]\nskiko="0.156.0"\nkotlin="2.5.0-Beta2"\n[libraries]\nnative-skiko={module="test.native:skiko",version.ref="skiko"}\n')
            with patch.object(sync_upstream, "ROOT", native):
                selected = sync_upstream.load_lock()
            self.assertEqual(selected["versions"]["kotlin"], "2.5.0-Beta2")
            self.assertEqual(selected["upstreamVersions"]["kotlin"], "2.2.0")
            self.assertEqual(json.loads((native / "upstream/lock.json").read_text()), lock)

    def test_different_source_release_cannot_be_silently_relabelled(self):
        with tempfile.TemporaryDirectory() as work:
            repository = Path(work)
            native = repository / "native-compose"
            (native / "upstream").mkdir(parents=True)
            (native / "upstream/lock.json").write_text(json.dumps({"versions": {"skiko": "0.150.1"}}))
            (repository / "libs.versions.toml").write_text('[versions]\nskiko="0.156.0"\n')
            with patch.object(sync_upstream, "ROOT", native), self.assertRaisesRegex(ValueError, "does not need a downgrade"):
                sync_upstream.load_lock()
