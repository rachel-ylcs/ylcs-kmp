@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual class PAGShapeLayer(override val delegate: PlatformPAGShapeLayer) : PAGLayer(delegate)