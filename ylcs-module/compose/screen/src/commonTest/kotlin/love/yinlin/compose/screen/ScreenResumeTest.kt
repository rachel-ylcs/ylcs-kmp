package love.yinlin.compose.screen

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import kotlin.test.*

class ScreenResumeTest {
    private class CallbackScreen(private val action: CallbackScreen.() -> Unit) : TestScreen() {
        var resumeCount = 0

        override fun resume() {
            ++resumeCount
            action()
        }

        inline fun decodeResume(block: () -> Unit) = withResume(block)

        inline fun <reified A1> decodeResume(block: (A1) -> Unit) = withResume(block)

        inline fun <reified A1, reified A2> decodeResume(block: (A1, A2) -> Unit) = withResume(block)

        inline fun <reified A1, reified A2, reified A3> decodeResume(block: (A1, A2, A3) -> Unit) = withResume(block)

        inline fun <reified A1, reified A2, reified A3, reified A4> decodeResume(block: (A1, A2, A3, A4) -> Unit) = withResume(block)

        inline fun <reified A1, reified A2, reified A3, reified A4, reified A5> decodeResume(block: (A1, A2, A3, A4, A5) -> Unit) = withResume(block)
    }

    private class ReturningScreen : TestScreen() {
        var resumeCount = 0
        var completedCount = 0

        override fun resume() {
            withResume { value: Int ->
                ++resumeCount
                if (value < 0) return
            }
            ++completedCount
        }
    }

    @Serializable
    private data class Payload(val title: String)

    @Test
    fun withResumeDecodesZeroToFiveArguments() {
        val results: MutableList<List<Any?>> = []
        val payload = Payload("中文 | ?")
        val numbers: List<Int> = [1, 2]
        val zero = CallbackScreen { decodeResume { results.add([]) } }
        val one = CallbackScreen { decodeResume { a: Int -> results.add([a]) } }
        val two = CallbackScreen { decodeResume { a: Int, b: String? -> results.add([a, b]) } }
        val three = CallbackScreen { decodeResume { a: Int, b: String, c: Boolean -> results.add([a, b, c]) } }
        val four = CallbackScreen { decodeResume { a: Int, b: String?, c: Boolean, d: Payload -> results.add([a, b, c, d]) } }
        val five = CallbackScreen { decodeResume { a: Long, b: String?, c: Boolean, d: Payload?, e: List<Int>? -> results.add([a, b, c, d, e]) } }
        val requests = [
            zero to ScreenArgs.Empty,
            one to ScreenArgs.build(7),
            two to ScreenArgs.build<Int, String?>(2, null),
            three to ScreenArgs.build(3, "three", true),
            four to ScreenArgs.build<Int, String?, Boolean, Payload>(4, null, false, payload),
            five to ScreenArgs.build<Long, String?, Boolean, Payload?, List<Int>?>(Long.MAX_VALUE, "five", true, payload, numbers)
        ]

        for ([screen, args] in requests) {
            screen.dispatchResume(args)
            assertEquals(1, screen.resumeCount)
        }

        val expected: List<List<Any?>> = [
            [], [7], [2, null], [3, "three", true], [4, null, false, payload], [Long.MAX_VALUE, "five", true, payload, numbers]
        ]
        assertEquals(expected, results)
    }

    @Test
    fun nullableParametersSupportBothNullAndNonNullValues() {
        val results: MutableList<List<Any?>> = []
        val payload = Payload("payload")
        val numbers: List<Int> = [1, 2]
        val screen = CallbackScreen {
            decodeResume { a: Int?, b: String?, c: Boolean?, d: Payload?, e: List<Int>? ->
                results.add([a, b, c, d, e])
            }
        }

        screen.dispatchResume(ScreenArgs.build<Int?, String?, Boolean?, Payload?, List<Int>?>(null, null, null, null, null))
        screen.dispatchResume(ScreenArgs.build<Int?, String?, Boolean?, Payload?, List<Int>?>(7, "text", true, payload, numbers))

        val expected: List<List<Any?>> = [[null, null, null, null, null], [7, "text", true, payload, numbers]]
        assertEquals(expected, results)
    }

    @Test
    fun withResumeRequiresAnActiveCallbackEvenForEmptyArguments() {
        var calls = 0
        val screen = CallbackScreen { decodeResume { ++calls } }

        assertFailsWith<IllegalArgumentException> { screen.decodeResume { ++calls } }
        screen.dispatchResume(ScreenArgs.Empty)
        assertEquals(1, calls)
        assertFailsWith<IllegalArgumentException> { screen.decodeResume { ++calls } }
        assertEquals(1, calls)
    }

    @Test
    fun missingArgumentsDoNotInvokeTheBlockOrRetainArguments() {
        var calls = 0
        val screen = CallbackScreen { decodeResume { _: Int, _: String? -> ++calls } }
        val invalid = [ScreenArgs.Empty, ScreenArgs.build(1)]

        for (args in invalid) {
            assertFailsWith<IllegalArgumentException> { screen.dispatchResume(args) }
            assertFailsWith<IllegalArgumentException> { screen.decodeResume<Int, String?> { _, _ -> ++calls } }
        }
        assertEquals(0, calls)

        screen.dispatchResume(ScreenArgs.build<Int, String?>(1, null))
        assertEquals(1, calls)
    }

    @Test
    fun withResumeCanIgnoreTrailingArguments() {
        var value: Int? = null
        var zeroCalls = 0
        val screen = CallbackScreen { decodeResume { first: Int -> value = first } }
        val zero = CallbackScreen { decodeResume { ++zeroCalls } }
        val args = ScreenArgs.build(7, "unused", true)

        screen.dispatchResume(args)
        zero.dispatchResume(args)

        assertEquals(7, value)
        assertEquals(1, zeroCalls)
        assertFailsWith<IllegalArgumentException> { screen.decodeResume<Int> { } }
        assertFailsWith<IllegalArgumentException> { zero.decodeResume { } }
    }

