package org.jetbrains.skia.icu

/** Canonical BCP 47 identity and direction obtained from the bundled ICU data. */
class LocaleInfo(
    val languageTag: String,
    val language: String,
    val script: String,
    val region: String,
    val isRightToLeft: Boolean
)
