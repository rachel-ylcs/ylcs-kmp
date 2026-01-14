package love.yinlin.screen.msg.weibo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastMap
import love.yinlin.api.WeiboAPI
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.compose.ui.image.LoadingCircle
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.rememberTextInputState
import love.yinlin.compose.ui.input.LoadingClickText
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.floating.FloatingSheet
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.extension.catchingError

@Composable
private fun WeiboUserItem(
    user: WeiboUserInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onClick).padding(CustomTheme.padding.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
    ) {
        if (user.avatar.isEmpty()) {
            LoadingCircle(size = CustomTheme.size.microImage)
        }
        else {
            WebImage(
                uri = user.avatar,
                key = remember { DateEx.TodayString },
                contentScale = ContentScale.Crop,
                circle = true,
                modifier = Modifier.size(CustomTheme.size.microImage)
            )
        }
        Text(
            text = user.name,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Stable
class ScreenWeiboFollows(manager: ScreenManager) : Screen(manager) {
    private var isLocal by mutableStateOf(true)
    private var state by mutableStateOf(BoxState.CONTENT)
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
        searchDialog.openSuspend()?.let { key ->
            state = BoxState.LOADING
            val result = WeiboAPI.searchWeiboUser(key)
            if (result != null) {
                searchResult = result
                state = if (result.isEmpty()) BoxState.EMPTY else BoxState.CONTENT
            }
            else state = BoxState.NETWORK_ERROR
            isLocal = false
        }
    }

    override suspend fun initialize() {
        refreshLocalUser()
    }

    override val title: String by derivedStateOf { if (isLocal) "微博关注" else "搜索结果" }

    @Composable
    override fun ActionScope.LeftActions() {
        if (!isLocal) {
            ActionSuspend(Icons.Outlined.Close) {
                refreshLocalUser()
            }
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(Icons.Outlined.Search, "搜索") {
            onSearchWeiboUser()
        }
        if (isLocal) {
            Action(Icons.Outlined.SwapVert, "备份") {
                importSheet.open()
            }
        }
    }

    @Composable
    override fun Content(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(CustomTheme.size.cardWidth),
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

    private val importSheet = this land object : FloatingSheet() {
        @Composable
        override fun Content() {
            val state = rememberTextInputState()

            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Text(text = "微博关注数据迁移")
                TextInput(
                    state = state,
                    hint = "关注列表(JSON格式)",
                    maxLines = 6,
                    clearButton = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LoadingClickText(
                        text = "导入(叠加)",
                        icon = Icons.Outlined.Download,
                        enabled = state.ok,
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
                    LoadingClickText(
                        text = "导出",
                        icon = Icons.Outlined.Upload,
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

    private val searchDialog = this land FloatingDialogInput(hint = "输入微博用户昵称关键字", maxLength = 16)
}