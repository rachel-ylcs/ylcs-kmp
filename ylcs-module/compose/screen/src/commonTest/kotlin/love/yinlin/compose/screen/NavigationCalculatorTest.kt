package love.yinlin.compose.screen

import love.yinlin.extension.makeArray
import kotlin.test.*

class NavigationCalculatorTest {
    private val policies = CreatePolicy.entries.flatMap { create ->
        ClearPolicy.entries.map { clear -> create + clear }
    }

    private fun arguments(value: String): ScreenArgs = ScreenArgs(makeArray { add(value) })

    private fun entry(type: String, value: String = "initial"): ScreenKey = ScreenKey(type, arguments(value))

    private fun sampleStack(): List<ScreenKey> = [
        entry("B", "first"),
        entry("A"),
        entry("B", "nearest"),
        entry("C"),
        entry("D")
    ]

    private fun largeArguments(): ScreenArgs = ScreenArgs(makeArray {
        obj {
            "payload" with "x".repeat(256 * 1024)
            arr("children") {
                repeat(32) { index ->
                    obj {
                        "index" with index
                        "enabled" with true
                    }
                }
            }
        }
    })

    private fun assertReferences(expected: List<ScreenKey>, actual: List<ScreenKey>, message: String = "entries") {
        assertEquals(expected.size, actual.size, message)
        for ([index, key] in expected.withIndex()) {
            assertSame(key, actual[index], "$message, index=$index")
        }
    }

    private data class Result(val after: List<ScreenKey>, val removed: List<ScreenKey>)

    private fun requireChange(before: List<ScreenKey>, plan: NavigationPlan, message: String = "change"): Result {
        (val fromIndex, val toIndex) = assertNotNull(plan.change, message)
        assertTrue(fromIndex in 0 .. before.size, message)
        assertTrue(toIndex in fromIndex.. before.size, message)
        val after = buildList {
            for (index in before.indices) {
                if (index !in fromIndex..<toIndex) add(before[index])
            }
            add(plan.key)
        }
        val firstRemoved = if (plan is NavigationPlan.Create) fromIndex else fromIndex + 1
        val removed = buildList {
            for (index in firstRemoved ..<toIndex) add(before[index])
        }
        return Result(after, removed)
    }

    private data class Case(
        val policy: NavigationPolicy,
        val retained: List<Int>,
        val removed: List<Int>,
        val creates: Boolean = false,
        val resumes: Boolean = false
    )

    @Test
    fun allPolicyCombinationsUseNearestMatch() {
        val before = sampleStack()
        val requestArgs = arguments("first")
        val cases = [
            Case(CreatePolicy.New + ClearPolicy.None, [0, 1, 2, 3, 4], [], creates = true),
            Case(CreatePolicy.New + ClearPolicy.Clear, [0, 1, 2, 3, 4], [], creates = true),
            Case(CreatePolicy.Replace + ClearPolicy.None, [0, 1, 3, 4], [2], creates = true),
            Case(CreatePolicy.Replace + ClearPolicy.Clear, [0, 1], [2, 3, 4], creates = true),
            Case(CreatePolicy.Move + ClearPolicy.None, [0, 1, 3, 4, 2], []),
            Case(CreatePolicy.Move + ClearPolicy.Clear, [0, 1, 2], [3, 4]),
            Case(CreatePolicy.Resume + ClearPolicy.None, [0, 1, 3, 4, 2], [], resumes = true),
            Case(CreatePolicy.Resume + ClearPolicy.Clear, [0, 1, 2], [3, 4], resumes = true)
        ]

        for (case in cases) {
            val message = case.policy.toString()
            val plan = NavigationCalculator.navigate(before, "B", requestArgs, case.policy)
            val key = when {
                case.creates -> assertIs<NavigationPlan.Create>(plan, message).key
                case.resumes -> {
                    (val key, val args) = assertIs<NavigationPlan.Resume>(plan, message)
                    assertSame(requestArgs, args, message)
                    key
                }
                else -> assertIs<NavigationPlan.Reuse>(plan, message).key
            }
            (val after, val removed) = requireChange(before, plan, message)
            val retained = case.retained.map { before[it] }
            val expectedAfter = if (case.creates) retained + key else retained

            assertReferences(expectedAfter, after, message)
            assertReferences(case.removed.map { before[it] }, removed, message)
            assertSame(after.last(), key, message)

            if (case.creates) {
                assertEquals("B", key.type, message)
                assertSame(requestArgs, key.args, message)
                assertFalse(before.any { it.id == key.id }, message)
            }
            else {
                assertSame(before[2], key, message)
                assertSame(before[2].args, key.args, message)
            }
        }
    }

