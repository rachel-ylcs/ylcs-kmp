package love.yinlin.tpl

import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class TestWeiboEncrypt {
    @Test
    fun testSetupH() {
        println("————————setupH————————")
        repeat(3) {
            println(WeiboEncrypt.setupH())
        }
    }

    @Test
    fun testAesCbcEncrypt() {
        runTest {
            println("————————aesCbcEncrypt————————")
            println(WeiboEncrypt.aesCbcEncrypt("Hello World!", WeiboEncrypt.setupH()))
        }
    }

    @Test
    fun testHash() {
        runTest {
            println("————————hash————————")
            println(listOf("sha256", "md5", "sha1").map { WeiboEncrypt.hash("Hello World!", it) })
        }
    }

    @Test
    fun testRSAEncrypt() {
        runTest {
            println("————————rSAEncrypt————————")
            println(WeiboEncrypt.rsaEncrypt("Hello World!"))
        }
    }

    @Test
    fun testLoad() {
        runTest {
            println("————————generateWeiboEncryptInfo————————")
            WeiboAPI.generateWeiboEncryptInfo()
        }
    }
}