package org.libpag

internal enum class PAGLayerType(internal val value: Long) {
    Unknown(0L),
    Null(1L),
    Solid(2L),
    Text(3L),
    Shape(4L),
    Image(5L),
    PreCompose(6L),
    Camera(7L),

    File(-1L);
}