package love.yinlin.compose.screen

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.cancel
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.annotation.LooseTyped
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.rememberImmersivePadding
import love.yinlin.compose.window.DeepLink
import love.yinlin.extension.cleaning
import love.yinlin.reflect.metaClassName

@OptIn(CompatibleRachelApi::class)
@Suppress("unused")
@Stable
class ScreenManager internal constructor(private val registry: ScreenRegistry, initialBackStack: List<ScreenKey>) : ViewModel() {
    internal val store = ScreenStore()

    internal val backStack: List<ScreenKey> field = mutableStateListOf<ScreenKey>()

    internal var isClosed: Boolean by mutableStateOf(false)
        private set

    private var changing: Boolean = false

    internal constructor(registry: ScreenRegistry) : this(registry, [])

    init {
        addCloseable(AutoCloseable(::closeScope))
        runCatching {
            val initialKeys = buildList { addAll(initialBackStack) }
            change {
                val ids: MutableSet<ScreenID> = []

                for ((val id) in initialKeys) {
                    require(ids.add(id)) { "The navigation stack cannot contain duplicate ScreenID." }
                }

                for (key in initialKeys) createScreen(key)

                backStack.addAll(initialKeys)
            }
            for (key in initialKeys) {
                if (isActive(key.id)) store[key.id]?.initializeOnce()
            }
        }.exceptionOrNull()?.let { failure ->
            val _ = runCatching(::closeScope)
            val _ = runCatching(viewModelScope::cancel)
            throw failure
        }
    }

    private inline fun <T> change(block: () -> T): T {
        require(!isClosed) { "The screen manager is closed." }
        require(!changing) { "The navigation stack is already being changed." }
        changing = true
        return cleaning({ changing = false}, block)
    }

    private fun finish(removed: List<ScreenKey>, action: () -> Unit = {}) {
        var failure: Throwable? = null
        for (key in removed) {
            val error = runCatching { store.remove(key.id) }.exceptionOrNull()
            failure = failure ?: error
        }
        val callbackFailure = runCatching(action).exceptionOrNull()
        (failure ?: callbackFailure)?.let { throw it }
    }

    private fun closeScope() {
        if (isClosed) return
        isClosed = true
        backStack.clear()
        store.close()
    }

    private fun createScreen(key: ScreenKey, fallback: Boolean = false): ScreenModel = store.create(key.id) {
        val screen = if (fallback) registry.create404() else registry.create(key)
        screen.attach(this, key)
        screen
    }

    internal fun navEntry(key: ScreenKey): NavEntry<ScreenKey> {
        val screen = store[key.id] ?: change {
            val activeKey = backStack.find { (val type, val id, val isUnboundKey) ->
                id == key.id && type == key.type && isUnboundKey == key.isUnboundKey
            }
            requireNotNull(activeKey) { "The screen entry is no longer active: ${key.id}." }
            createScreen(activeKey, fallback = true)
        }.also(ScreenModel::initializeOnce)
        require(screen.manager === this && screen.screenKey.type == key.type && screen.screenKey.isUnboundKey == key.isUnboundKey) { "The screen key does not match its managed instance: ${key.id}." }
        return screen.navEntry
    }

    private fun execute(buildPlan: () -> NavigationPlan) {
        var removed: List<ScreenKey> = []
        val plan = change {
            val plan = buildPlan()
            if (plan is NavigationPlan.Create) createScreen(plan.key)
            else require(plan.key.id in store) { "The screen entry is no longer active: ${plan.key.id}." }

            plan.change?.let { (val fromIndex, val toIndex) ->
                val firstRemoved = if (plan is NavigationPlan.Create) fromIndex else fromIndex + 1
                if (firstRemoved < toIndex) removed = buildList {
                    for (index in firstRemoved  ..< toIndex) add(backStack[index])
                }
                runCatching {
                    Snapshot.withMutableSnapshot {
                        if (fromIndex < toIndex) backStack.removeRange(fromIndex, toIndex)
                        backStack.add(plan.key)
                    }
                }.exceptionOrNull()?.let { failure ->
                    if (plan is NavigationPlan.Create) {
                        val _ = runCatching { store.remove(plan.key.id) }
                    }
                    throw failure
                }
            }
            plan
        }

        val screen = requireNotNull(store[plan.key.id]) { "The screen entry does not exist: ${plan.key.id}." }
        finish(removed) {
            if (isActive(plan.key.id)) {
                screen.initializeOnce()
                if (plan is NavigationPlan.Resume && isActive(plan.key.id)) screen.dispatchResume(plan.args)
            }
        }
    }

    internal fun isActive(id: ScreenID): Boolean = backStack.any { it.id == id }

    @PublishedApi
    internal fun navigateTo(type: String, args: ScreenArgs = ScreenArgs.Empty, policy: NavigationPolicy = NavigationPolicy.Default, isUnboundKey: Boolean = false): Unit = execute {
        NavigationCalculator.navigate(backStack, type, args, policy, isUnboundKey)
    }

    @PublishedApi
    internal fun navigateKey(key: String, args: ScreenArgs, policy: NavigationPolicy) {
        val type = registry.resolveKey(key)
        navigateTo(type ?: key, args, policy, isUnboundKey = type == null)
    }

