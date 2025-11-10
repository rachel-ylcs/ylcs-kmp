package love.yinlin.api

data object ServerRes : ResNode("public") {
    data object Activity : ResNode(this, "activity") {
        fun activity(uniqueId: String) = ResNode(this, "${uniqueId}.webp")
    }

    data object Assets : ResNode(this, "assets") {
        val DefaultAvatar = ResNode(this, "default_avatar.webp")

        val DefaultWall = ResNode(this, "default_wall.webp")
    }

    data object Emoji : ResNode(this, "emoji") {
        fun webp(id: Int) = ResNode(this, "${id}.webp")
        fun lottie(id: Int) = ResNode(this, "${id}.json")
    }

    data object Game : ResNode(this, "game") {
        fun x(id: Int) = ResNode(this, "game${id}x.webp")
        fun y(id: Int) = ResNode(this, "game${id}y.webp")
        fun xy(id: Int, isX: Boolean) = if (isX) x(id) else y(id)
        fun res(id: Int, key: String) = ResNode(this, "game$id$key.webp")
    }

    data object Photo : ResNode(this, "photo") {
        fun pic(classification: String, index: Int, thumb: Boolean) = ResNode(
            parent = ResNode(this, classification),
            name = "$index${if (thumb) ".thumb" else ""}.webp"
        )
    }

    data object Mod : ResNode(this, "mod") {
        class Song(sid: String) : ResNode(this, sid) {
            fun res(filename: String) = ResNode(this, filename)
        }
    }

    data object Users : ResNode(this, "users") {
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
}