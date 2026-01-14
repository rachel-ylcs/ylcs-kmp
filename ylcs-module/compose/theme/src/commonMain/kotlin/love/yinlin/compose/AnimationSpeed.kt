package love.yinlin.compose

import love.yinlin.compose.extension.localComposition

const val DefaultAnimationSpeed = 400

val LocalAnimationSpeed = localComposition { DefaultAnimationSpeed }