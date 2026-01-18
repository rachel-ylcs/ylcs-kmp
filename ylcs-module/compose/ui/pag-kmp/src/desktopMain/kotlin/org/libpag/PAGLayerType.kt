package org.libpag

enum class PAGLayerType(internal val value: Int) {
    Unknown(0),
    Null(1),
    Solid(2),
    Text(3),
    Shape(4),
    Image(5),
    PreCompose(6),
    Camera(7),

    File(114514),
}