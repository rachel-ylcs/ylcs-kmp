package love.yinlin.compose.screen

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.rememberImmersivePadding
import love.yinlin.compose.window.DeepLink
import love.yinlin.extension.Array
import love.yinlin.extension.DateEx
import love.yinlin.extension.parseJson

@Stable
class ScreenManager @PublishedApi internal constructor(savedBackStack: List<String>) {
    @Stable
    companion object {
        @PublishedApi
        internal val saver = listSaver(
            save = { it.backStack },
            restore = { ScreenManager(it) }
        )

        @PublishedApi
        internal inline fun <reified Main : BasicScreen> build(): ScreenManager = ScreenManager(listOf(Route<Main>().build()))

        @PublishedApi
        @Composable
        internal inline fun <reified Main : BasicScreen> rememberBuild(): ScreenManager = rememberSaveable(saver = saver) { build<Main>() }

        @Composable
        inline fun <reified Main : BasicScreen> Navigation(
            modifier: Modifier = Modifier.fillMaxSize(),
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
            crossinline builder: ScreenMap.() -> Unit,
        ) {
            val manager = rememberBuild<Main>()
            val map = remember { ScreenMap().also(builder) }

            DeepLink.Register(deeplink, manager)

            CompositionLocalProvider(LocalImmersivePadding provides rememberImmersivePadding()) {
                NavDisplay(
                    backStack = manager.backStack,
                    modifier = modifier,
                    transitionSpec = transitionSpecProvider,
                    popTransitionSpec = transitionSpecProvider,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = { manager.registerScreen(map, it) }
                )
            }
        }
    }

    @PublishedApi
    internal val backStack = savedBackStack.toMutableStateList()

    @PublishedApi
    internal fun registerScreen(
        map: ScreenMap,
        route: String
    ): NavEntry<String> = NavEntry(key = route, contentKey = route) { navRoute ->
        Box {
            viewModel {
                val (screenName, uniqueId, argsText) = Route.parse(navRoute)
                val factory = map.screens[screenName]
                val screen = if (factory == null) map.screen404Factory() else factory(argsText.parseJson.Array)
                ScreenGlobal.VMMap[uniqueId] = screen
                screen.uniqueId = uniqueId
                screen.manager = this@ScreenManager
                screen.launch { screen.initialize() }
                screen
            }.ComposedUI()
        }
    }

    @PublishedApi
    internal fun unregisterScreen(id: String) {
        ScreenGlobal.VMMap -= id
    }

    val topScreen: BasicScreen get() {
        val last = backStack.last()
        val (_, uniqueId, _) = Route.parse(last)
        return ScreenGlobal.VMMap[uniqueId]!!
    }

    inline fun <reified S : BasicScreen> findScreen(): S? {
        for (route in backStack.asReversed()) {
            val (screenName, uniqueId, _) = Route.parse(route)
            if (Route.key<S>() == screenName) return ScreenGlobal.VMMap[uniqueId] as? S
        }
        return null
    }

    inline fun <reified S : BasicScreen> findScreens(): List<S> {
        val target = mutableListOf<S>()
        for (route in backStack.asReversed()) {
            val (screenName, uniqueId, _) = Route.parse(route)
            if (Route.key<S>() == screenName) (ScreenGlobal.VMMap[uniqueId] as? S)?.let { target += it }
        }
        return target
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    private var lastNavigateTime = 0L

    @PublishedApi
    internal fun navigate(dstRoute: String, navigationPolicy: NavigationPolicy) {
        // 防抖处理
        val currentTime = DateEx.CurrentLong
        if (currentTime - lastNavigateTime <= 300L) return
        lastNavigateTime = currentTime

        val (createPolicy, clearPolicy) = navigationPolicy

        if (createPolicy == CreatePolicy.New) {
            // 新页面直接创建
            backStack += dstRoute
        }
        else {
            val (dstScreenName, _, dstArgs) = Route.parse(dstRoute)
            var target: Pair<Int, BasicScreen>? = null

            // 查找最晚加入导航栈的同类页面
            for (index in backStack.indices.reversed()) {
                val route = backStack[index]
                val (screenName, uniqueId, _) = Route.parse(route)
                if (screenName == dstScreenName) {
                    val screen = ScreenGlobal.VMMap[uniqueId]
                    if (screen != null) {
                        target = index to screen
                        break
                    }
                }
            }

            // 判断非直接添加的导航策略
            if (createPolicy == CreatePolicy.Replace) {
                if (target != null) {
                    // 检查是否需要清理沿途页面
                    val targetIndex = target.first
                    val endIndex = if (clearPolicy == ClearPolicy.Clear) backStack.size else targetIndex + 1
                    backStack.removeRange(targetIndex, endIndex)
                }
                // 替换无论如何都有新页面及作用域产生
                backStack += dstRoute
            }
            else { // Move 或 Resume
                // 只有存在目标页才替换
                if (target != null) {
                    val targetIndex = target.first
                    val oldRoute = backStack[targetIndex]
                    // 如果当前页就是目标页则无需移动
                    if (targetIndex != backStack.lastIndex) {
                        // 检查是否需要清理沿途页面
                        val endIndex = if (clearPolicy == ClearPolicy.Clear) backStack.size else targetIndex + 1
                        backStack.removeRange(targetIndex, endIndex)
                        backStack += oldRoute
                    }

                    // 唤醒策略
                    if (createPolicy == CreatePolicy.Resume) {
                        val screen = target.second
                        // 注入唤醒参数
                        screen.lastResumeArgs = dstArgs
                        screen.launch { screen.resume() }
                    }
                }
                else backStack += dstRoute // 否则直接创建
            }
        }
    }

    @Suppress("unused")
    inline fun <reified S : BasicScreen> navigate(metaConstructor: () -> S, policy: NavigationPolicy = NavigationPolicy.New) {
        navigate(Route<S>().build(), policy)
    }

    @Suppress("unused")
    inline fun <reified S : BasicScreen, reified A1> navigate(metaConstructor: (A1) -> S, arg1: A1, policy: NavigationPolicy = NavigationPolicy.New) {
        navigate(Route<S>().arg(arg1).build(), policy)
    }

    @Suppress("unused")
    inline fun <reified S : BasicScreen, reified A1, reified A2> navigate(metaConstructor: (A1, A2) -> S, arg1: A1, arg2: A2, policy: NavigationPolicy = NavigationPolicy.New) {
        navigate(Route<S>().arg(arg1).arg(arg2).build(), policy)
    }

    @Suppress("unused")
    inline fun <reified S : BasicScreen, reified A1, reified A2, reified A3> navigate(metaConstructor: (A1, A2, A3) -> S, arg1: A1, arg2: A2, arg3: A3, policy: NavigationPolicy = NavigationPolicy.New) {
        navigate(Route<S>().arg(arg1).arg(arg2).arg(arg3).build(), policy)
    }
}