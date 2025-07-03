package io.nayuki.flac.common

import korlibs.io.stream.*

internal fun FastByteArrayInputStream.readLong(): Long = readS64BE()
internal fun FastByteArrayInputStream.readUnsignedShort(): Int = readU16BE()

