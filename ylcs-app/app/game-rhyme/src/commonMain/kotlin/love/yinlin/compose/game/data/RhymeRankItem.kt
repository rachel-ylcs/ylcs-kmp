package love.yinlin.compose.game.data

import androidx.compose.runtime.Stable
import love.yinlin.cs.ServerRes
import love.yinlin.cs.url
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.RhymeRank

@Stable
data class RhymeRankItem(
    val rid: Long,
    val uid: Int,
    val name: String,
    val avatarPath: String,
    val character: CharacterInfo,
    val score: Int,
    val statistics: List<Int>
) {
    companion object {
        fun parse(rankList: List<RhymeRank>): Map<RhymeDifficulty, List<RhymeRankItem>> {
            return rankList.groupBy { RhymeDifficulty.fromInt(it.result.difficulty) }.mapValues { (_, items) ->
                items.asSequence().sortedByDescending { it.result.score }.map { item ->
                    val result = item.result
                    val uid = item.uid
                    RhymeRankItem(
                        rid = item.rid,
                        uid = uid,
                        avatarPath = ServerRes.Users.User(uid).avatar.url,
                        name = item.name,
                        character = CharacterInfo.Pool[result.character] ?: CharacterInfo.Default,
                        score = result.score,
                        statistics = result.statistics
                    )
                }.toList()
            }
        }
    }
}