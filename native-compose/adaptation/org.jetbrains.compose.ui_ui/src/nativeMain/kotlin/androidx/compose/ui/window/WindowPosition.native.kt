/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.window

import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp

fun WindowPosition(x: Dp, y: Dp) = WindowPosition.Absolute(x, y)

fun WindowPosition(alignment: Alignment) = WindowPosition.Aligned(alignment)

/** Desktop position in Compose dp. Wayland compositors control window positions themselves. */
@Stable
sealed class WindowPosition {
    abstract val x: Dp
    abstract val y: Dp
    abstract val isSpecified: Boolean

    /** Leaves the initial position to the window system. Only valid before attachment. */
    @Stable
    data object PlatformDefault : WindowPosition() {
        override val x: Dp get() = Dp.Unspecified
        override val y: Dp get() = Dp.Unspecified
        override val isSpecified: Boolean get() = false
    }

    /** Aligns to the current display's usable area, excluding taskbars and docks. */
    @Stable
    data class Aligned(val alignment: Alignment) : WindowPosition() {
        override val x: Dp get() = Dp.Unspecified
        override val y: Dp get() = Dp.Unspecified
        override val isSpecified: Boolean get() = false
    }

    @Stable
    data class Absolute(override val x: Dp, override val y: Dp) : WindowPosition() {
        override val isSpecified: Boolean get() = true
    }
}
