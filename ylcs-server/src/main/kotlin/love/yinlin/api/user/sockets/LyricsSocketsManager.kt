package love.yinlin.api.user.sockets

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import love.yinlin.data.rachel.sockets.LyricsSockets
import love.yinlin.extension.parseJsonValue
import love.yinlin.logger
import java.util.concurrent.ConcurrentHashMap

private suspend inline fun WebSocketServerSession.send(data: LyricsSockets.SM) = this.sendSerialized(data)

object LyricsSocketsManager {
    @Serializable
    data class Lyrics(val q: String, val a: String)

    private data class Player(
        val uid: Int,
        val name: String,
        val session: DefaultWebSocketServerSession,
        var room: Room? = null
    ) {
        val info: LyricsSockets.PlayerInfo get() = LyricsSockets.PlayerInfo(uid, name)
    }

    private class Room(val info1: LyricsSockets.PlayerInfo, val info2: LyricsSockets.PlayerInfo) {
        private val lyrics = library.indices.shuffled().take(LyricsSockets.QUESTION_COUNT).map { library[it] }
        val createTime = System.currentTimeMillis()
        var submitTime1: Long? = null
        var submitTime2: Long? = null
        val questions: List<String> get() = lyrics.map { it.q }
        val answers: List<String> get() = lyrics.map { it.a }
        val answers1: MutableList<String?> = MutableList(LyricsSockets.QUESTION_COUNT) { null }
        val answers2: MutableList<String?> = MutableList(LyricsSockets.QUESTION_COUNT) { null }
    }

    private val library = run {
        val classLoader = LyricsSocketsManager::class.java.classLoader
        val stream = classLoader.getResourceAsStream("lyrics_game.json")!!
        val lyrics = stream.readAllBytes().decodeToString().parseJsonValue<List<Lyrics>>()!!
        stream.close()
        lyrics
    }

    private val players = ConcurrentHashMap<Int, Player>()
    private val scope = CoroutineScope(Dispatchers.Default)

    private val availablePlayers: List<LyricsSockets.PlayerInfo> get() = players.values.filter { it.room == null }.map { it.info }

    private suspend fun onInviteTimer(uid: Int, targetUid: Int) {
        delay(LyricsSockets.INVITE_TIME)
        // 超时自动拒绝
        val target = players[targetUid]
        val user = players[uid]
        if ((target == null || target.room == null) && user != null) {
            user.session.send(LyricsSockets.SM.InviteResult(false))
        }
    }

    private suspend fun onPlayingTimer(uid1: Int, uid2: Int) {
        delay(LyricsSockets.PLAYING_TIME)
        // 强制结算
        val room = players[uid1]?.room ?: players[uid2]?.room
        if (room != null) {
            submitAnswers(room, room.info1.uid)
            submitAnswers(room, room.info2.uid)
        }
    }

    private suspend fun submitAnswers(room: Room, uid: Int) {
        if (room.info1.uid == uid) {
            if (room.submitTime1 != null) room.submitTime1 = System.currentTimeMillis()
        }
        else if (room.info2.uid == uid) {
            if (room.submitTime2 != null) room.submitTime2 = System.currentTimeMillis()
        }

        if (room.submitTime1 != null && room.submitTime2 != null) endGame(room)
    }

    private suspend fun endGame(room: Room) {
        val player1 = players[room.info1.uid]
        val player2 = players[room.info2.uid]
        val standardAnswers = room.answers
        val answers1 = room.answers1
        val answers2 = room.answers2
        var count1 = 0
        var count2 = 0
        for (index in standardAnswers.indices) {
            val answer = standardAnswers[index]
            if (answers1[index] == answer) ++count1
            if (answers2[index] == answer) ++count2
        }
        logger.warn("count1=$count1, count2=$count2, answers1=$answers1, answers2=$answers2, standardAnswers=$standardAnswers")
        // 仍然在线的玩家发送结果
        val lastSubmitTime = System.currentTimeMillis()
        val response = LyricsSockets.SM.SendResult(
            result1 = LyricsSockets.GameResult(room.info1, count1, (room.submitTime1 ?: lastSubmitTime) - room.createTime),
            result2 = LyricsSockets.GameResult(room.info2, count2, (room.submitTime2 ?: lastSubmitTime) - room.createTime)
        )
        player1?.session?.send(response)
        player2?.session?.send(response)
    }

