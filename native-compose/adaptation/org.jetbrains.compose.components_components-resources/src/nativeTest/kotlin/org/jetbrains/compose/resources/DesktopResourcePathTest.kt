package org.jetbrains.compose.resources

import kotlin.test.*

class DesktopResourcePathTest {
    @Test
    fun rejectsAbsoluteAndTraversalPathsOnEveryDesktop() {
        for (windows in [false, true]) {
            for (path in ["", "/etc/file", "C:/file", "files/../outside", "files/./x", "files//x", "files\\x", "files/\u0000x"]) {
                assertFailsWith<IllegalArgumentException>(path) { validateDesktopResourcePath(path, windows) }
            }
            validateDesktopResourcePath("composeResources/包名/files/目录 含空格.txt", windows)
        }
    }

    @Test
    fun rejectsWin32NormalizationAndDeviceNames() {
        for (path in ["files/.. /outside", "files/..../outside", "files/a ", "files/con.txt", "files/CON .txt", "files/NUL", "files/lpt1.log", "files/COM¹", "files/a?b"]) {
            assertFailsWith<IllegalArgumentException>(path) { validateDesktopResourcePath(path, true) }
        }
        validateDesktopResourcePath("files/CON.txt", false)
        validateDesktopResourcePath("files/COM10.txt", true)
    }
}
