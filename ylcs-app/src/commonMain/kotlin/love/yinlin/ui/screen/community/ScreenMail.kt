package love.yinlin.ui.screen.community

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
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.extension.findAssign
import love.yinlin.platform.app
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.layout.BoxState
import love.yinlin.ui.component.layout.PaginationArgs
import love.yinlin.ui.component.layout.PaginationGrid
import love.yinlin.ui.component.layout.StatefulBox
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.FloatingArgsSheet
import love.yinlin.ui.component.text.RichString
import love.yinlin.ui.component.text.RichText
import love.yinlin.ui.screen.common.ScreenWebpage.Companion.gotoWebPage

@Stable
class ScreenMail(model: AppModel) : CommonSubScreen(model) {
    private var state by mutableStateOf(BoxState.EMPTY)

    private val page = object : PaginationArgs<Mail, Long, Long, Boolean>(Long.MAX_VALUE, false) {
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
            shadowElevation = ThemeValue.Shadow.Surface,
            border = if (mail.processed) null else BorderStroke(ThemeValue.Border.Small, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clickable{ mailDetailsSheet.open(mail) }
                    .padding(ThemeValue.Padding.EqualValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
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
    override fun SubContent(device: Device) {
        StatefulBox(
            state = state,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationGrid(
                items = page.items,
                key = { it.mid },
                columns = GridCells.Adaptive(ThemeValue.Size.CardWidth),
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewMails(false) },
                onLoading = { requestMoreMails() },
                modifier = Modifier.fillMaxSize(),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace)
            ) {
                MailItem(
                    mail = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewMails(true) }
        else gridState.animateScrollToItem(0)
    }

    private val mailDetailsSheet = object : FloatingArgsSheet<Mail>() {
        @Composable
        override fun Content(args: Mail) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (args.withYes) RachelButton(
                        text = "接受",
                        icon = Icons.Outlined.CheckCircle,
                        onClick = {
                            launch { onProcessMail("接受此邮件结果?", args.mid, true) }
                        }
                    )
                    if (args.withNo) RachelButton(
                        text = "拒绝",
                        icon = Icons.Outlined.Cancel,
                        color = MaterialTheme.colorScheme.error,
                        onClick = {
                            launch { onProcessMail("拒绝此邮件结果?", args.mid, false) }
                        }
                    )
                    if (args.processed) RachelButton(
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
                    onLinkClick = { gotoWebPage(it) },
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                )
            }
        }
    }

    @Composable
    override fun Floating() {
        mailDetailsSheet.Land()
    }
}