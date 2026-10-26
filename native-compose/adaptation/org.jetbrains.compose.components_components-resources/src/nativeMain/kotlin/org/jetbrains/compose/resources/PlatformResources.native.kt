@file:OptIn(org.jetbrains.skiko.InternalSkikoApi::class)

package org.jetbrains.compose.resources

import org.jetbrains.skiko.resources.DesktopResourceSupport

internal fun resourceBaseDirectory(): String = DesktopResourceSupport.baseDirectory()

internal fun resourceDisplayScale(): Float = DesktopResourceSupport.displayScale()

internal fun resourceFileExists(path: String): Boolean = DesktopResourceSupport.isFile(path)

internal fun readResourceFile(path: String, offset: Long, requestedSize: Long?): ByteArray = DesktopResourceSupport.readFile(path, offset, requestedSize) ?: throw MissingResourceException(path)
