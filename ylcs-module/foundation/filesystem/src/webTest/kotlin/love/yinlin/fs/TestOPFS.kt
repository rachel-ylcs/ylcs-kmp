package love.yinlin.fs

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.cleaning
import love.yinlin.io.WebFileSink
import love.yinlin.platform.UnsupportedPlatformException
import kotlin.random.Random
import kotlin.test.*

class TestOPFS {
    private suspend fun withDirectory(block: suspend (File) -> Unit) {
        val directory = File("/rachel-opfs-test-${Random.nextLong().toULong()}")

        cleaning({
            directory.mkdir()
            block(directory)
        }) {
            Coroutines.wait {
                check(directory.deleteRecursively())
            }
        }
    }

    @Test
    fun createsListsAndDeletesDirectoryTrees() = runTest {
        withDirectory { directory ->
            val root = File("/")
            assertNull(root.parent)
            assertTrue(root.isDirectory())
            assertTrue(directory in root.list())
            assertTrue(directory.name in File("").list().map { it.name })

            val nested = File(directory, "a\\b//./child/..")
            val file = File(nested, "图像.txt")
            assertNull(file.metadata())
            nested.mkdir()
            nested.mkdir()
            assertEquals(nested, file.parent)
            assertTrue(file.writeText("数据"))
            assertEquals([file], nested.list())
            assertEquals(6L, directory.size())
            assertTrue(file.list().isEmpty())
            assertNull(File(file, "child").metadata())
            assertFailsWith<Throwable> { nested.delete() }
            assertTrue(nested.deleteRecursively())
            assertFalse(file.exists())
            nested.delete()
        }
    }

    @Test
    fun commitsTextBinaryAppendCopyAndEmptyWrites() = runTest {
        withDirectory { directory ->
            val file = File(directory, "binary")
            val copy = File(directory, "copy")
            val data = ByteArray(130_000) { (it % 251).toByte() }
            assertTrue(file.writeByteArray(data))
            assertContentEquals(data, file.read { it.readByteArray() })
            assertTrue(file.writeTo(copy))
            assertContentEquals(data, copy.readByteArray())

            assertTrue(file.writeText("first"))
            file.write(append = true) { it.writeString(" + second") }
            assertEquals("first + second", file.readText())
            assertTrue(file.writeByteArray([]))
            assertEquals(0L, file.fileSize())
            assertContentEquals([], file.readByteArray())
            assertFalse(File(directory, "missing", "file").writeText("fail"))
        }
    }

    @Test
    fun failedWriteLeavesPreviouslyCommittedDataAndReleasesWriter() = runTest {
        withDirectory { directory ->
            val file = File(directory, "original")
            assertTrue(file.writeText("original"))
            assertFailsWith<IllegalStateException> {
                file.write {
                    it.write(ByteArray(130_000))
                    it.close()
                    error("Abort the write")
                }
            }
            assertEquals("original", file.readText())
            assertTrue(file.writeText("retry"))
            assertEquals("retry", file.readText())
        }
    }

    @Test
    fun cancelsScopedWritesAndAwaitsRawSinkCommit() = runTest {
        withDirectory { directory ->
            val file = File(directory, "cancelled")
            assertTrue(file.writeText("kept"))
            val writing = CompletableDeferred<Unit>()
            val writer = launch {
                file.write {
                    it.writeString("discarded")
                    it.flush()
                    writing.complete(Unit)
                    awaitCancellation()
                }
            }
            writing.await()
            writer.cancelAndJoin()
            assertEquals("kept", file.readText())

            val raw = file.rawSink(append = true) as WebFileSink
            val buffer = Buffer().apply { writeString(" + raw") }
            raw.use { it.write(buffer, buffer.size) }
            raw.awaitClosed()
            assertEquals("kept + raw", file.readText())
        }
    }

    @Test
    fun movesFilesAndNestedDirectoriesAndPreservesSourceOnFailure() = runTest {
        withDirectory { directory ->
            val source = File(directory, "source")
            val inner = File(source, "inner")
            inner.mkdir()
            File(source, "empty").mkdir()
            val file = File(inner, "file")
            assertTrue(file.writeText("new"))
            assertFailsWith<IOException> { file.move(File(directory, "missing", "target")) }
            assertEquals("new", file.readText())
            assertFailsWith<IllegalArgumentException> { source.move(File(source, "descendant")) }

            val target = File(directory, "target")
            assertTrue(target.writeText("old"))
            assertFailsWith<IllegalStateException> { source.move(target) }
            file.move(target)
            assertFalse(file.exists())
            assertEquals("new", target.readText())
            assertEquals("renamed", target.rename("renamed")?.name)
            assertFalse(target.exists())

            assertTrue(file.writeText("nested"))
            val tree = File(directory, "tree")
            source.move(tree)
            assertFalse(source.exists())
            assertTrue(File(tree, "empty").isDirectory())
            assertEquals("nested", File(tree, "inner", "file").readText())
        }
    }

    @Test
    fun synchronousFileSystemEntryPointsReportUnsupported() {
        val file = File("browser")
        assertFailsWith<UnsupportedPlatformException> { file.metadataSync() }
        assertFailsWith<UnsupportedPlatformException> { file.mkdirSync() }
        assertFailsWith<UnsupportedPlatformException> { file.deleteSync() }
        assertFailsWith<UnsupportedPlatformException> { file.moveSync(File("other")) }
        assertFailsWith<UnsupportedPlatformException> { file.listSync() }
        assertFailsWith<UnsupportedPlatformException> { file.rawSourceSync() }
        assertFailsWith<UnsupportedPlatformException> { file.rawSinkSync() }
    }
}
