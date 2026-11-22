package love.yinlin.tpl

import love.yinlin.data.radio.RadioUserInfo
import love.yinlin.tpl.radio.RadioAPI
import kotlin.test.Test

class TestRadioAPI {
    @Test
    fun testRadio() = TPTest {
        val rachel = RadioUserInfo.Default[0].id

        val cookie by tp { RadioAPI.generateCookie() }
        tpAssert { cookie.nmtid.isNotEmpty() }

        val user by tp { RadioAPI.requestUser(rachel) }

        val programs by tp { RadioAPI.requestPrograms(rachel, cookie) }
        tpAssert { programs.isNotEmpty() }
        val program = programs[0]
        tpAssert { program.audioId.isNotEmpty() }

        val audio by tp { RadioAPI.requestAudio(program.audioId, cookie) }
        tpAssert { audio.size > 0L && audio.url.isNotEmpty() }
    }
}