    @Test
    fun missingMatchFallsBackToNewForEveryPolicy() {
        val before = sampleStack()
        val requestArgs = arguments("new")

        for (policy in policies) {
            val message = policy.toString()
            val plan = assertIs<NavigationPlan.Create>(NavigationCalculator.navigate(before, "Missing", requestArgs, policy), message)
            val key = plan.key
            (val after, val removed) = requireChange(before, plan)

            assertReferences(before + key, after, message)
            assertSame(key, after.last(), message)
            assertEquals("Missing", key.type, message)
            assertSame(requestArgs, key.args, message)
            assertFalse(before.any { it.id == key.id }, message)
            assertTrue(removed.isEmpty(), message)
        }
    }

    @Test
    fun emptyStackCreatesForEveryPolicy() {
        val before: List<ScreenKey> = []
        for (policy in policies) {
            val message = policy.toString()
            val plan = assertIs<NavigationPlan.Create>(NavigationCalculator.navigate(before, "B", policy = policy), message)
            val key = plan.key
            (val after, val removed) = requireChange(before, plan)

            assertReferences([key], after, message)
            assertEquals("B", key.type, message)
            assertSame(ScreenArgs.Empty, key.args, message)
            assertTrue(removed.isEmpty(), message)
        }
    }

    @Test
    fun defaultPolicyAllowsRepeatedEntriesWithIdenticalArguments() {
        val original = entry("B")
        val before = [original]
        val first = assertIs<NavigationPlan.Create>(NavigationCalculator.navigate(before, "B", original.args))
        (val after1 = after, val removed1 = removed) = requireChange(before, first)
        val second = assertIs<NavigationPlan.Create>(NavigationCalculator.navigate(after1, "B", original.args))
        (val after2 = after, val removed2 = removed) = requireChange(after1, second)
        val key1 = first.key
        val key2 = second.key

        assertReferences([original, key1, key2], after2)
        assertEquals(3, after2.map { it.id }.toSet().size)
        assertSame(original.args, key1.args)
        assertSame(original.args, key2.args)
        assertTrue(removed1.isEmpty())
        assertTrue(removed2.isEmpty())
    }

    @Test
    fun moveAtTopDoesNotChangeStackOrArguments() {
        val before = [entry("A"), entry("B")]

        for (clear in ClearPolicy.entries) {
            (val key, val change) = assertIs<NavigationPlan.Reuse>(NavigationCalculator.navigate(before, "B", arguments("ignored"), CreatePolicy.Move + clear))

            assertSame(before.last(), key)
            assertSame(before.last().args, key.args)
            assertNull(change)
        }
    }

    @Test
    fun resumeAtTopDeliversArgumentsWithoutChangingKey() {
        val before = [entry("A"), entry("B")]
        val requestArgs = arguments("updated")

        for (clear in ClearPolicy.entries) {
            (val key, val change, val args) = assertIs<NavigationPlan.Resume>(NavigationCalculator.navigate(before, "B", requestArgs, CreatePolicy.Resume + clear))

            assertSame(before.last(), key)
            assertSame(before.last().args, key.args)
            assertSame(requestArgs, args)
            assertNull(change)
        }
    }

    @Test
    fun resumeAtTopStillDeliversEmptyArguments() {
        val before = [entry("B")]

        for (clear in ClearPolicy.entries) {
            (val key, val change, val args) = assertIs<NavigationPlan.Resume>(NavigationCalculator.navigate(before, "B", policy = CreatePolicy.Resume + clear))

            assertSame(before.single(), key)
            assertSame(ScreenArgs.Empty, args)
            assertSame(before.single().args, key.args)
            assertNull(change)
        }
    }

    @Test
    fun replaceAtTopCreatesFreshIdentityEvenWithSameArguments() {
        val before = [entry("A"), entry("B")]
        val original = before.last()

        for (clear in ClearPolicy.entries) {
            val plan = assertIs<NavigationPlan.Create>(NavigationCalculator.navigate(before, "B", original.args, CreatePolicy.Replace + clear))
            val key = plan.key
            (val after, val removed) = requireChange(before, plan)

            assertReferences([before.first(), key], after)
            assertReferences([original], removed)
            assertSame(original.args, key.args)
            assertNotEquals(original.id, key.id)
        }
    }

