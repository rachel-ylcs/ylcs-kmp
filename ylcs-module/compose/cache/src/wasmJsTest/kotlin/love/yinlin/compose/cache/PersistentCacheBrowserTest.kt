package love.yinlin.compose.cache

import kotlinx.coroutines.test.runTest
import kotlinx.io.readString
import kotlinx.io.writeString
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.fs.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PersistentCacheBrowserTest {
    @Test
    fun legacyPersistentEntryUsesAsyncMetadataAndReadsWithoutFetching() = runTest {
        Coroutines.io {
            val directory = File("persistent-review-${Random.nextInt().toUInt()}")

            cleaning({
                directory.mkdir()
                File(directory, "-123").write { it.writeString("legacy-fixture") }
                var fetches = 0
                val cache = DiskCache<String>(directory, key = { "-123" }) { _, _ ->
                    fetches++
                    error("Existing entry must be retained")
                }

                assertEquals("legacy-fixture", cache.loadString("source"))
                assertContentEquals("legacy-fixture".encodeToByteArray(), cache.loadByteArray("source"))
                assertEquals("legacy-fixture", cache.load("source")?.use { it.readString() })
                assertEquals(0, fetches)
            }) {
                directory.delete()
            }
        }
    }
}