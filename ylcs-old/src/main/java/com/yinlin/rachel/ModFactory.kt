@file:SuppressLint("UnsafeOptInUsageError")
package com.yinlin.rachel

import android.annotation.SuppressLint
import androidx.compose.runtime.Stable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.random.Random

@Stable
@Serializable
enum class MusicResourceType(
    val id: Int,
    val description: String,
    val uniqueName: String? = null,
    val defaultName: String? = uniqueName
) {
    Config(
        id = 0,
        description = "媒体配置",
        uniqueName = "config"
    ),
    Audio(
        id = 10,
        description = "音源",
        defaultName = "flac"
    ),
    Record(
        id = 20,
        description = "封面",
        uniqueName = "record"
    ),
    Background(
        id = 21,
        description = "壁纸",
        uniqueName = "background"
    ),
    Animation(
        id = 22,
        description = "动画",
        uniqueName = "animation"
    ),
    LineLyrics(
        id = 30,
        description = "逐行歌词",
        defaultName = "lrc"
    ),
    Video(
        id = 40,
        description = "视频",
        defaultName = "pv"
    );
}

@Stable
@Serializable
data class ModInfo(
    val author: String = "无名", // 作者
)

@Stable
@Serializable
data class MusicResource(
    val id: Int,
    val name: String
) {
    init {
        require(check(id, name)) { "资源名称非法: $id-$name" }
    }

    override fun toString(): String = "$id-$name"

    companion object {
        @OptIn(ExperimentalContracts::class)
        private fun check(id: Int?, name: String): Boolean {
            contract {
                returns(true) implies (id != null)
            }

            return id != null && id >= 0 && name.length in 1 .. 32
        }

        fun fromString(text: String): MusicResource? = try {
            val list = text.split("-")
            MusicResource(list[0].toInt(), list[1])
        }
        catch (_: Throwable) { null }
    }
}

inline fun <reified T> T?.toJsonString(): String = if (this == null) "null" else Json.encodeToString(this)

object ModFactory {
    private const val MAGIC = 1211
    private const val INTERVAL = 1024 * 64
    const val VERSION = 4

    @Stable
    class Merge(
        private val mediaPaths: List<Path>,
        private val sink: Sink,
        private val info: ModInfo = ModInfo()
    ) {
        companion object {
            private fun Sink.writeLengthString(str: String) {
                val bytes = str.encodeToByteArray()
                writeInt(bytes.size)
                write(bytes)
            }
        }

        private suspend fun Sink.writeMetadata() {
            withContext(Dispatchers.IO) {
                writeInt(MAGIC) // 写文件头
                writeInt(VERSION) // 写版本号
                writeInt(mediaPaths.size) // 写媒体数
                writeLengthString(info.toJsonString()) // 写额外信息
            }
        }

        private suspend fun Sink.writeResource(resourcePath: Path, resource: MusicResource) {
            withContext(Dispatchers.IO) {
                writeInt(resource.id) // 写资源类型
                writeLengthString(resource.name) // 写资源名称
                // 写资源数据
                val resLength = SystemFileSystem.metadataOrNull(resourcePath)!!.size.toInt()
                require(resLength > 0) { "资源长度非法 $resourcePath Length: $resLength" }
                val times = resLength / INTERVAL
                val remain = resLength - times * INTERVAL
                writeInt(resLength) // 写资源长度
                SystemFileSystem.source(resourcePath).buffered().use { source ->
                    repeat(times) {
                        source.readTo(this@writeResource, INTERVAL.toLong())
                        writeByte(Random.nextInt(127).toByte())
                    }
                    if (remain > 0) source.readTo(this@writeResource, remain.toLong())
                }
            }
        }

        private suspend fun Sink.writeMedia(mediaPath: Path, filter: List<MusicResourceType>) {
            withContext(Dispatchers.IO) {
                writeLengthString(mediaPath.name) // 写媒体ID
                val resourcePaths = SystemFileSystem.list(mediaPath).filter { path ->
                    val resource = MusicResource.fromString(path.name)
                    require(resource != null) { "资源名非法 ${path.name}" }
                    filter.find { it.id == resource.id } == null
                }
                writeInt(resourcePaths.size) // 写资源数
                for (resourcePath in resourcePaths) {
                    val resource = MusicResource.fromString(resourcePath.name)
                    require(resource != null) { "资源名非法 ${resourcePath.name}" }
                    writeResource(resourcePath, resource)
                }
            }
        }

        suspend fun process(
            filter: List<MusicResourceType> = listOf(),
            onProcess: (Int, Int, String) -> Unit
        ): Result<Unit> = try {
            sink.writeMetadata()
            for ((index, mediaPath) in mediaPaths.withIndex()) {
                sink.writeMedia(mediaPath, filter)
                withContext(Dispatchers.Main) {
                    onProcess(index, mediaPaths.size, mediaPath.name)
                }
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}