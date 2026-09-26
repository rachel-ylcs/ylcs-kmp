package love.yinlin.common

import love.yinlin.data.compose.Picture
import love.yinlin.data.information.UnifiedPicture

val UnifiedPicture.asPicture: Picture get() = Picture(this.image, this.source, this.video)

val List<UnifiedPicture>.asPicture: List<Picture> get() = this.map { it.asPicture }