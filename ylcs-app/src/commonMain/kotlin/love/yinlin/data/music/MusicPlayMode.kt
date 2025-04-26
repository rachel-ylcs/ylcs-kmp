package love.yinlin.data.music

enum class MusicPlayMode {
    ORDER, LOOP, RANDOM;

    val next: MusicPlayMode get() = when (this) {
        ORDER -> LOOP
        LOOP -> RANDOM
        RANDOM -> ORDER
    }

    companion object {
        fun fromInt(value: Int): MusicPlayMode? = when (value) {
            ORDER.ordinal -> ORDER
            LOOP.ordinal -> LOOP
            RANDOM.ordinal -> RANDOM
            else -> null
        }
    }
}