package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.crypto.XXHash64
import love.yinlin.extension.DateEx

@Serializable
@Stable
data class RhymeUploadResult(
    val uid: Int, // 用户ID
    val sid: String, // 歌曲ID
    val ts: Long, // 时间戳
    val difficulty: Int, // 难度
    val character: Int, // 角色ID
    val duration: Long, // 时长
    val score: Int, // 分数
    val statistics: List<Int>, // 统计数据
    val hash: String,
) {
    companion object {
        private fun buildRaw(
            uid: Int,
            sid: String,
            ts: Long,
            difficulty: Int,
            character: Int,
            duration: Long,
            score: Int,
            statistics: List<Int>
        ): String = XXHash64.encodeToString(buildString {
            append(uid)
            append(sid)
            append(ts)
            append(difficulty)
            append(character)
            append(duration)
            append(score)
            append(statistics.size)
            statistics.forEach { append(it) }
        }) + ts.toString().map { digit -> 'a' + (digit - '0') }.joinToString("")

        private fun hash(
            uid: Int,
            sid: String,
            ts: Long,
            difficulty: Int,
            character: Int,
            result: RhymePlayResult
        ): String = buildRaw(uid, sid, ts, difficulty, character, result.duration, result.score, result.statistics)

        fun build(
            uid: Int,
            sid: String,
            ts: Long,
            difficulty: Int,
            character: Int,
            result: RhymePlayResult
        ): RhymeUploadResult = RhymeUploadResult(uid, sid, ts, difficulty, character, result.duration, result.score, result.statistics, hash(uid, sid, ts, difficulty, character, result))
    }

    val valid: Boolean get() {
        if (hash.length != 45) return false
        val ots = hash.substring(32).map { it - 'a' }.joinToString("").toLongOrNull() ?: return false
        if (ots != ts) return false
        if (DateEx.CurrentLong - ots !in -60000L .. 3600000L) return false
        return buildRaw(uid, sid, ots, difficulty, character, duration, score, statistics) == hash
    }
}