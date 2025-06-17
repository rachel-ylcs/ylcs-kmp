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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private suspend inline fun WebSocketServerSession.send(data: LyricsSockets.SM) = this.sendSerialized(data)

object LyricsSocketsManager {
    @Serializable
    data class Lyrics(val q: String, val a: String)

    private data class Player(
        val uid: Int,
        val name: String,
        var session: DefaultWebSocketServerSession,
        var room: Room? = null
    ) {
        val info: LyricsSockets.PlayerInfo get() = LyricsSockets.PlayerInfo(uid, name)
    }

    private class Room(val info1: LyricsSockets.PlayerInfo, val info2: LyricsSockets.PlayerInfo) {
        private val lyrics = library.indices.shuffled().take(LyricsSockets.QUESTION_COUNT).map { library[it] }
        val roomId: String = UUID.randomUUID().toString()
        var createTime: Long = System.currentTimeMillis()
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

    private suspend fun onInviteTimer(uid: Int, targetInfo: LyricsSockets.PlayerInfo) {
        delay(LyricsSockets.INVITE_TIME)
        // 超时自动拒绝
        logger.warn("onInviteTimer: 超时 $targetInfo")
        val target = players[targetInfo.uid]
        if (target == null || target.room == null) {
            players[uid]?.session?.send(LyricsSockets.SM.RefuseInvitation(targetInfo))
        }
    }

    private suspend fun prepareGame(room: Room) {
        // 启动准备计时
        delay(LyricsSockets.PREPARE_TIME)
        logger.info("prepareGame: 准备结束 ${room.info1} ${room.info2}")
        val player1 = players[room.info1.uid]
        val player2 = players[room.info2.uid]
        if (player1?.room?.roomId == room.roomId) player1.session.send(LyricsSockets.SM.GameStart(room.questions))
        if (player2?.room?.roomId == room.roomId) player2.session.send(LyricsSockets.SM.GameStart(room.questions))
        onPlayingTimer(room)
    }

    private suspend fun onPlayingTimer(room: Room) {
        // 启动游戏计时
        delay(LyricsSockets.PLAYING_TIME)
        // 强制结算
        logger.info("onPlayingTimer: 强制结算 ${room.info1} ${room.info2}")
        players[room.info1.uid]?.room?.let { room1 ->
            if (room.roomId == room1.roomId) submitAnswers(room, room.info1.uid)
        }
        players[room.info2.uid]?.room?.let { room2 ->
            if (room.roomId == room2.roomId) submitAnswers(room, room.info2.uid)
        }
    }

    private suspend fun submitAnswers(room: Room, uid: Int) {
        if (room.info1.uid == uid && room.submitTime1 == null) {
            room.submitTime1 = System.currentTimeMillis()
            logger.warn("submitAnswers: ${room.info1}")
            if (room.submitTime2 != null) endGame(room)
        }
        else if (room.info2.uid == uid && room.submitTime2 == null) {
            room.submitTime2 = System.currentTimeMillis()
            logger.warn("submitAnswers: ${room.info2}")
            if (room.submitTime1 != null) endGame(room)
        }
    }

    private suspend fun endGame(room: Room) {
        val player1 = players[room.info1.uid]
        val player2 = players[room.info2.uid]
        val standardAnswers = room.answers
        val answers1 = room.answers1
        val answers2 = room.answers2
        // 计算正确数
        var count1 = 0
        var count2 = 0
        for (index in standardAnswers.indices) {
            val answer = standardAnswers[index]
            if (answers1[index] == answer) ++count1
            if (answers2[index] == answer) ++count2
        }
        logger.warn("endGame: count1=$count1, count2=$count2, answers1=$answers1, answers2=$answers2, standardAnswers=$standardAnswers")
        // 生成结果
        val lastSubmitTime = System.currentTimeMillis()
        val response = LyricsSockets.SM.SendResult(
            result1 = LyricsSockets.GameResult(room.info1, count1, (room.submitTime1 ?: lastSubmitTime) - room.createTime),
            result2 = LyricsSockets.GameResult(room.info2, count2, (room.submitTime2 ?: lastSubmitTime) - room.createTime)
        )
        // 仍然在线的玩家发送结果
        if (player1?.room?.roomId == room.roomId) {
            player1.session.send(response)
            player1.room = null
        }
        if (player2?.room?.roomId == room.roomId) {
            player2.session.send(response)
            player2.room = null
        }
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
                            logger.warn("Login: $uid $name lists=${players.values.joinToString(",")}")
                            session.send(LyricsSockets.SM.PlayerList(availablePlayers))
                        }
                        else {
                            logger.warn("Login: 重复连接服务器")
                            session.send(LyricsSockets.SM.Error("重复连接服务器"))
                            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "重复连接服务器"))
                        }
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
                                logger.warn("InvitePlayer: ${currentPlayer.info}")
                                target.session.send(LyricsSockets.SM.InviteReceived(currentPlayer.info))
                                // 启动邀请验证
                                scope.launch { onInviteTimer(currentPlayer.uid, target.info) }
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
                                val info = currentPlayer.info
                                val inviterInfo = inviter.info
                                logger.warn("InviteResponse: $inviterInfo $info")
                                if (msg.accept) {
                                    val room = Room(inviterInfo, info)
                                    inviter.room = room
                                    currentPlayer.room = room
                                    inviter.session.send(LyricsSockets.SM.GamePrepare(inviterInfo, info))
                                    session.send(LyricsSockets.SM.GamePrepare(info, inviterInfo))
                                    scope.launch { prepareGame(room) }
                                }
                                else inviter.session.send(LyricsSockets.SM.RefuseInvitation(info))
                            }
                        }
                    }
                    is LyricsSockets.CM.SaveAnswer if currentPlayer != null -> {
                        currentPlayer.room?.let { room ->
                            val triple = when (currentPlayer.uid) {
                                room.info1.uid -> Triple(room.answers1, room.info2.uid, room.answers2)
                                room.info2.uid -> Triple(room.answers2, room.info1.uid, room.answers1)
                                else -> null
                            }
                            logger.warn("SaveAnswer: $triple $msg")
                            triple?.let { (answers, otherUid, otherAnswers) ->
                                if (msg.index in 0 ..< LyricsSockets.QUESTION_COUNT) {
                                    answers[msg.index] = msg.answer
                                    // 通知进度
                                    val count = answers.count { it != null }
                                    val otherCount = otherAnswers.count { it != null }
                                    session.send(LyricsSockets.SM.AnswerUpdated(count, otherCount))
                                    val otherPlayer = players[otherUid]
                                    if (otherPlayer?.room?.roomId == room.roomId) {
                                        otherPlayer.session.send(LyricsSockets.SM.AnswerUpdated(otherCount, count))
                                    }
                                }
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
                logger.warn("finally 移除 ${player.uid} ${player.name}")
                players.remove(player.uid)
                player.room?.let { room -> submitAnswers(room, player.uid) }
            }
        }
    }
}