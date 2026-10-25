package org.jetbrains.skia

/** Returns this filter's 4 by 5 RGBA matrix, or null when the filter has no matrix representation. */
expect fun ColorFilter.colorMatrixOrNull(): ColorMatrix?
