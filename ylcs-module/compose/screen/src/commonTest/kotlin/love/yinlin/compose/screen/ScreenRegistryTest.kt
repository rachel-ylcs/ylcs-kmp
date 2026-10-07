package love.yinlin.compose.screen

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.extension.makeArray
import love.yinlin.reflect.metaClassName
import kotlin.test.*

@OptIn(CompatibleRachelApi::class)
class ScreenRegistryTest {
    @Serializable
    private data class Payload(val id: Int, val title: String)

    private class Screen0 : TestScreen() {
        var clearCount: Int = 0
            private set

        override fun uninitialize() { ++clearCount }
    }

    private class Screen1(val first: Int) : TestScreen()
    private class Screen2(val first: Long, val second: String) : TestScreen()
    private class Screen3(val first: String, val second: Int?, val third: Boolean) : TestScreen()
    private class Screen4(val first: Int, val second: String?, val third: Payload, val fourth: List<Long>) : TestScreen()
    private class Screen5(val first: Long, val second: String?, val third: Boolean, val fourth: Payload?, val fifth: List<Long>?) : TestScreen()
    private class NullableScreen(val value: String?) : TestScreen()
    private class Custom404 : TestScreen()
    private class Other404 : TestScreen()

    private inline fun <reified S : ScreenModel> key(args: ScreenArgs = ScreenArgs.Empty): ScreenKey = ScreenKey(type = metaClassName<S>(), args = args)

    @Test
    fun constructorsWithZeroToFiveArgumentsDecodeInOrder() {
        val registry = ScreenRegistry {
            screen(::Screen0)
            screen(::Screen1)
            screen(::Screen2)
            screen(::Screen3)
            screen(::Screen4)
            screen(::Screen5)
        }
        val largeId = 9007199254740993L
        val payload = Payload(7, "payload")
        val ids: List<Long> = [10L, 20L]

        assertIs<Screen0>(registry.create(key<Screen0>()))
        assertEquals(1, assertIs<Screen1>(registry.create(key<Screen1>(ScreenArgs.build(1)))).first)

        val second = assertIs<Screen2>(registry.create(key<Screen2>(ScreenArgs.build(largeId, "second"))))
        assertEquals(largeId, second.first)
        assertEquals("second", second.second)

        val third = assertIs<Screen3>(registry.create(key<Screen3>(ScreenArgs.build<String, Int?, Boolean>("third", null, true))))
        assertEquals("third", third.first)
        assertNull(third.second)
        assertTrue(third.third)

        val fourth = assertIs<Screen4>(registry.create(key<Screen4>(ScreenArgs.build<Int, String?, Payload, List<Long>>(4, null, payload, ids))))
        assertEquals(4, fourth.first)
        assertNull(fourth.second)
        assertEquals(payload, fourth.third)
        assertEquals(ids, fourth.fourth)

        val fifth = assertIs<Screen5>(registry.create(key<Screen5>(ScreenArgs.build<Long, String?, Boolean, Payload?, List<Long>?>(largeId, "fifth", false, payload, ids))))
        assertEquals(largeId, fifth.first)
        assertEquals("fifth", fifth.second)
        assertFalse(fifth.third)
        assertEquals(payload, fifth.fourth)
        assertEquals(ids, fifth.fifth)
    }

    @Test
    fun registrationAndContainsDoNotInvokeFactories() {
        var factoryCalls = 0
        val registry = ScreenRegistry {
            screen<Screen0> {
                ++factoryCalls
                Screen0()
            }
        }

        repeat(3) {
            assertTrue(metaClassName<Screen0>() in registry)
            assertFalse(metaClassName<Screen1>() in registry)
        }
        assertEquals(0, factoryCalls)

        assertIs<Screen0>(registry.create(key<Screen0>()))
        assertEquals(1, factoryCalls)
    }

    @Test
    fun repeatedCreationDoesNotCacheScreenInstances() {
        var factoryCalls = 0
        val registry = ScreenRegistry {
            screen<Screen0> {
                ++factoryCalls
                Screen0()
            }
        }
        val key = key<Screen0>()

        val first = registry.create(key)
        val second = registry.create(key)

        assertNotSame(first, second)
        assertEquals(2, factoryCalls)
    }

