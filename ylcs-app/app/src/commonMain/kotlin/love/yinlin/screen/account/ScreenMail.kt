package love.yinlin.screen.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.api.API
import love.yinlin.api.APIConfig
import love.yinlin.api.ClientAPI
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.Data
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.extension.findAssign
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationGrid
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.layout.BoxState
import love.yinlin.compose.ui.layout.StatefulBox
import love.yinlin.screen.common.ScreenWebpage
import love.yinlin.screen.community.BoxText
import love.yinlin.compose.ui.text.RichString
import love.yinlin.compose.ui.text.RichText

@Stable
class ScreenMail(manager: ScreenManager) : Screen(manager) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private val page = object : PaginationArgs<Mail, Long, Long, Boolean>(
        default = Long.MAX_VALUE,
        default1 = false,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: Mail): Long = item.mid
        override fun offset(item: Mail): Long = item.mid
        override fun arg1(item: Mail): Boolean = item.processed
    }

    private val gridState = LazyGridState()

    private suspend fun requestNewMails(loading: Boolean) {
        if (state != BoxState.LOADING) {
            if (loading) state = BoxState.LOADING
            val result = ClientAPI.request(
                route = API.User.Mail.GetMails,
                data = API.User.Mail.GetMails.Request(
                    token = app.config.userToken,
                    num = page.pageNum
                )
            )
            state = if (result is Data.Success) {
                if (page.newData(result.data)) BoxState.CONTENT else BoxState.EMPTY
            } else BoxState.NETWORK_ERROR
        }
    }

    private suspend fun requestMoreMails() {
        val result = ClientAPI.request(
            route = API.User.Mail.GetMails,
            data = API.User.Mail.GetMails.Request(
                token = app.config.userToken,
                isProcessed = page.arg1,
                mid = page.offset,
                num = page.pageNum
            )
        )
        if (result is Data.Success) page.moreData(result.data)
    }

    private suspend fun onProcessMail(text: String, mid: Long, value: Boolean) {
        if (slot.confirm.openSuspend(content = text)) {
            slot.loading.openSuspend()
            val result = ClientAPI.request(
                route = API.User.Mail.ProcessMail,
                data = API.User.Mail.ProcessMail.Request(
                    token = app.config.userToken,
                    mid = mid,
                    confirm = value
                )
            )
            when (result) {
                is Data.Success -> {
                    page.items.findAssign(predicate = { it.mid == mid }) {
                        it.copy(processed = true)
                    }
                    mailDetailsSheet.close()
                }
                is Data.Failure -> slot.tip.error(result.message)
            }
            slot.loading.close()
        }
    }

    private suspend fun onDeleteMail(mid: Long) {
        if (slot.confirm.openSuspend(content = "删除此邮件?")) {
            slot.loading.openSuspend()
            val result = ClientAPI.request(
                route = API.User.Mail.DeleteMail,
                data = API.User.Mail.DeleteMail.Request(
                    token = app.config.userToken,
                    mid = mid
                )
            )
            when (result) {
                is Data.Success -> {
                    page.items.removeAll { it.mid == mid }
                    mailDetailsSheet.close()
                }
                is Data.Failure -> slot.tip.error(result.message)
            }
            slot.loading.close()
        }
    }

    @Composable
    private fun MailItem(
        mail: Mail,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier,
            shadowElevation = CustomTheme.shadow.surface,
            border = if (mail.processed) null else BorderStroke(CustomTheme.border.small, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clickable{ mailDetailsSheet.open(mail) }
                    .padding(CustomTheme.padding.equalValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxText(
                        text = mail.typeString,
                        color = when (mail.type) {
                            Mail.Type.INFO -> MaterialTheme.colorScheme.primary
                            Mail.Type.CONFIRM -> MaterialTheme.colorScheme.secondary
                            Mail.Type.DECISION -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = mail.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = mail.ts,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = mail.content,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override suspend fun initialize() {
        requestNewMails(true)
    }

    override val title: String = "邮箱"

    @Composable
    override fun Content(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationGrid(
                items = page.items,
                key = { it.mid },
                columns = GridCells.Adaptive(CustomTheme.size.cardWidth),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewMails(false) },
                onLoading = { requestMoreMails() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = CustomTheme.padding.equalValue,
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace)
            ) {
                MailItem(
                    mail = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector by derivedStateOf { if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward }

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewMails(true) }
        else gridState.animateScrollToItem(0)
    }

    private val mailDetailsSheet = this land object : FloatingArgsSheet<Mail>() {
        @Composable
        override fun Content(args: Mail) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Text(
                    text = args.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (args.withYes) ClickText(
                        text = "接受",
                        icon = Icons.Outlined.CheckCircle,
                        onClick = {
                            launch { onProcessMail("接受此邮件结果?", args.mid, true) }
                        }
                    )
                    if (args.withNo) ClickText(
                        text = "拒绝",
                        icon = Icons.Outlined.Cancel,
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            launch { onProcessMail("拒绝此邮件结果?", args.mid, false) }
                        }
                    )
                    if (args.processed) ClickText(
                        text = "删除",
                        icon = Icons.Outlined.Delete,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            launch { onDeleteMail(args.mid) }
                        }
                    )
                }
                RichText(
                    text = remember(args) { RichString.parse(args.content) },
                    onLinkClick = { uri -> ScreenWebpage.gotoWebPage(uri) { navigate(::ScreenWebpage, it) } },
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                )
            }
        }
    }
}