package love.yinlin.extension

// mapString

inline fun CharSequence.mapString(transform: (Char) -> Char): String = buildString(length) {
    for (ch in this@mapString) append(transform(ch))
}

inline fun CharSequence.mapStringIndexed(transform: (Int, Char) -> Char): String = buildString(length) {
    for (i in indices) append(transform(i, this@mapStringIndexed[i]))
}