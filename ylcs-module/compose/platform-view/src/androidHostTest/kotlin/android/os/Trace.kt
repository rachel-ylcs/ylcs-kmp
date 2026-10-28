package android.os

/**
 * JVM host tests have no Android systrace service. Compose Runtime calls this pair even when
 * rendering through the test applier, so supply only the tracing no-ops its lifecycle test needs.
 * This source set is test-only; all other Android framework calls retain their normal stub errors.
 */
@Suppress("UNUSED_PARAMETER")
object Trace {
    @JvmStatic
    fun beginSection(sectionName: String) {}

    @JvmStatic
    fun endSection() {}
}