    inline fun <reified S : ScreenModel> navigate(metaConstructor: () -> S, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateTo(metaClassName<S>(), ScreenArgs.Empty, policy)

    inline fun <reified S : ScreenModel, reified A1> navigate(metaConstructor: (A1) -> S, arg1: A1, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateTo(metaClassName<S>(), ScreenArgs.build(arg1), policy)

    inline fun <reified S : ScreenModel, reified A1, reified A2> navigate(metaConstructor: (A1, A2) -> S, arg1: A1, arg2: A2, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateTo(metaClassName<S>(), ScreenArgs.build(arg1, arg2), policy)

    inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3> navigate(metaConstructor: (A1, A2, A3) -> S, arg1: A1, arg2: A2, arg3: A3, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateTo(metaClassName<S>(), ScreenArgs.build(arg1, arg2, arg3), policy)

    inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3, reified A4> navigate(metaConstructor: (A1, A2, A3, A4) -> S, arg1: A1, arg2: A2, arg3: A3, arg4: A4, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateTo(metaClassName<S>(), ScreenArgs.build(arg1, arg2, arg3, arg4), policy)

    inline fun <reified S : ScreenModel, reified A1, reified A2, reified A3, reified A4, reified A5> navigate(metaConstructor: (A1, A2, A3, A4, A5) -> S, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateTo(metaClassName<S>(), ScreenArgs.build(arg1, arg2, arg3, arg4, arg5), policy)

    @LooseTyped
    fun navigate(key: String, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateKey(key, ScreenArgs.Empty, policy)

    @LooseTyped
    inline fun <reified A1> navigate(key: String, arg1: A1, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateKey(key, ScreenArgs.build(arg1), policy)

    @LooseTyped
    inline fun <reified A1, reified A2> navigate(key: String, arg1: A1, arg2: A2, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateKey(key, ScreenArgs.build(arg1, arg2), policy)

    @LooseTyped
    inline fun <reified A1, reified A2, reified A3> navigate(key: String, arg1: A1, arg2: A2, arg3: A3, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateKey(key, ScreenArgs.build(arg1, arg2, arg3), policy)

    @LooseTyped
    inline fun <reified A1, reified A2, reified A3, reified A4> navigate(key: String, arg1: A1, arg2: A2, arg3: A3, arg4: A4, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateKey(key, ScreenArgs.build(arg1, arg2, arg3, arg4), policy)

    @LooseTyped
    inline fun <reified A1, reified A2, reified A3, reified A4, reified A5> navigate(key: String, arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, policy: NavigationPolicy = NavigationPolicy.Default): Unit =
        navigateKey(key, ScreenArgs.build(arg1, arg2, arg3, arg4, arg5), policy)

    internal fun pop(): Boolean {
        val removed = change { if (backStack.size <= 1) null else backStack.removeAt(backStack.lastIndex) } ?: return false
        finish([removed])
        return true
    }

    @Stable
    companion object {
        internal fun obtainScreenManager(
            store: ViewModelStore,
            managerId: String,
            mainType: String,
            registry: () -> ScreenRegistry
        ): ScreenManager = ViewModelProvider.create(
            store = store,
            factory = viewModelFactory {
                addInitializer(ScreenManager::class) {
                    ScreenManager(registry(), [ScreenKey(type = mainType)])
                }
            }
        )[managerId, ScreenManager::class]

        @Composable
        inline fun <reified Main : ScreenModel> Navigation(
            modifier: Modifier = Modifier,
            deeplink: DeepLink<ScreenManager> = DeepLink.default(),
            noinline transitionSpecProvider: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(durationMillis = 400)
                ) togetherWith slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(durationMillis = 400)
                )
            },
            noinline builder: ScreenRegistry.Builder.() -> Unit
        ) = Navigation(
            mainType = metaClassName<Main>(),
            deeplink = deeplink,
            transitionSpecProvider = transitionSpecProvider,
            modifier = modifier,
            builder = builder
        )

        @PublishedApi
        @Composable
        internal fun Navigation(
            mainType: String,
            modifier: Modifier,
            deeplink: DeepLink<ScreenManager>,
            transitionSpecProvider: AnimatedContentTransitionScope<*>.() -> ContentTransform,
            builder: ScreenRegistry.Builder.() -> Unit
        ) {
            // 根导航使用 Activity / Window 的 Owner；嵌套导航使用父页面的 Owner
            val owner = requireNotNull(LocalViewModelStoreOwner.current) { "ScreenManager requires a ViewModelStoreOwner." }
            val managerId = rememberSaveable { ScreenID().toString() }
            val manager = remember(owner.viewModelStore, managerId) {
                obtainScreenManager(owner.viewModelStore, managerId, mainType) { ScreenRegistry(builder) }
            }

            if (manager.isClosed || manager.backStack.isEmpty()) Box(modifier = modifier)
            else {
                DeepLink.Register(deeplink, manager)

                val immersivePadding = rememberImmersivePadding()
                CompositionLocalProvider(LocalImmersivePadding provides immersivePadding) {
                    NavDisplay(
                        backStack = manager.backStack,
                        modifier = modifier,
                        onBack = manager::pop,
                        transitionSpec = transitionSpecProvider,
                        popTransitionSpec = transitionSpecProvider,
                        entryDecorators = [
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(manager.store.provider),
                        ],
                        entryProvider = { manager.navEntry(it) }
                    )
                }
            }
        }
    }
}
