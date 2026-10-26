package org.jetbrains.compose.resources

import kotlin.test.Test
import kotlin.test.assertFailsWith

class NativeResourceRangeTest {
    @Test
    fun rejectsNegativeRangesBeforeOpeningTheFile() {
        assertFailsWith<IllegalArgumentException> { readResourceFile("missing-resource.bin", -1L, null) }
        assertFailsWith<IllegalArgumentException> { readResourceFile("missing-resource.bin", 0L, -1L) }
    }

    @Test
    fun aMissingFileCannotSucceedAsAnEmptyRead() {
        assertFailsWith<MissingResourceException> { readResourceFile("missing-resource.bin", 0L, 0L) }
        assertFailsWith<MissingResourceException> { readResourceFile("missing-resource.bin", Long.MAX_VALUE, 1L) }
    }
}
