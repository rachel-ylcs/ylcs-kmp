@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.fs

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import love.yinlin.extension.cleaning
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestOPFS {
    @Test
    fun asynchronousFileHelpersReadOpfsFilesOnTheBrowserMainThread() = runTest {
        val directory = File("image-review-${Random.nextInt().toUInt()}")
        val file = File(directory, "picture.txt")
        cleaning({
            directory.mkdir()
            file.write { it.writeString("image-fixture") }
            assertEquals(13L, file.fileSize())
            assertContentEquals("image-fixture".encodeToByteArray(), file.read { it.readByteArray() })
            assertContentEquals("image-fixture".encodeToByteArray(), file.readByteArray())
            assertEquals("image-fixture", file.readText())
        }) { directory.delete() }
    }

    @Test
    fun testStorageManager() = runTest {
        val storageManager = window.navigator.storage
        val estimate = storageManager.estimate().await()
        println("quota = ${estimate.quota}, usage = ${estimate.usage}, usageDetails = ${estimate.usageDetails}\n")
        println("persist = ${storageManager.persist().await().toBoolean()}\n")
        println("persisted = ${storageManager.persisted().await().toBoolean()}\n")
        println("root = ${storageManager.getDirectory().await()}\n")
    }
}