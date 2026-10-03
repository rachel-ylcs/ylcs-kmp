package love.yinlin.tpl

import kotlinx.coroutines.test.runTest
import love.yinlin.data.radio.RadioUserInfo
import love.yinlin.tpl.radio.RadioAPI
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestRadioAPI {
    @Test
    fun testRadio() = runTest {
        val rachel = RadioUserInfo.Default[0].id
        val cookie = RadioAPI.generateCookie()!!
        assertTrue { cookie.nmtid.isNotEmpty() }
        val user = RadioAPI.requestUser(rachel)
        assertNotNull(user)
        val programs = RadioAPI.requestPrograms(rachel, cookie)
        assertNotNull(programs)
        assertTrue { programs.isNotEmpty() }
        val program = programs[0]
        assertTrue { program.audioId.isNotEmpty() }
        val audio = RadioAPI.requestAudio(program.audioId, cookie)!!
        assertTrue { audio.size > 0L && audio.url.isNotEmpty() }
    }
}