package love.yinlin.data.rachel.game

import love.yinlin.resources.*
import org.jetbrains.compose.resources.DrawableResource

val Game.imgX: DrawableResource get() = when (this) {
    Game.AnswerQuestion -> Res.drawable.game1x
    Game.BlockText -> Res.drawable.game2x
    Game.FlowersOrder -> Res.drawable.game3x
    Game.SearchAll -> Res.drawable.game4x
}

val Game.imgY: DrawableResource get() = when (this) {
    Game.AnswerQuestion -> Res.drawable.game1y
    Game.BlockText -> Res.drawable.game2y
    Game.FlowersOrder -> Res.drawable.game3y
    Game.SearchAll -> Res.drawable.game4y
}