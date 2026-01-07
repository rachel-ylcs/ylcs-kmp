package love.yinlin.extension

import kotlinx.io.files.Path

fun Path.toNioPath(): java.nio.file.Path = java.nio.file.Paths.get(this.toString())