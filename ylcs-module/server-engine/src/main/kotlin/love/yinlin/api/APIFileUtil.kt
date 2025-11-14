package love.yinlin.api

import java.io.File

fun APIFile.copy(other: ResNode2): File = File(files.first()).copyTo(File(other.path), true)