package love.yinlin.api

object ServerRes : APIRes("public") {
    object Activity : APIRes(this) {
        fun activity(uniqueId: String) = APIRes(this, "${uniqueId}.webp")
    }

    object Assets : APIRes(this) {
        val DefaultAvatar = APIRes(this, "default_avatar.webp")
        val DefaultWall = APIRes(this, "default_wall.webp")
    }

    object Emoji : APIRes(this) {
        fun webp(id: Int) = APIRes(this, "${id}.webp")
        fun lottie(id: Int) = APIRes(this, "${id}.json")
    }

    object Game : APIRes(this) {
        fun x(id: Int) = APIRes(this, "game${id}x.webp")
        fun y(id: Int) = APIRes(this, "game${id}y.webp")
        fun xy(id: Int, isX: Boolean) = if (isX) x(id) else y(id)
        fun res(id: Int, key: String) = APIRes(this, "game$id$key.webp")
        object Rhyme : APIRes(this) {
            fun pic(key: String) = APIRes(this, "$key.webp")
            fun res(filename: String) = APIRes(this, filename)
        }
    }

    object Photo : APIRes(this) {
        fun pic(classification: String, index: Int, thumb: Boolean) = APIRes(
            parent = APIRes(this, classification),
            name = "$index${if (thumb) ".thumb" else ""}.webp"
        )
    }

    object Mod : APIRes(this) {
        class Song(sid: String) : APIRes(this, sid) {
            fun res(filename: String) = APIRes(this, filename)
        }
    }

    object Users : APIRes(this) {
        class User(uid: Int) : APIRes(this, "$uid") {
            val avatar = APIRes(this, "avatar.webp")
            val wall = APIRes(this, "wall.webp")

            inner class Pics : APIRes(this) {
                fun pic(uniqueId: String) = APIRes(this, "${uniqueId}.webp")
            }
        }
    }
    object Prize : APIRes(this, "prize") {
        fun prize(itemID:Int)=APIRes(this,"${itemID}.webp")
    }
}