package org.jetbrains.compose.resources

internal expect fun resourceBaseDirectory(): String
internal expect fun resourceDisplayScale(): Float
internal expect fun resourceFileExists(path: String): Boolean
internal expect fun readResourceFile(path: String, offset: Long, requestedSize: Long?): ByteArray
