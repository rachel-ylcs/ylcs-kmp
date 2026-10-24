"""The SDK may contain local paths; the published CInterop may only export system flags."""
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_sdl import portable_link_options, static_link_options


class StaticSdlTest(unittest.TestCase):
    def test_archive_is_embedded_and_system_framework_pairs_survive(self):
        options = ["-L/producer/SDK/lib", "-lSDL3-static", "-mwindows", "-pthread",
                   "-framework", "Cocoa", "-framework", "IOKit", "-lm"]
        self.assertEqual(portable_link_options(options),
                         ["-lpthread", "-framework", "Cocoa", "-framework", "IOKit", "-lm"])

    def test_empty_private_requires_does_not_consume_the_next_field(self):
        with tempfile.TemporaryDirectory() as work:
            prefix = Path(work)
            pc = prefix / "lib/pkgconfig/sdl3.pc"
            pc.parent.mkdir(parents=True)
            pc.write_text('Requires.private: \nConflicts:\nLibs: -L${libdir} -lSDL3-static -lkernel32\nLibs.private: -lole32\n')
            with patch("prepare_sdl.shutil.which", return_value=None):
                self.assertEqual(static_link_options(prefix, {}), ["-lkernel32", "-lole32"])

    def test_unresolved_private_packages_cannot_be_silently_omitted(self):
        with tempfile.TemporaryDirectory() as work:
            prefix = Path(work)
            pc = prefix / "lib/pkgconfig/sdl3.pc"
            pc.parent.mkdir(parents=True)
            pc.write_text('Requires.private: native-system-package\nLibs: -lSDL3\n')
            with patch("prepare_sdl.shutil.which", return_value=None), self.assertRaisesRegex(ValueError, "pkg-config"):
                static_link_options(prefix, {})

    def test_producer_archive_paths_are_rejected(self):
        for option in ["/home/builder/libprivate.a", "C:/SDK/private.lib", "-Wl,-rpath,/tmp/sdk"]:
            with self.subTest(option=option), self.assertRaisesRegex(ValueError, "Non-portable"):
                portable_link_options([option])
