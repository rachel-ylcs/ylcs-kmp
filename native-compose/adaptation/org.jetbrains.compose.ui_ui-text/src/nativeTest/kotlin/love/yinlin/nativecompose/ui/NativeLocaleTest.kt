@file:Suppress("DEPRECATION")

package love.yinlin.nativecompose.ui

import androidx.compose.ui.text.WeakReference
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.intl.isRtl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NativeLocaleTest {
    @Test
    fun platformLocaleDelegateProvidesANonEmptyCurrentList() {
        val locales = LocaleList.current
        assertTrue(locales.isNotEmpty())
        assertEquals(locales.first(), Locale.current)
        locales.forEach { assertTrue(it.toLanguageTag().isNotBlank()) }
    }

    @Test
    fun localeDirectionDistinguishesArabicHebrewAndLatin() {
        assertTrue(Locale("ar-SA").isRtl())
        assertTrue(Locale("he-IL").isRtl())
        assertFalse(Locale("en-US").isRtl())
    }

    @Test
    fun nativeWeakReferenceCanBeExplicitlyCleared() {
        val value = Any()
        val reference = WeakReference(value)
        assertSame(value, reference.get())
        reference.clear()
        assertNull(reference.get())
    }
}
