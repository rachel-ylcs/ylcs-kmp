package androidx.compose.ui.graphics

/** Borrows this Skia canvas as a Compose canvas; the caller retains ownership of the Skia canvas. */
expect fun org.jetbrains.skia.Canvas.asComposeCanvas(): Canvas