    @Test
    fun nearestMatchDependsOnStackPositionRatherThanCreationOrder() {
        val older = entry("B", "older")
        val separator = entry("A")
        val newer = entry("B", "newer")
        val trailing = entry("C")
        val before = [newer, separator, older, trailing]

        val plan = assertIs<NavigationPlan.Reuse>(NavigationCalculator.navigate(before, "B", policy = CreatePolicy.Move + ClearPolicy.Clear))
        (val after, val removed) = requireChange(before, plan)

        assertSame(older, plan.key)
        assertReferences([newer, separator, older], after)
        assertReferences([trailing], removed)
    }

    @Test
    fun typeMatchingUsesFullName() {
        val user = entry("Screen.User")
        val details = entry("Screen.User.Details")
        val before = [user, details]
        val plan = assertIs<NavigationPlan.Reuse>(NavigationCalculator.navigate(before, "Screen.User", policy = CreatePolicy.Move + ClearPolicy.None))
        (val after, val removed) = requireChange(before, plan)

        assertSame(user, plan.key)
        assertReferences([details, user], after)
        assertTrue(removed.isEmpty())
    }

    @Test
    fun clearFromRootRemovesTheCorrectRange() {
        val root = entry("B")
        val c = entry("C")
        val d = entry("D")
        val before = [root, c, d]

        val plan = assertIs<NavigationPlan.Create>(NavigationCalculator.navigate(before, "B", policy = CreatePolicy.Replace + ClearPolicy.Clear))
        val key = plan.key
        (val after1 = after, val removed1 = removed) = requireChange(before, plan)
        assertReferences([key], after1)
        assertReferences([root, c, d], removed1)
        assertNotEquals(root.id, key.id)

        val move = assertIs<NavigationPlan.Reuse>(NavigationCalculator.navigate(before, "B", policy = CreatePolicy.Move + ClearPolicy.Clear))
        (val after, val removed) = requireChange(before, move)
        assertSame(root, move.key)
        assertReferences([root], after)
        assertReferences([c, d], removed)

        val resume = assertIs<NavigationPlan.Resume>(NavigationCalculator.navigate(before, "B", policy = CreatePolicy.Resume + ClearPolicy.Clear))
        (val after2 = after, val removed2 = removed) = requireChange(before, resume)
        assertSame(root, resume.key)
        assertSame(ScreenArgs.Empty, resume.args)
        assertReferences([root], after2)
        assertReferences([c, d], removed2)
    }

    @Test
    fun duplicateIdsAreRejectedEvenWhenOtherFieldsDiffer() {
        val original = entry("B")
        val invalidStacks = [
            [original, original],
            [original, original.copy(type = "Other", args = arguments("different"))]
        ]

        for (policy in policies) {
            for (before in invalidStacks) {
                assertFailsWith<IllegalArgumentException>(policy.toString()) {
                    NavigationCalculator.navigate(before, "B", policy = policy)
                }
            }
        }
    }

    @Test
    fun blankTypesAreRejected() {
        for (type in ["", " ", "\n\t"]) {
            assertFailsWith<IllegalArgumentException> {
                NavigationCalculator.navigate([], type)
            }
        }
    }

    @Test
    fun unrelatedLargeParametersKeepTheirOriginalReferences() {
        val largeArgs = largeArguments()
        val heavy = ScreenKey("Heavy", largeArgs)
        val payload = largeArgs.delegate[0]
        val before = [heavy, entry("B"), entry("C")]

        for (policy in policies) {
            val message = policy.toString()
            val plan = NavigationCalculator.navigate(before, "B", arguments("updated"), policy)
            val change = requireChange(before, plan, message)

            assertSame(heavy, change.after.first(), message)
            assertSame(largeArgs, change.after.first().args, message)
            assertSame(payload, change.after.first().args.delegate[0], message)
        }
    }