    @Test
    fun decodingFailureDoesNotInvokeTheBlockOrRetainArguments() {
        var calls = 0
        val screen = CallbackScreen { decodeResume { _: Int, _: String -> ++calls } }
        val invalid = [ScreenArgs.build("not-an-int", "text"), ScreenArgs.build<Int, String?>(7, null)]

        for (args in invalid) {
            assertFailsWith<SerializationException> { screen.dispatchResume(args) }
            assertFailsWith<IllegalArgumentException> { screen.decodeResume<Int, String> { _, _ -> ++calls } }
        }
        assertEquals(0, calls)

        screen.dispatchResume(ScreenArgs.build(7, "text"))
        assertEquals(1, calls)
    }

    @Test
    fun blockFailureClearsArgumentsAndAllowsTheNextResume() {
        val values: MutableList<Int> = []
        val failure = IllegalStateException("callback")
        var shouldFail = true
        val screen = CallbackScreen {
            decodeResume { value: Int ->
                values += value
                if (shouldFail) throw failure
            }
        }

        val caught = assertFailsWith<IllegalStateException> { screen.dispatchResume(ScreenArgs.build(1)) }
        assertSame(failure, caught)
        assertFailsWith<IllegalArgumentException> { screen.decodeResume<Int> { values += it } }

        shouldFail = false
        screen.dispatchResume(ScreenArgs.build(2))
        assertEquals([1, 2], values)
    }

    @Test
    fun callbacksCanIgnoreArgumentsWithoutRetainingThem() {
        val screen = CallbackScreen { }
        val defaultScreen = @Suppress("StaticFieldLeak") object : TestScreen() { }

        screen.dispatchResume(ScreenArgs.build(Payload("ignored")))
        defaultScreen.dispatchResume(ScreenArgs.build(Payload("default")))

        assertEquals(1, screen.resumeCount)
        assertFailsWith<IllegalArgumentException> { screen.decodeResume<Payload> { } }
        assertFailsWith<IllegalArgumentException> { defaultScreen.requireResumeArgs(1) }
    }

    @Test
    fun earlyReturnFromWithResumeStillClearsArguments() {
        val screen = ReturningScreen()

        screen.dispatchResume(ScreenArgs.build(-1))
        assertEquals(1, screen.resumeCount)
        assertEquals(0, screen.completedCount)
        assertFailsWith<IllegalArgumentException> { screen.requireResumeArgs(1) }

        screen.dispatchResume(ScreenArgs.build(1))
        assertEquals(2, screen.resumeCount)
        assertEquals(1, screen.completedCount)
    }

    @Test
    fun nestedResumeBeforeDecodingRestoresOuterArguments() {
        val values: MutableList<Int> = []
        var depth = 0
        val screen = CallbackScreen {
            if (depth == 0) {
                ++depth
                cleaning({ --depth }) {
                    dispatchResume(ScreenArgs.build(2))
                }
            }
            decodeResume { value: Int -> values += value }
        }

        screen.dispatchResume(ScreenArgs.build(1))

        assertEquals([2, 1], values)
        assertEquals(2, screen.resumeCount)
        assertFailsWith<IllegalArgumentException> { screen.decodeResume<Int> { } }
    }

    @Test
    fun nestedResumeInsideTheBlockRestoresOuterArguments() {
        val values: MutableList<Int> = []
        val screen = CallbackScreen {
            decodeResume { value: Int ->
                values += value
                if (value == 1) {
                    dispatchResume(ScreenArgs.build(2))
                    decodeResume { outer: Int -> values += outer }
                }
            }
        }

        screen.dispatchResume(ScreenArgs.build(1))

        assertEquals([1, 2, 1], values)
        assertEquals(2, screen.resumeCount)
        assertFailsWith<IllegalArgumentException> { screen.decodeResume<Int> { } }
    }

    @Test
    fun nestedDecodingOrCallbackFailureRestoresOuterArguments() {
        for (decodeFailure in [true, false]) {
            val values: MutableList<Int> = []
            val failure = IllegalStateException("nested callback")
            var caught: Throwable? = null
            var depth = 0
            val screen = CallbackScreen {
                if (depth == 0) {
                    ++depth
                    cleaning({ --depth }) {
                        val inner = if (decodeFailure) ScreenArgs.build("not-an-int") else ScreenArgs.build(2)
                        caught = catchingError { dispatchResume(inner) }
                    }
                    decodeResume { value: Int -> values += value }
                }
                else decodeResume { _: Int -> throw failure }
            }

            screen.dispatchResume(ScreenArgs.build(1))

            if (decodeFailure) assertIs<SerializationException>(caught)
            else assertSame(failure, caught)
            assertEquals([1], values)
            assertFailsWith<IllegalArgumentException> { screen.decodeResume<Int> { } }
        }
    }

    @Test
    fun decodedValuesCanBeCapturedForWorkAfterTheCallback() {
        var pending: (() -> Pair<Int, String?>)? = null
        val screen = CallbackScreen {
            decodeResume { a: Int, b: String? -> pending = { a to b } }
        }

        screen.dispatchResume(ScreenArgs.build<Int, String?>(1, null))
        val first = assertNotNull(pending)
        screen.dispatchResume(ScreenArgs.build(2, "next"))
        val second = assertNotNull(pending)

        assertEquals(1 to null, first())
        assertEquals(2 to "next", second())
        assertFailsWith<IllegalArgumentException> { screen.decodeResume<Int, String?> { _, _ -> } }
    }
}
