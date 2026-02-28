package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.fastMap
import love.yinlin.app
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.StatefulStatus
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.LoadingTextButton
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingError
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.tpl.WeiboAPI

@Stable
class ScreenWeiboFollows : Screen() {
    private val provider = RachelStatefulProvider(StatefulStatus.Content)
    private var isLocal by mutableStateOf(true)
    private var searchResult by mutableRefStateOf(emptyList<WeiboUserInfo>())

    private suspend fun refreshLocalUser() {
        val weiboUsers = app.config.weiboUsers
        for ((index, user) in weiboUsers.withIndex()) {
            if (user.avatar.isEmpty()) {
                val data = WeiboAPI.getWeiboUser(user.id)
                if (data != null) weiboUsers[index] = data.info
            }
        }
        isLocal = true
    }

    private suspend fun onSearchWeiboUser() {
        searchDialog.open()?.let { key ->
            provider.withLoading {
                val result = WeiboAPI.searchWeiboUser(key)!!
                searchResult = result
                result.isNotEmpty()
            }
            isLocal = false
        }
    }

    override val title: String get() = if (isLocal) "微博关注" else "搜索结果"

    override suspend fun initialize() {
        refreshLocalUser()
    }

    @Composable
    override fun RowScope.LeftActions() {
        if (!isLocal) {
            LoadingIcon(icon = Icons.Clear, tip = "关闭", onClick = ::refreshLocalUser)
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Search, tip = "搜索", onClick = ::onSearchWeiboUser)
        if (isLocal) {
            Icon(icon = Icons.SwapVert, tip = "备份", onClick = importSheet::open)
        }
    }

    @Composable
    private fun WeiboUserItem(
        user: WeiboUserInfo,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Row(
            modifier = modifier.clickable(onClick = onClick).padding(Theme.padding.value),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9)
        ) {
            if (user.avatar.isEmpty()) CircleLoading.Content()
            else {
                WebImage(
                    uri = user.avatar,
                    key = remember { DateEx.TodayString },
                    contentScale = ContentScale.Crop,
                    circle = true,
                    modifier = Modifier.size(Theme.size.image9)
                )
            }
            SimpleEllipsisText(user.name)
        }
    }

    @Composable
    override fun Content() {
        StatefulBox(
            provider = provider,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell1),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = if (isLocal) app.config.weiboUsers.items else searchResult,
                    key = { it.id }
                ) {
                    WeiboUserItem(
                        user = it,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navigate(::ScreenWeiboUser, it.id) }
                    )
                }
            }
        }
    }

    private val searchDialog = this land DialogInput(hint = "输入微博用户昵称关键字", maxLength = 16)

    private val importSheet = this land object : Sheet() {
        @Composable
        override fun Content() {
            val state = rememberInputState()

            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                SimpleEllipsisText(text = "微博关注数据迁移")
                Input(
                    state = state,
                    hint = "关注列表(JSON格式)",
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LoadingTextButton(
                        text = "导入(叠加)",
                        icon = Icons.Download,
                        enabled = state.isSafe,
                        onClick = {
                            catchingError {
                                val localUsers = app.config.weiboUsers
                                val items = state.text.parseJsonValue<List<WeiboUserInfo>>()
                                for (item in items) {
                                    if (!localUsers.contains { it.id == item.id }) localUsers += WeiboUserInfo(item.id, item.name, "")
                                }
                                slot.tip.success("导入成功")
                            }?.let {
                                slot.tip.error("导入格式错误")
                            }
                        }
                    )
                    LoadingTextButton(
                        text = "导出",
                        icon = Icons.Upload,
                        onClick = {
                            catchingError {
                                state.text = app.config.weiboUsers.items.fastMap { WeiboUserInfo(it.id, it.name, "") }.toJsonString()
                            }?.let {
                                slot.tip.error(it.message ?: "导出失败")
                            }
                        }
                    )
                }
            }
        }
    }
}