    @Test
    fun largeRequestArgumentsArePassedByReference() {
        val before = [entry("A"), entry("B"), entry("C")]
        val largeArgs = largeArguments()

        for (policy in policies) {
            val message = policy.toString()
            val plan = NavigationCalculator.navigate(before, "B", largeArgs, policy)

            when (policy.createPolicy) {
                CreatePolicy.New, CreatePolicy.Replace -> {
                    val create = assertIs<NavigationPlan.Create>(plan, message)
                    assertSame(largeArgs, create.key.args, message)
                }
                CreatePolicy.Move -> {
                    val reuse = assertIs<NavigationPlan.Reuse>(plan, message)
                    assertSame(before[1], reuse.key, message)
                    assertSame(before[1].args, reuse.key.args, message)
                }
                CreatePolicy.Resume -> {
                    (val key, val args) = assertIs<NavigationPlan.Resume>(plan, message)
                    assertSame(before[1], key, message)
                    assertSame(before[1].args, key.args, message)
                    assertSame(largeArgs, args, message)
                }
            }
        }
    }

    @Test
    fun largeStackPreservesEntryReferencesAndRemovalOrder() {
        val before = List(4096) { ScreenKey("Screen$it") }
        val targetIndex = before.size / 2
        val target = before[targetIndex]
        val move = assertIs<NavigationPlan.Reuse>(NavigationCalculator.navigate(before, target.type, policy = CreatePolicy.Move + ClearPolicy.None))
        val resume = assertIs<NavigationPlan.Resume>(NavigationCalculator.navigate(before, target.type, policy = CreatePolicy.Resume + ClearPolicy.Clear))
        (val after, val removed) = requireChange(before, move)
        (val after1 = after, val removed1 = removed) = requireChange(before, resume)

        assertReferences(buildList {
            addAll(before.take(targetIndex))
            addAll(before.drop(targetIndex + 1))
            add(target)
        }, after)
        assertReferences(before.take(targetIndex + 1), after1)
        assertReferences(before.drop(targetIndex + 1), removed1)
        assertSame(target, move.key)
        assertSame(target, resume.key)
        assertTrue(removed.isEmpty())
        assertSame(ScreenArgs.Empty, resume.args)
    }


    @Test
    fun newPlanOnlyDescribesAppendingInsteadOfBuildingAReplacementList() {
        val before = List(4096) { ScreenKey("Screen$it") }
        val args = largeArguments()
        (val key, val change) = assertIs<NavigationPlan.Create>(NavigationCalculator.navigate(before, "New", args))

        assertEquals(NavigationPlan.StackChange(before.size, before.size), change)
        assertSame(args, key.args)
        assertEquals(4096, before.size)
    }

    @Test
    fun reusePlanStoresOnlyIndicesAndTheTargetReference() {
        val target = entry("B")
        val before: MutableList<ScreenKey> = [entry("A"), target, entry("C")]
        (val key, val change) = assertIs<NavigationPlan.Reuse>(NavigationCalculator.navigate(before, "B", policy = CreatePolicy.Move + ClearPolicy.None))

        assertEquals(NavigationPlan.StackChange(1, 2), change)
        before.clear()

        assertSame(target, key)
        assertSame(target.args, key.args)
        assertEquals(NavigationPlan.StackChange(1, 2), change)
    }

    @Test
    fun unboundKeysAndClassTypesWithIdenticalNamesMatchIndependently() {
        val firstTyped = entry("A", "first typed")
        val firstUnbound = entry("A", "first unbound").copy(isUnboundKey = true)
        val lastTyped = entry("A", "last typed")
        val lastUnbound = entry("A", "last unbound").copy(isUnboundKey = true)
        val before = [firstTyped, firstUnbound, lastTyped, lastUnbound, entry("B")]

        for (policy in policies) for (isUnboundKey in [false, true]) {
            val target = if (isUnboundKey) lastUnbound else lastTyped
            val index = if (isUnboundKey) 3 else 2
            val args = arguments("updated")
            val plan = NavigationCalculator.navigate(before, "A", args, policy, isUnboundKey)

            assertEquals(isUnboundKey, plan.key.isUnboundKey)
            if (policy.createPolicy == CreatePolicy.New || policy.createPolicy == CreatePolicy.Replace) {
                assertIs<NavigationPlan.Create>(plan)
                assertNotEquals(target.id, plan.key.id)
                assertSame(args, plan.key.args)
            } else {
                assertSame(target, plan.key)
                assertSame(target.args, plan.key.args)
            }
            val fromIndex = if (policy.createPolicy == CreatePolicy.New) before.size else index
            val toIndex = if (policy.createPolicy == CreatePolicy.New || policy.clearPolicy == ClearPolicy.Clear) before.size else index + 1
            assertEquals(NavigationPlan.StackChange(fromIndex, toIndex), plan.change)
        }

        assertReferences([firstTyped, firstUnbound, lastTyped, lastUnbound], before.take(4))
    }
}
