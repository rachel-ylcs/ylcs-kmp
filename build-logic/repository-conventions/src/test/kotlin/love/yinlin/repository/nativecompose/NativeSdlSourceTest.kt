package love.yinlin.repository.nativecompose

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NativeSdlSourceTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun directApiProfilePreservesOtherPlatformSwitchesAndIsIdempotent() {
        val header = temporary.root.resolve("src/dynapi/SDL_dynapi.h").apply {
            parentFile.mkdirs()
            writeText("/* upstream copyright */\r\n#if PRIVATE_PLATFORM\r\n#define SDL_DYNAMIC_API 0\r\n#endif\r\n#ifndef SDL_DYNAMIC_API\r\n#define SDL_DYNAMIC_API 1\r\n#endif\r\n")
        }
        NativeSdlSource.disableDynamicApi(temporary.root)
        val first = header.readText()
        assertTrue(first.startsWith("/* upstream copyright */\r\n#if PRIVATE_PLATFORM\r\n#define SDL_DYNAMIC_API 0\r\n"))
        assertTrue("#define SDL_DYNAMIC_API 0 /* Rachel:" in first)
        NativeSdlSource.disableDynamicApi(temporary.root)
        assertEquals(first, header.readText())
    }

    @Test
    fun upstreamSwitchChangesFailBeforeModifyingTheSource() {
        val header = temporary.root.resolve("src/dynapi/SDL_dynapi.h").apply { parentFile.mkdirs() }
        for (source in listOf("#define SDL_DYNAMIC_API 2\n", "#define SDL_DYNAMIC_API 1\n#define SDL_DYNAMIC_API 1\n")) {
            header.writeText(source)
            assertFailsWith<IllegalStateException> { NativeSdlSource.disableDynamicApi(temporary.root) }
            assertEquals(source, header.readText())
        }
    }
}