    @Test
    fun nullableArgumentsDecodeNullEmptyAndNonNullValues() {
        val registry = ScreenRegistry {
            screen(::NullableScreen)
            screen(::Screen5)
        }
        val nullValue = assertIs<NullableScreen>(registry.create(key<NullableScreen>(ScreenArgs.build<String?>(null))))
        val emptyValue = assertIs<NullableScreen>(registry.create(key<NullableScreen>(ScreenArgs.build<String?>(""))))
        val value = assertIs<NullableScreen>(registry.create(key<NullableScreen>(ScreenArgs.build<String?>("value"))))

        assertNull(nullValue.value)
        assertEquals("", emptyValue.value)
        assertEquals("value", value.value)

        val screen = assertIs<Screen5>(registry.create(key<Screen5>(ScreenArgs.build<Long, String?, Boolean, Payload?, List<Long>?>(10L, null, false, null, null))))
        assertEquals(10L, screen.first)
        assertNull(screen.second)
        assertFalse(screen.third)
        assertNull(screen.fourth)
        assertNull(screen.fifth)
    }

    @Test
    fun keyJsonRoundTripRestoresFiveParameterFactoryArguments() {
        val registry = ScreenRegistry { screen(::Screen5) }
        val payload = Payload(42, "中文 | ?")
        val ids: List<Long> = [Long.MIN_VALUE, Long.MAX_VALUE]
        val original = key<Screen5>(ScreenArgs.build<Long, String?, Boolean, Payload?, List<Long>?>(Long.MAX_VALUE, "source|?", true, payload, ids))
        val restored = ScreenKey.parse(original.toString())
        val screen = assertIs<Screen5>(registry.create(restored))

        assertEquals(original, restored)
        assertEquals(original.id, restored.id)
        assertEquals(Long.MAX_VALUE, screen.first)
        assertEquals("source|?", screen.second)
        assertTrue(screen.third)
        assertEquals(payload, screen.fourth)
        assertEquals(ids, screen.fifth)
    }

    @Test
    fun unregisteredTypesDoNotCallOtherFactories() {
        var factoryCalls = 0
        val registry = ScreenRegistry {
            screen<Screen0> {
                ++factoryCalls
                Screen0()
            }
        }

        assertIs<Screen404>(registry.create(key<Screen1>(ScreenArgs.build(1))))
        assertEquals(0, factoryCalls)

        assertIs<Screen0>(registry.create(key<Screen0>()))
        assertEquals(1, factoryCalls)
    }

    @Test
    fun duplicateTypesAreRejectedBeforeFactoryInvocation() {
        var factoryCalls = 0

        assertFailsWith<IllegalArgumentException> {
            ScreenRegistry {
                screen<Screen0> {
                    ++factoryCalls
                    Screen0()
                }
                screen<Screen0> {
                    ++factoryCalls
                    Screen0()
                }
            }
        }
        assertEquals(0, factoryCalls)
    }

    @Test
    fun builtRegistryDoesNotObserveLaterBuilderRegistrations() {
        lateinit var builder: ScreenRegistry.Builder
        val registry = ScreenRegistry {
            builder = this
            screen(::Screen0)
            screen(::Screen1)
        }

        builder.screen(::Screen2)

        assertTrue(metaClassName<Screen0>() in registry)
        assertTrue(metaClassName<Screen1>() in registry)
        assertFalse(metaClassName<Screen2>() in registry)
        assertIs<Screen404>(registry.create(key<Screen2>(ScreenArgs.build(2L, "later"))))
        assertEquals(1, assertIs<Screen1>(registry.create(key<Screen1>(ScreenArgs.build(1)))).first)
    }

    @Test
    fun argumentCountMismatchDoesNotInvokeFactories() {
        var factoryCalls = 0
        val registry = ScreenRegistry {
            screen<Screen0> {
                ++factoryCalls
                Screen0()
            }
            screen<Screen5, Long, String?, Boolean, Payload?, List<Long>?> { first, second, third, fourth, fifth ->
                ++factoryCalls
                Screen5(first, second, third, fourth, fifth)
            }
        }
        val invalidArgs: List<ScreenArgs> = [
            ScreenArgs.Empty,
            ScreenArgs.build<Long, String?, Boolean, Payload?>(1L, null, true, null),
            ScreenArgs(makeArray { for (value in 1 .. 6) add(value) }),
        ]

        assertFailsWith<IllegalArgumentException> { registry.create(key<Screen0>(ScreenArgs.build(1))) }
        for (args in invalidArgs) {
            assertFailsWith<IllegalArgumentException> { registry.create(key<Screen5>(args)) }
        }
        assertEquals(0, factoryCalls)
    }

    @Test
    fun argumentDecodingFailureDoesNotInvokeFactory() {
        var factoryCalls = 0
        val registry = ScreenRegistry {
            screen<Screen1, Int> { value ->
                ++factoryCalls
                Screen1(value)
            }
        }

        assertFailsWith<SerializationException> { registry.create(key<Screen1>(ScreenArgs.build("not-an-int"))) }
        assertFailsWith<SerializationException> { registry.create(key<Screen1>(ScreenArgs.build<Int?>(null))) }
        assertEquals(0, factoryCalls)

        assertEquals(7, assertIs<Screen1>(registry.create(key<Screen1>(ScreenArgs.build(7)))).first)
        assertEquals(1, factoryCalls)
    }

