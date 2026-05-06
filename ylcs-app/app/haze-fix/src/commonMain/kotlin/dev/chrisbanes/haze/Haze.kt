// Copyright 2023, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0

package dev.chrisbanes.haze

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

@Stable
class HazeState {
  var positionStrategy: HazePositionStrategy by mutableStateOf(HazePositionStrategy.Auto)

  internal var resolvedStrategy: HazePositionStrategy by mutableStateOf(HazePositionStrategy.Local)

  private val _areas = mutableStateListOf<HazeArea>()
  val areas: List<HazeArea> get() = _areas

  internal fun addArea(area: HazeArea) {
    _areas += area
  }

  internal fun removeArea(area: HazeArea) {
    _areas -= area
  }
}

internal fun resolvePositionStrategy(
  configured: HazePositionStrategy,
  areas: List<HazeArea>,
  windowId: Any?,
): HazePositionStrategy = when (configured) {
  HazePositionStrategy.Auto -> {
    if (areas.any { it.windowId != null && it.windowId != windowId }) {
      HazePositionStrategy.Screen
    } else {
      HazePositionStrategy.Local
    }
  }
  else -> configured
}

@Stable
class HazeArea internal constructor() {

   var position: Offset by mutableStateOf(Offset.Unspecified)
    internal set

   var size: Size by mutableStateOf(Size.Unspecified)
    internal set

   var zIndex: Float by mutableFloatStateOf(0f)
    internal set

   var key: Any? = null
    internal set

   var windowId: Any? = null
    internal set

  internal val preDrawListeners = mutableStateSetOf<OnPreDrawListener>()

  /**
   * Internal content [GraphicsLayer] used when capturing source content for effects.
   *
   * This is exposed for effect implementations and is not a stable public contract.
   */
   var contentLayer: GraphicsLayer? by mutableStateOf(null)
    internal set

  internal val bounds: Rect?
    get() = when {
      size.isSpecified && position.isSpecified -> Rect(position, size)
      else -> null
    }

  internal var contentDrawing: Boolean = false

   val isContentDrawing: Boolean
    get() = contentDrawing

   override fun toString(): String = buildString {
    append("HazeArea(")
    append("position=$position, ")
    append("size=$size, ")
    append("zIndex=$zIndex")
    append(")")
  }
}

internal fun HazeArea.reset() {
  position = Offset.Unspecified
  size = Size.Unspecified
  contentDrawing = false
}

internal fun interface OnPreDrawListener {
  operator fun invoke()
}

/**
 * Captures background content for [hazeEffect] child nodes, which will be drawn with a blur
 * in a 'glassmorphism' style.
 *
 * When running on Android 12 devices (and newer), usage of this API renders the corresponding composable
 * into a separate graphics layer. On older Android platforms, a translucent scrim will be drawn
 * instead.
 */
@Stable
fun Modifier.hazeSource(
  state: HazeState,
  zIndex: Float = 0f,
  key: Any? = null,
): Modifier = this then HazeSourceElement(state, zIndex, key)

internal data class HazeSourceElement(
  val state: HazeState,
  val zIndex: Float = 0f,
  val key: Any? = null,
) : ModifierNodeElement<HazeSourceNode>() {

  override fun create(): HazeSourceNode = HazeSourceNode(state = state, zIndex = zIndex, key = key)

  override fun update(node: HazeSourceNode) {
    node.state = state
    node.zIndex = zIndex
    node.key = key
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "hazeSource"
    properties["zIndex"] = zIndex
    properties["key"] = key
  }
}
