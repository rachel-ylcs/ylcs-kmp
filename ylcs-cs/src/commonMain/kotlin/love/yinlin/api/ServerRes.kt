package love.yinlin.api

object ServerRes : ResNode("public") {
    object Activity : ResNode(this, "activity") {
        fun activity(uniqueId: String) = ResNode(this, "${uniqueId}.webp")
    }

    object Assets : ResNode(this, "assets") {
        val DefaultAvatar = ResNode(this, "default_avatar.webp")

        val DefaultWall = ResNode(this, "default_wall.webp")
    }

    object Emoji : ResNode(this, "emoji") {
        fun webp(id: Int) = ResNode(this, "${id}.webp")
        fun lottie(id: Int) = ResNode(this, "${id}.json")
    }

    object Game : ResNode(this, "game") {
        fun x(id: Int) = ResNode(this, "game${id}x.webp")
        fun y(id: Int) = ResNode(this, "game${id}y.webp")
        fun xy(id: Int, isX: Boolean) = if (isX) x(id) else y(id)
        fun res(id: Int, key: String) = ResNode(this, "game$id$key.webp")
    }

    object Song : ResNode(this, "song") {
        fun song(sid: Int) = ResNode(this, "${sid}.webp")
    }

    object Users : ResNode(this, "users") {
        class User(uid: Int) : ResNode(this, "$uid") {
            val avatar = ResNode(this, "avatar.webp")

            val wall = ResNode(this, "wall.webp")

            inner class Pics : ResNode(this, "pics") {
                fun pic(uniqueId: String) = ResNode(this, "${uniqueId}.webp")
            }
        }
    }

    val Server = ResNode(this, "server.json")
    val Update = ResNode(this, "update.json")
    val Photo = ResNode(this, "photo.json")
}