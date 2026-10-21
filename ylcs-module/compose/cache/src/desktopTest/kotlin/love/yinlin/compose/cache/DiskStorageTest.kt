package love.yinlin.compose.cache

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.fs.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("BlockingMethodInNonBlockingContext")
class DiskStorageTest {
    @Test
    fun persistentCacheReadsLegacyFilesAndDoesNotEvictThem() = runTest {
        Coroutines.io {
            val directory = Files.createTempDirectory("rachel-persistent").toFile()

            cleaning({
                java.io.File(directory, "-123").writeText("legacy")
                var fetches = 0
                val cache = DiskCache<String>(File(directory.path), key = { "-123" }) { _, sink ->
                    fetches++
                    sink.write(byteArrayOf(42))
                }

                assertEquals("legacy", cache.loadString("source"))
                assertEquals(0, fetches)

                val store = FileDiskStorage(File(directory.path))
                repeat(4) { index -> store.write("key$index") { it.write(ByteArray(1024)) } }
                assertEquals("legacy", cache.loadString("source"))
            }) {
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun lruRestoresAccessOrderAndCleansInterruptedWrites() = runTest {
        Coroutines.io {
            val directory = Files.createTempDirectory("rachel-lru").toFile()

            cleaning({
                val path = File(directory.path)
                val store = FileDiskStorage(path, DiskPolicy.Lru(8))
                store.write("a") { it.write(ByteArray(4) { 1 }) }
                store.write("b") { it.write(ByteArray(4) { 2 }) }
                assertContentEquals(ByteArray(4) { 1 }, store.read("a") { it.readByteArray() })

                java.io.File(directory, ".interrupted.tmp").writeText("uncommitted")
                val reopened = FileDiskStorage(path, DiskPolicy.Lru(8))
                reopened.write("c") { it.write(ByteArray(4) { 3 }) }
                assertNull(reopened.read("b") { it.readByteArray() })
                assertContentEquals(ByteArray(4) { 1 }, reopened.read("a") { it.readByteArray() })
                assertTrue(directory.listFiles().orEmpty().none { it.extension == "tmp" })
            }) {
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun externalApplicationClearDoesNotDisableFutureWrites() = runTest {
        Coroutines.io {
            val directory = Files.createTempDirectory("rachel-external-clear").toFile()

            cleaning({
                val store = FileDiskStorage(File(directory.path), DiskPolicy.Lru(8))
                store.write("old") { it.write(byteArrayOf(1, 2)) }
                assertTrue(directory.deleteRecursively())

                store.write("fresh") { it.write(byteArrayOf(3, 4)) }
                assertNull(store.read("old") { it.readByteArray() })
                assertContentEquals(byteArrayOf(3, 4), store.read("fresh") { it.readByteArray() })
            }) {
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun cancellationPreservesOldValueAndPathTraversalIsRejected() = runTest {
        Coroutines.io {
            val directory = Files.createTempDirectory("rachel-atomic").toFile()

            cleaning({
                val store = FileDiskStorage(File(directory.path))
                store.write("key") { it.write(byteArrayOf(1, 2)) }

                assertFailsWith<CancellationException> {
                    store.write("key") {
                        it.write(byteArrayOf(9))
                        throw CancellationException("cancel test writer")
                    }
                }
                assertContentEquals(byteArrayOf(1, 2), store.read("key") { it.readByteArray() })
                assertFailsWith<IllegalArgumentException> { store.write("../escape") { it.write(byteArrayOf(1)) } }
                assertTrue(directory.listFiles().orEmpty().none { it.extension == "tmp" })
            }) {
                directory.deleteRecursively()
            }
        }
    }
}