package love.yinlin.api

import java.io.File

fun APIFile.copy(other: APIFile): File = File(files.first()).copyTo(File(other.files.first()), true)