    @Test
    fun factoryFailureIsPropagatedAndAllowsRetry() {
        val failure = IllegalStateException("factory")
        var shouldFail = true
        var factoryCalls = 0
        val registry = ScreenRegistry {
            screen<Screen0> {
                ++factoryCalls
                if (shouldFail) throw failure
                Screen0()
            }
        }
        val key = key<Screen0>()

        val caught = assertFailsWith<IllegalStateException> { registry.create(key) }
        assertSame(failure, caught)
        shouldFail = false

        assertIs<Screen0>(registry.create(key))
        assertEquals(2, factoryCalls)
    }

    @Test
    fun sharedRegistryCreatesIndependentScreensInDifferentStores() {
        val registry = ScreenRegistry { screen(::Screen0) }
        val key = key<Screen0>()

        ScreenStore().use { firstStore ->
            ScreenStore().use { secondStore ->
                val first = assertIs<Screen0>(firstStore.create(key.id) { registry.create(key) })
                val second = assertIs<Screen0>(secondStore.create(key.id) { registry.create(key) })

                assertNotSame(first, second)
                firstStore.close()

                assertEquals(1, first.clearCount)
                assertEquals(0, second.clearCount)
                assertNull(firstStore[key.id])
                assertSame(second, secondStore[key.id])

                secondStore.close()
                assertEquals(1, second.clearCount)
            }
        }
    }

    @Test
    fun failedArgumentDecodingRollsBackStoreAndSupportsRetry() {
        val registry = ScreenRegistry { screen(::Screen1) }
        val invalidKey = key<Screen1>(ScreenArgs.build("not-an-int"))

        ScreenStore().use { store ->
            assertFailsWith<SerializationException> { store.create(invalidKey.id) { registry.create(invalidKey) } }
            assertNull(store[invalidKey.id])
            assertFalse(invalidKey.id in store)

            val validKey = invalidKey.copy(args = ScreenArgs.build(7))
            val screen = assertIs<Screen1>(store.create(validKey.id) { registry.create(validKey) })

            assertEquals(7, screen.first)
            assertSame(screen, store[validKey.id])
            assertTrue(validKey.id in store)
        }
    }

    @Test
    fun default404CreatesIndependentInstancesWithoutDecodingMissingPageArguments() {
        val registry = ScreenRegistry { }
        val missing = key<Screen5>(ScreenArgs.build("these arguments do not match Screen5"))

        val first = assertIs<Screen404>(registry.create(missing))
        val second = assertIs<Screen404>(registry.create(missing))

        assertNotSame(first, second)
        assertFalse(missing.type in registry)
    }

    @Test
    fun custom404RegistrationDoesNotInvokeTheFactoryOrRegisterAnOrdinaryType() {
        var calls = 0
        val registry = ScreenRegistry {
            screen(::Screen0)
            screen404 {
                ++calls
                Custom404()
            }
        }

        assertEquals(0, calls)
        assertFalse(metaClassName<Custom404>() in registry)
        assertIs<Screen0>(registry.create(key<Screen0>()))
        assertEquals(0, calls)

        assertIs<Custom404>(registry.create(key<Screen1>(ScreenArgs.build(7))))
        assertIs<Custom404>(registry.create404())
        assertEquals(2, calls)
    }

    @Test
    fun custom404CanAlsoBeRegisteredAsAnOrdinaryScreen() {
        val registry = ScreenRegistry {
            screen(::Custom404)
            screen404(::Custom404)
        }

        assertTrue(metaClassName<Custom404>() in registry)
        assertIs<Custom404>(registry.create(key<Custom404>()))
        assertIs<Custom404>(registry.create(key<Screen1>()))
    }

    @Test
    fun last404ConfigurationWinsAndBuiltRegistryKeepsItsFactory() {
        lateinit var builder: ScreenRegistry.Builder
        val registry = ScreenRegistry {
            builder = this
            screen404(::Custom404)
            screen404(::Other404)
        }
        builder.screen404(::Custom404)

        assertIs<Other404>(registry.create404())
        assertIs<Other404>(registry.create(key<Screen1>()))
    }

    @Test
    fun registeredFactoryFailuresDoNotFallBackTo404() {
        var fallbackCalls = 0
        val failure = IllegalStateException("registered factory")
        val registry = ScreenRegistry {
            screen<Screen1, Int> { throw failure }
            screen404 { ++fallbackCalls; Custom404() }
        }

        assertFailsWith<IllegalArgumentException> { registry.create(key<Screen1>()) }
        assertFailsWith<SerializationException> { registry.create(key<Screen1>(ScreenArgs.build("invalid"))) }
        assertSame(failure, assertFailsWith<IllegalStateException> { registry.create(key<Screen1>(ScreenArgs.build(7))) })
        assertEquals(0, fallbackCalls)
    }

