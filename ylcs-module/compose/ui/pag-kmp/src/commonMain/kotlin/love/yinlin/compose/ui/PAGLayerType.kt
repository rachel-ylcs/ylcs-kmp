package love.yinlin.compose.ui

enum class PAGLayerType(internal val value: Int) {
    Unknown(0),
    Null(1),
    Solid(2),
    Text(3),
    Shape(4),
    Image(5),
    PreCompose(6),
    Camera(7),

    File(114514);

    internal val originType: Int get() = if (this == File) PreCompose.ordinal else this.ordinal
}