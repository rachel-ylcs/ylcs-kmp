package love.yinlin.data.music

enum class MusicPlayMode {
    ORDER, LOOP, RANDOM;

    val next: MusicPlayMode get() = when (this) {
        ORDER -> LOOP
        LOOP -> RANDOM
        RANDOM -> ORDER
    }
}