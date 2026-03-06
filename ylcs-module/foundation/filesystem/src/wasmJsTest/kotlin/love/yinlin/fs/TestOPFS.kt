@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.fs

import kotlinx.browser.window
import kotlinx.coroutines.test.runTest
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.await
import kotlin.test.Test

class TestOPFS {
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