    suspend fun dispatchMessage(session: DefaultWebSocketServerSession) {
        var currentPlayer: Player? = null

        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue
                when (val msg = frame.readText().parseJsonValue<LyricsSockets.CM>()!!) {
                    is LyricsSockets.CM.Login -> {
                        val (uid, name) = msg.info
                        if (!players.containsKey(uid)) {
                            val player = Player(uid, name, session)
                            currentPlayer = player
                            players[uid] = player
                            logger.warn("加入 $uid $name lists=${players.values.joinToString(",")}")
                            session.send(LyricsSockets.SM.PlayerList(availablePlayers))
                        }
                        else session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "连接已存在"))
                    }
                    LyricsSockets.CM.GetPlayers if currentPlayer != null -> session.send(LyricsSockets.SM.PlayerList(availablePlayers))
                    is LyricsSockets.CM.InvitePlayer if currentPlayer != null -> {
                        val target = players[msg.targetUid]
                        when {
                            target == null -> session.send(LyricsSockets.SM.Error("对方未上线"))
                            currentPlayer.uid == target.uid -> session.send(LyricsSockets.SM.Error("不能邀请自己"))
                            currentPlayer.room != null -> session.send(LyricsSockets.SM.Error("你已在游戏中"))
                            target.room != null -> session.send(LyricsSockets.SM.Error("对方已在游戏中"))
                            else -> {
                                target.session.send(LyricsSockets.SM.InviteReceived(currentPlayer.info))
                                // 启动邀请验证
                                scope.launch { onInviteTimer(currentPlayer.uid, target.uid) }
                            }
                        }
                    }
                    is LyricsSockets.CM.InviteResponse if currentPlayer != null -> {
                        val inviter = players[msg.inviterUid]
                        when {
                            inviter == null -> session.send(LyricsSockets.SM.Error("对方未上线"))
                            currentPlayer.uid == inviter.uid -> session.send(LyricsSockets.SM.Error("不能邀请自己"))
                            currentPlayer.room != null -> session.send(LyricsSockets.SM.Error("你已在游戏中"))
                            inviter.room != null -> session.send(LyricsSockets.SM.Error("对方已在游戏中"))
                            else -> {
                                if (msg.accept) {
                                    val room = Room(inviter.info, currentPlayer.info)
                                    val questions = room.questions
                                    inviter.room = room
                                    currentPlayer.room = room
                                    inviter.session.send(LyricsSockets.SM.GameStart(inviter.info, currentPlayer.info, room.createTime, questions))
                                    session.send(LyricsSockets.SM.GameStart(currentPlayer.info, inviter.info, room.createTime, questions))
                                    // 启动游戏计时
                                    scope.launch { onPlayingTimer(inviter.uid, currentPlayer.uid) }
                                }
                                else inviter.session.send(LyricsSockets.SM.InviteResult(false))
                            }
                        }
                    }
                    is LyricsSockets.CM.SaveAnswer if currentPlayer != null -> {
                        currentPlayer.room?.let { room ->
                            val (answers, isFirst) = when (currentPlayer.uid) {
                                room.info1.uid -> room.answers1 to true
                                room.info2.uid -> room.answers2 to false
                                else -> null to false
                            }
                            if (answers != null && msg.index in 0 ..< LyricsSockets.QUESTION_COUNT) {
                                answers[msg.index] = msg.answer
                                // 通知对手自身进度
                                val player = players[if (isFirst) room.info2.uid else room.info1.uid]
                                player?.session?.send(LyricsSockets.SM.OtherAnswerUpdated(answers.count { it != null }))
                            }
                        }
                    }
                    LyricsSockets.CM.Submit if currentPlayer != null -> currentPlayer.room?.let { submitAnswers(it, currentPlayer.uid) }
                    else -> {}
                }
            }
        }
        catch (e: Throwable) {
            logger.warn(e.stackTraceToString())
        }
        finally {
            currentPlayer?.let { player ->
                logger.warn("移除 ${player.uid} ${player.name}")
                players.remove(player.uid)
                player.room?.let { room -> submitAnswers(room, player.uid) }
            }
        }
    }
}