    @Test
    fun custom404FactoryFailurePropagatesAndCanBeRetried() {
        var shouldFail = true
        val failure = IllegalStateException("404 factory")
        val registry = ScreenRegistry {
            screen404 {
                if (shouldFail) throw failure
                Custom404()
            }
        }

        assertSame(failure, assertFailsWith<IllegalStateException> { registry.create(key<Screen1>()) })
        shouldFail = false
        assertIs<Custom404>(registry.create(key<Screen1>()))
    }

    @Test
    fun keysBindZeroToFiveArgumentFactoriesWithoutInvokingThem() {
        var calls = 0
        val registry = ScreenRegistry {
            screen<Screen0>({ ++calls; Screen0() }, key = "zero")
            screen(::Screen1, "one")
            screen(::Screen2, "two")
            screen(::Screen3, "three")
            screen(::Screen4, "four")
            screen(::Screen5, "five")
        }

        val bindings = [
            "zero" to metaClassName<Screen0>(),
            "one" to metaClassName<Screen1>(),
            "two" to metaClassName<Screen2>(),
            "three" to metaClassName<Screen3>(),
            "four" to metaClassName<Screen4>(),
            "five" to metaClassName<Screen5>(),
        ]
        for ([alias, type] in bindings) assertEquals(type, registry.resolveKey(alias))
        assertEquals(0, calls)
        assertEquals(7, assertIs<Screen1>(registry.create(key<Screen1>(ScreenArgs.build(7)))).first)
        assertEquals(0, calls)
    }

    @Test
    fun classNamesAndBoundKeysUseSeparateNamespaces() {
        val registry = ScreenRegistry {
            screen(::Screen0, key = metaClassName<Screen1>())
            screen(::Screen1, key = "detail")
            screen(::Screen2, key = null)
        }

        assertNull(registry.resolveKey(metaClassName<Screen0>()))
        assertNull(registry.resolveKey(metaClassName<Screen2>()))
        assertEquals(metaClassName<Screen0>(), registry.resolveKey(metaClassName<Screen1>()))
        assertEquals(metaClassName<Screen1>(), registry.resolveKey("detail"))
        assertNull(registry.resolveKey("Detail"))
        assertNull(registry.resolveKey(" detail "))
        assertIs<Screen1>(registry.create(key<Screen1>(ScreenArgs.build(7))))
        assertIs<Screen404>(registry.create(ScreenKey("detail", ScreenArgs.build(7))))
        assertIs<Screen404>(registry.create(key<Screen0>().copy(isUnboundKey = true)))
    }

    @Test
    fun failedDuplicateRegistrationsLeaveBothNamespacesUnchanged() {
        val registry = ScreenRegistry {
            screen(::Screen0, "shared")
            assertFailsWith<IllegalArgumentException> { screen(::Screen1, "shared") }
            screen(::Screen1, "one")
            assertFailsWith<IllegalArgumentException> { screen(::Screen0, "new") }
            screen(::Screen2, "new")
        }

        assertEquals(metaClassName<Screen0>(), registry.resolveKey("shared"))
        assertEquals(metaClassName<Screen1>(), registry.resolveKey("one"))
        assertEquals(metaClassName<Screen2>(), registry.resolveKey("new"))
        assertIs<Screen0>(registry.create(key<Screen0>()))
        assertEquals(7, assertIs<Screen1>(registry.create(key<Screen1>(ScreenArgs.build(7)))).first)
    }

    @Test
    fun blankKeysAreRejectedBeforeRegistrationAndNullDoesNotBindAKey() {
        val registry = ScreenRegistry {
            for (alias in ["", " ", "\t\n"]) {
                assertFailsWith<IllegalArgumentException> { screen(::Screen0, alias) }
            }
            screen(::Screen0, key = null)
        }

        assertTrue(metaClassName<Screen0>() in registry)
        assertNull(registry.resolveKey(metaClassName<Screen0>()))
        assertNull(registry.resolveKey(""))
        assertIs<Screen0>(registry.create(key<Screen0>()))
    }

    @Test
    fun builtRegistryDoesNotObserveLaterKeyBindings() {
        lateinit var builder: ScreenRegistry.Builder
        val registry = ScreenRegistry {
            builder = this
            screen(::Screen0, "zero")
        }
        builder.screen(::Screen1, "one")

        assertEquals(metaClassName<Screen0>(), registry.resolveKey("zero"))
        assertNull(registry.resolveKey("one"))
        assertFalse(metaClassName<Screen1>() in registry)
    }
}
