package love.yinlin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.app
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.BoxText
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationGrid
import love.yinlin.compose.ui.text.RachelRichParser
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.cs.*
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.extension.findAssign

@Stable
class ScreenMail : Screen() {
    private val provider = RachelStatefulProvider()

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
        provider.withLoading(loading) {
            page.newData(ApiMailGetMails.requestNull(app.config.userToken, page.default1, page.default, page.pageNum)!!.o1)
        }
    }

    private suspend fun requestMoreMails() {
        ApiMailGetMails.request(app.config.userToken, page.arg1, page.offset, page.pageNum) {
            page.moreData(it)
        }
    }

    private suspend fun onProcessMail(text: String, mid: Long, value: Boolean) {
        if (slot.confirm.open(content = text)) {
            slot.loading.open {
                ApiMailProcessMail.request(app.config.userToken, mid, value) { message ->
                    page.items.findAssign(predicate = { it.mid == mid }) { it.copy(processed = true) }
                    mailDetailsSheet.close()
                    slot.tip.info(message)
                }.errorTip
            }
        }
    }

    private suspend fun onDeleteMail(mid: Long) {
        if (slot.confirm.open(content = "删除此邮件?")) {
            slot.loading.open {
                ApiMailDeleteMail.request(app.config.userToken, mid) {
                    page.items.removeAll { it.mid == mid }
                    mailDetailsSheet.close()
                }.errorTip
            }
        }
    }

    override val title: String = "邮箱"

    override suspend fun initialize() {
        requestNewMails(true)
    }

    @Composable
    private fun MailItem(mail: Mail, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.eValue,
            shadowElevation = Theme.shadow.v3,
            border = if (mail.processed) null else BorderStroke(Theme.border.v7, Theme.color.primary),
            onClick = { mailDetailsSheet.open(mail) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxText(
                        text = mail.typeString,
                        color = when (mail.type) {
                            Mail.Type.INFO -> Theme.color.primary
                            Mail.Type.CONFIRM -> Theme.color.secondary
                            Mail.Type.DECISION -> Theme.color.tertiary
                            else -> LocalColor.current
                        }
                    )
                    SimpleEllipsisText(
                        text = mail.title,
                        style = Theme.typography.v7.bold,
                        modifier = Modifier.weight(1f)
                    )
                    SimpleEllipsisText(
                        text = mail.ts,
                        color = LocalColorVariant.current,
                        style = Theme.typography.v8
                    )
                }
                SimpleEllipsisText(
                    text = mail.content,
                    color = LocalColorVariant.current,
                    style = Theme.typography.v8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    override fun Content() {
        StatefulBox(
            provider = provider,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationGrid(
                items = page.items,
                key = { it.mid },
                columns = GridCells.Adaptive(Theme.size.cell1),
                state = gridState,
                canRefresh = true,
                canLoading = page.canLoading,
                onRefresh = { requestNewMails(false) },
                onLoading = ::requestMoreMails,
                modifier = Modifier.fillMaxSize(),
                contentPadding = Theme.padding.eValue,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.e)
            ) {
                MailItem(mail = it, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    override val fab: FAB = object : FAB() {
        private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

        override val action: FABAction = FABAction(
            iconProvider = { if (isScrollTop) Icons.Refresh else Icons.ArrowUpward },
            onClick = {
                if (isScrollTop) requestNewMails(true)
                else gridState.animateScrollToItem(0)
            }
        )
    }

    private val mailDetailsSheet = this land object : SheetContent<Mail>() {
        @Composable
        override fun Content(args: Mail) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                SimpleEllipsisText(
                    text = args.title,
                    style = Theme.typography.v6.bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                ActionScope.Right.Container(modifier = Modifier.fillMaxWidth()) {
                    if (args.withYes) {
                        TextButton(text = "接受", icon = Icons.Check, onClick = {
                            launch { onProcessMail("接受此邮件结果?", args.mid, true) }
                        })
                    }
                    if (args.withNo) {
                        TextButton(text = "拒绝", icon = Icons.Cancel, color = Theme.color.error, onClick = {
                            launch { onProcessMail("拒绝此邮件结果?", args.mid, false) }
                        })
                    }
                    if (args.processed) {
                        TextButton(text = "删除", icon = Icons.Delete, color = Theme.color.secondary, onClick = {
                            launch { onDeleteMail(args.mid) }
                        })
                    }
                }
                RachelRichText(
                    text = remember(args) { RachelRichParser.parse(args.content) },
                    onLinkClick = ::navigateScreenWebPage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}