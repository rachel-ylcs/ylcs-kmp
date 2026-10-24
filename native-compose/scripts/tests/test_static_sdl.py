"""The SDK may contain local paths; the published CInterop may only export system flags."""
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_sdl import linux_cmake_options, portable_link_options, static_link_options


class StaticSdlTest(unittest.TestCase):
    def test_macos_driver_flags_become_direct_linker_arguments(self):
        self.assertEqual(portable_link_options([
            "-Wl,-framework,CoreMedia", "-Wl,-weak_framework,UniformTypeIdentifiers",
            "-Xlinker", "-framework", "-Xlinker", "Metal", "-lSDL3-static",
        ]), ["-framework", "CoreMedia", "-weak_framework", "UniformTypeIdentifiers", "-framework", "Metal"])

    def test_standalone_metadata_does_not_invoke_a_broken_windows_pkg_config(self):
        with tempfile.TemporaryDirectory() as work:
            prefix = Path(work)
            pc = prefix / "lib/pkgconfig/sdl3.pc"
            pc.parent.mkdir(parents=True)
            pc.write_text('Requires.private: \nLibs: -L${libdir} -lSDL3-static\nLibs.private: -lkernel32 -lole32\n')
            with patch("prepare_sdl.shutil.which", return_value="C:/Strawberry/perl/bin/pkg-config.BAT"), \
                    patch("prepare_sdl.subprocess.check_output", side_effect=OSError("broken resolver")) as resolver:
                self.assertEqual(static_link_options(prefix, {}), ["-lkernel32", "-lole32"])
                resolver.assert_not_called()

    def test_required_packages_are_resolved_instead_of_dropped(self):
        with tempfile.TemporaryDirectory() as work:
            prefix = Path(work)
            pc = prefix / "lib/pkgconfig/sdl3.pc"
            pc.parent.mkdir(parents=True)
            pc.write_text('Requires: x11\nLibs: -lSDL3-static\n')
            with patch("prepare_sdl.shutil.which", return_value="pkg-config"), \
                    patch("prepare_sdl.subprocess.check_output", return_value="-L/producer/lib -lSDL3-static -lX11 -ldl"):
                self.assertEqual(static_link_options(prefix, {}), ["-lX11", "-ldl"])

    def test_linux_build_uses_the_distributions_toolchain_and_sysroot(self):
        with tempfile.TemporaryDirectory(prefix="native toolchain ") as work:
            root = Path(work)
            native_home = root / "native-home"
            properties = native_home / "konan/konan.properties"
            properties.parent.mkdir(parents=True)
            properties.write_text('toolchainDependency.linux_x64=selected-gcc-abi\nllvm.linux_x64.user=selected-llvm\n')
            cache = root / "native-cache"
            toolchain = cache / "dependencies/selected-gcc-abi"
            sysroot = toolchain / "x86_64-unknown-linux-gnu/sysroot"
            compiler = cache / "dependencies/selected-llvm/bin/clang"
            for path in [compiler, toolchain / "bin/x86_64-unknown-linux-gnu-ar",
                         toolchain / "bin/x86_64-unknown-linux-gnu-ranlib", sysroot / "usr/include/features.h"]:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.touch()
            with patch("prepare_sdl.subprocess.run") as bootstrap:
                options, profile = linux_cmake_options(native_home, {"KONAN_DATA_DIR": str(cache)})
                bootstrap.assert_not_called()
            self.assertIn(f"-DCMAKE_SYSROOT={sysroot}", options)
            self.assertIn(f"-DCMAKE_C_COMPILER={compiler}", options)
            self.assertIn(f"-DCMAKE_C_COMPILER_EXTERNAL_TOOLCHAIN={toolchain}", options)
            self.assertEqual(profile["toolchainDependency.linux_x64"], "selected-gcc-abi")

    def test_missing_native_toolchain_cannot_fall_back_to_host_libc(self):
        with tempfile.TemporaryDirectory() as work:
            native_home = Path(work)
            properties = native_home / "konan/konan.properties"
            properties.parent.mkdir(parents=True)
            properties.write_text('toolchainDependency.linux_x64=missing-abi\nllvm.linux_x64.user=missing-llvm\n')
            with patch("prepare_sdl.subprocess.run"), patch("prepare_sdl.VENDOR", native_home / "vendor"), \
                    self.assertRaisesRegex(ValueError, "toolchain is incomplete"):
                linux_cmake_options(native_home, {"KONAN_DATA_DIR": str(native_home / "cache")})

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
