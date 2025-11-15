package love.yinlin.screen.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import love.yinlin.api.*
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.floating.FloatingDialogChoice
import love.yinlin.compose.ui.floating.FloatingSheet
import love.yinlin.data.compose.Picture
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.SubComment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.rachel.topic.TopicDetails
import love.yinlin.extension.DateEx
import love.yinlin.extension.findAssign
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.SplitActionLayout
import love.yinlin.compose.ui.platform.UnsupportedPlatformComponent
import love.yinlin.screen.common.ScreenImagePreview
import love.yinlin.screen.common.ScreenMain
import love.yinlin.screen.common.ScreenWebpage
import love.yinlin.compose.ui.layout.*
import love.yinlin.compose.ui.text.RichEditor
import love.yinlin.compose.ui.text.RichEditorState
import love.yinlin.compose.ui.text.RichString
import love.yinlin.compose.ui.text.RichText

@Composable
private fun CoinLayout(
    num: Int,
    modifier: Modifier = Modifier,
    onClick: (Int) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = CustomTheme.shadow.tonal,
        shadowElevation = CustomTheme.shadow.surface
    ) {
        Column(
            modifier = Modifier.clickable{ onClick(num) }.padding(CustomTheme.padding.equalValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            Row (
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(num) {
                    MiniIcon(
                        icon = Icons.Filled.Paid,
                        color = when (num) {
                            1 -> MaterialTheme.colorScheme.tertiary
                            2 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        size = CustomTheme.size.mediumIcon * (1 - num / 20f)
                    )
                }
            }
            if (num == UserConstraint.MIN_COIN_REWARD) {
                Text(
                    text = "作者获赠1银币",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$num 银币",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Stable
private data class AtInfo(val uid: Int, val name: String) {
    override fun equals(other: Any?): Boolean = other is AtInfo && other.uid == uid
    override fun hashCode(): Int = uid

    val avatarPath: String by lazy { ServerRes.Users.User(uid).avatar.url }
}

@Composable
private fun AtUserItem(
    info: AtInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick).padding(CustomTheme.padding.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
    ) {
        WebImage(
            uri = info.avatarPath,
            key = remember { DateEx.TodayString },
            contentScale = ContentScale.Crop,
            circle = true,
            modifier = Modifier.size(CustomTheme.size.microImage)
        )
        Text(
            text = info.name,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Stable
class ScreenTopic(manager: ScreenManager, currentTopic: Topic) : Screen(manager) {
    private val subScreenDiscovery = manager.get<ScreenMain>().get<SubScreenDiscovery>()

    private var details: TopicDetails? by mutableRefStateOf(null)
    private var topic: Topic by mutableRefStateOf(currentTopic)

    private val pageComments = object : PaginationArgs<Comment, Int, Int, Boolean>(
        default = 0,
        default1 = true,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: Comment): Int = item.cid
        override fun offset(item: Comment): Int = item.cid
        override fun arg1(item: Comment): Boolean = item.isTop
    }

    private val listState = LazyListState()

    private var currentSendComment: Comment? by mutableRefStateOf(null)

    private val sendCommentState = object : RichEditorState() {
        override val useImage: Boolean = true
        override val useAt: Boolean = true

        @Composable
        override fun AtLayout(modifier: Modifier) {
            val userList = remember(details, pageComments.items) {
                val userSet = mutableSetOf(AtInfo(topic.uid, topic.name))
                pageComments.items.fastForEach { userSet.add(AtInfo(it.uid, it.name)) }
                userSet -= AtInfo(app.config.userProfile?.uid ?: 0, "")
                userSet.toList()
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(CustomTheme.size.cellWidth),
                modifier = modifier
            ) {
                items(
                    items = userList,
                    key = { it.uid }
                ) {
                    AtUserItem(
                        info = it,
                        onClick = { closeLayout("[at|${it.uid}|@${it.name}]") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        @Composable
        override fun ImageLayout(modifier: Modifier) {
            UnsupportedPlatformComponent(modifier = modifier)
        }
    }

    private suspend fun requestMoreComments() {
        ApiTopicGetTopicComments.request(topic.tid, topic.rawSection, pageComments.arg1, pageComments.offset, pageComments.pageNum) {
            pageComments.moreData(it)
        }
    }

    private suspend fun requestSubComments(pid: Int, cid: Int, num: Int): List<SubComment>? {
        var data: List<SubComment>? = null
        ApiTopicGetTopicSubComments.request(pid, topic.rawSection, cid, num) { data = it }
        return data
    }

    private fun onAvatarClick(uid: Int) {
        subScreenDiscovery.onUserAvatarClick(uid)
    }

    private fun onImageClick(images: List<Picture>, current: Int) {
        navigate(::ScreenImagePreview, images, current)
    }

    private suspend fun onChangeTopicIsTop(value: Boolean) {
        ApiTopicUpdateTopicTop.request(app.config.userToken, topic.tid, value) {
            topic = topic.copy(isTop = value)
        }.errorTip
    }

    private suspend fun onDeleteTopic() {
        if (slot.confirm.openSuspend(content = "删除主题?")) {
            ApiTopicDeleteTopic.request(app.config.userToken, topic.tid) {
                subScreenDiscovery.page.items.removeAll { it.tid == topic.tid }
                pop()
            }.errorTip
        }
    }

    private suspend fun onMoveTopic() {
        moveTopicDialog.openSuspend()?.let { index ->
            val newSection = Comment.Section.MovableSection[index]
            details?.let { oldDetails ->
                val oldSection = oldDetails.section
                if (newSection == oldSection) {
                    slot.tip.warning("不能与原板块相同哦")
                    return@let
                }
                ApiTopicMoveTopic.request(app.config.userToken, topic.tid, newSection) {
                    if (subScreenDiscovery.currentSection == oldSection) subScreenDiscovery.page.items.removeAll { it.tid == topic.tid }
                    details = oldDetails.copy(section = newSection)
                    slot.tip.success("移动成功")
                }.errorTip
            }
        }
    }

    private suspend fun onSendCoin(num: Int) {
        if (app.config.userProfile?.uid == topic.uid) {
            slot.tip.warning("不能给自己投币哦")
            return
        }
        ApiTopicSendCoin.request(app.config.userToken, topic.uid, topic.tid, num) {
            subScreenDiscovery.page.items.findAssign(predicate = { it.tid == topic.tid }) {
                it.copy(coinNum = it.coinNum + num)
            }
            app.config.userProfile?.let {
                app.config.userProfile = it.copy(coin = it.coin - num)
            }
            slot.tip.success("投币成功")
        }.errorTip
    }

    private suspend fun onSendComment(content: String): Boolean {
        val user = app.config.userProfile
        return if (user != null) {
            // 回复主题
            val target = currentSendComment
            if (target == null) {
                ApiTopicSendComment.request(app.config.userToken, topic.tid, topic.rawSection, content) { cid ->
                    subScreenDiscovery.page.items.findAssign(predicate = { it.tid == topic.tid }) {
                        it.copy(commentNum = it.commentNum + 1)
                    }
                    pageComments.items += Comment(
                        cid = cid,
                        uid = user.uid,
                        ts = DateEx.CurrentString,
                        content = content,
                        isTop = false,
                        subCommentNum = 0,
                        name = user.name,
                        label = user.label,
                        exp = user.exp
                    )
                    listState.animateScrollToItem(pageComments.items.size - 1)
                }.errorTip == null
            }
            else { // 回复评论
                ApiTopicSendSubComment.request(app.config.userToken, topic.tid, target.cid, topic.rawSection, content) {
                    pageComments.items.findAssign(predicate = { it.cid == target.cid }) {
                        it.copy(subCommentNum = it.subCommentNum + 1)
                    }
                    currentSendComment = null
                }.errorTip == null
            }
        } else false
    }

    private suspend fun onChangeCommentIsTop(cid: Int, isTop: Boolean) {
        ApiTopicUpdateCommentTop.request(app.config.userToken, topic.tid, cid, topic.rawSection, isTop) {
            pageComments.items.findAssign(predicate = { it.cid == cid }) {
                it.copy(isTop = isTop)
            }
            pageComments.items.sort()
            listState.scrollToItem(pageComments.items.indexOfFirst { it.cid == cid })
        }.errorTip
    }

    private suspend fun onDeleteComment(cid: Int) {
        if (slot.confirm.openSuspend(content = "删除回复(楼中楼会同步删除)")) {
            ApiTopicDeleteComment.request(app.config.userToken, topic.tid, cid, topic.rawSection) {
                subScreenDiscovery.page.items.findAssign(predicate = { it.tid == topic.tid }) {
                    it.copy(commentNum = it.commentNum - 1)
                }
                pageComments.items.removeAll { it.cid == cid }
            }.errorTip
        }
    }

    private suspend fun onDeleteSubComment(pid: Int, cid: Int, onDelete: suspend () -> Unit) {
        if (slot.confirm.openSuspend(content = "删除回复")) {
            ApiTopicDeleteSubComment.request(app.config.userToken, topic.tid, pid, topic.rawSection, cid, onDelete).errorTip
        }
    }

    @Composable
    private fun RichTextLayout(
        text: RichString,
        modifier: Modifier = Modifier
    ) {
        RichText(
            text = text,
            fixLineHeight = true,
            onLinkClick = { uri -> ScreenWebpage.gotoWebPage(uri) { navigate(::ScreenWebpage, it) } },
            onTopicClick = {},
            onAtClick = { navigate(::ScreenUserCard, it.toIntOrNull() ?: 0) },
            modifier = modifier
        )
    }

    @Composable
    private fun TopicLayout(details: TopicDetails, modifier: Modifier = Modifier) {
        val pics = remember(details, topic) { details.pics.fastMap { Picture(topic.picPath(it).url) } }

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
        ) {
            UserBar(
                avatar = remember(topic) { topic.avatarPath.url },
                name = topic.name,
                time = details.ts,
                label = details.label,
                level = details.level,
                onAvatarClick = { onAvatarClick(topic.uid) }
            )
            SelectionContainer {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            RichTextLayout(
                text = remember(details) { RichString.parse(details.content) },
                modifier = Modifier.fillMaxWidth()
            )
            if (pics.isNotEmpty()) {
                NineGrid(
                    modifier = Modifier.fillMaxWidth(),
                    pics = pics,
                    onImageClick = { onImageClick(pics, it) }
                ) { modifier, pic, contentScale, onClick ->
                    WebImage(
                        uri = pic.image,
                        contentScale = contentScale,
                        modifier = modifier,
                        onClick = onClick
                    )
                }
            }
        }
    }

    @Composable
    private fun CommentBar(comment: Comment, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            UserBar(
                avatar = remember(comment) { comment.avatarPath.url },
                name = comment.name,
                time = comment.ts,
                label = comment.label,
                level = comment.level,
                onAvatarClick = { onAvatarClick(comment.uid) }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (comment.isTop) BoxText(text = "置顶", color = MaterialTheme.colorScheme.primary)
                if (comment.uid == topic.uid) BoxText(text = "楼主", color = MaterialTheme.colorScheme.secondary)
            }
            RichTextLayout(
                text = remember(comment) { RichString.parse(comment.content) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = CustomTheme.padding.verticalSpace),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (comment.subCommentNum > 0) {
                    Box(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ">> 查看${comment.subCommentNum}条回复",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { subCommentSheet.open(comment) }
                                .padding(CustomTheme.padding.littleValue)
                        )
                    }
                }
                Text(
                    text = "回复",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { currentSendComment = comment }
                        .padding(CustomTheme.padding.littleValue)
                )
                app.config.userProfile?.let { user ->
                    if (user.canUpdateCommentTop(topic.uid)) {
                        Text(
                            text = if (comment.isTop) "取消置顶" else "置顶",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable {
                                launch { onChangeCommentIsTop(comment.cid, !comment.isTop) }
                            }.padding(CustomTheme.padding.littleValue)
                        )
                    }
                    if (user.canDeleteComment(topic.uid, comment.uid)) {
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable {
                                launch { onDeleteComment(comment.cid) }
                            }.padding(CustomTheme.padding.littleValue)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SubCommentBar(
        subComment: SubComment,
        parentComment: Comment,
        modifier: Modifier = Modifier,
        onDelete: suspend () -> Unit
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            UserBar(
                avatar = remember(subComment) { subComment.avatarPath.url },
                name = subComment.name,
                time = subComment.ts,
                label = subComment.label,
                level = subComment.level,
                onAvatarClick = {
                    subCommentSheet.close()
                    onAvatarClick(subComment.uid)
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = CustomTheme.padding.verticalSpace),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (subComment.uid == topic.uid) BoxText(text = "楼主", color = MaterialTheme.colorScheme.secondary)
                if (subComment.uid == parentComment.uid) BoxText(text = "层主", color = MaterialTheme.colorScheme.tertiary)
                app.config.userProfile?.let { user ->
                    if (user.canDeleteComment(topic.uid, subComment.uid)) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "删除",
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.clickable {
                                    launch { onDeleteSubComment(parentComment.cid, subComment.cid, onDelete) }
                                }.padding(CustomTheme.padding.littleValue)
                            )
                        }
                    }
                }
            }
            RichTextLayout(
                text = remember(subComment) { RichString.parse(subComment.content) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun BottomLayout(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            SplitActionLayout(
                modifier = Modifier.fillMaxWidth(),
                left = {
                    Action(
                        icon = Icons.Filled.Home,
                        tip = "回复楼主",
                        enabled = currentSendComment != null
                    ) {
                        currentSendComment = null
                    }
                },
                right = {
                    Action(Icons.Filled.Paid, "投币") {
                        sendCoinSheet.open()
                    }
                    ActionSuspend(
                        icon = Icons.AutoMirrored.Filled.Send,
                        tip = "发送",
                        enabled = sendCommentState.ok
                    ) {
                        if (onSendComment(sendCommentState.richString.toString())) {
                            sendCommentState.text = ""
                            sendCommentState.closePreview()
                        }
                    }
                }
            )
            RichEditor(
                state = sendCommentState,
                hint = remember(currentSendComment) { "回复 @${currentSendComment?.name ?: "主题"}" },
                maxLength = 256,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    private fun Portrait(details: TopicDetails) {
        PauseLoading(listState)

        PaginationColumn(
            items = pageComments.items,
            key = { it.cid },
            state = listState,
            canRefresh = false,
            canLoading = pageComments.canLoading,
            onLoading = { requestMoreComments() },
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            header = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = CustomTheme.shadow.tonal
                ) {
                    TopicLayout(
                        details = details,
                        modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = CustomTheme.padding.verticalSpace))
            },
            itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalSpace)
        ) {
            CommentBar(
                comment = it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalSpace)
            )
        }
    }

    @Composable
    private fun Landscape(details: TopicDetails) {
        Row(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .padding(LocalImmersivePadding.current.withoutEnd)
                    .width(CustomTheme.size.panelWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                tonalElevation = CustomTheme.shadow.tonal
            ) {
                TopicLayout(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
                    details = details
                )
            }
            VerticalDivider()

            PauseLoading(listState)

            PaginationColumn(
                items = pageComments.items,
                key = { it.cid },
                state = listState,
                canRefresh = false,
                canLoading = pageComments.canLoading,
                onLoading = { requestMoreComments() },
                itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalSpace),
                modifier = Modifier
                    .padding(LocalImmersivePadding.current.withoutStart)
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(CustomTheme.padding.value)
            ) {
                CommentBar(
                    comment = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override val title: String = "主题"

    override suspend fun initialize() {
        launch {
            ApiTopicGetTopicDetails.request(topic.tid) {
                details = it
            }
        }
        launch {
            ApiTopicGetTopicComments.request(topic.tid, topic.rawSection, pageComments.default1, pageComments.default, pageComments.pageNum) {
                pageComments.newData(it)
            }
        }
    }

    override fun onBack() {
        if (subCommentSheet.isOpen) subCommentSheet.close()
        else pop()
    }

    @Composable
    override fun ActionScope.RightActions() {
        if (details != null) {
            val canUpdateTopicTop by rememberDerivedState { app.config.userProfile?.canUpdateTopicTop(topic.uid) == true }
            val canDeleteTopic by rememberDerivedState { app.config.userProfile?.canDeleteTopic(topic.uid) == true }
            val canMoveTopic by rememberDerivedState { app.config.userProfile?.hasPrivilegeVIPTopic == true }
            if (canUpdateTopicTop) {
                ActionSuspend(
                    icon = if (topic.isTop) Icons.Outlined.MobiledataOff else Icons.Outlined.VerticalAlignTop,
                    tip = if (topic.isTop) "取消置顶" else "置顶"
                ) {
                    onChangeTopicIsTop(!topic.isTop)
                }
            }
            if (canMoveTopic) {
                ActionSuspend(Icons.Outlined.MoveUp, "移动") {
                    onMoveTopic()
                }
            }
            if (canDeleteTopic) {
                ActionSuspend(Icons.Outlined.Delete, "删除") {
                    onDeleteTopic()
                }
            }
        }
    }

    @Composable
    override fun BottomBar() {
        if (details != null && app.config.userProfile != null) {
            BottomLayout(modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxWidth()
                .padding(CustomTheme.padding.equalValue)
            )
        }
    }

    @Composable
    override fun Content(device: Device) {
        details?.let {
            when (device.type) {
                Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait(details = it)
                Device.Type.LANDSCAPE -> Landscape(details = it)
            }
        } ?: EmptyBox()
    }

    private val subCommentSheet = this land object : FloatingArgsSheet<Comment>() {
        var page: Pagination<SubComment, Int, Int> by mutableRefStateOf(object : Pagination<SubComment, Int, Int>(
            default = 0,
            pageNum = APIConfig.MIN_PAGE_NUM
        ) {
            override fun distinctValue(item: SubComment): Int = item.cid
            override fun offset(item: SubComment): Int = item.cid
        })

        override suspend fun initialize(args: Comment) {
            page = object : Pagination<SubComment, Int, Int>(
                default = 0,
                pageNum = APIConfig.MIN_PAGE_NUM
            ) {
                override fun distinctValue(item: SubComment): Int = item.cid
                override fun offset(item: SubComment): Int = item.cid
            }
            requestSubComments(
                pid = args.cid,
                cid = page.offset,
                num = page.pageNum
            )?.let { page.newData(it) }
        }

        @Composable
        override fun Content(args: Comment) {
            val state = rememberLazyListState()
            PauseLoading(state)

            PaginationColumn(
                items = page.items,
                key = { it.cid },
                state = state,
                canRefresh = false,
                canLoading = page.canLoading,
                onLoading = {
                    requestSubComments(
                        pid = args.cid,
                        cid = page.offset,
                        num = page.pageNum
                    )?.let { page.moreData(it) }
                },
                itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalExtraSpace),
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue)
            ) { subComment ->
                SubCommentBar(
                    subComment = subComment,
                    parentComment = args,
                    modifier = Modifier.fillMaxWidth(),
                    onDelete = {
                        page.items -= subComment
                        pageComments.items.findAssign(predicate = { it.cid == args.cid }) {
                            it.copy(subCommentNum = it.subCommentNum - 1)
                        }
                        // 楼中楼最后一条回复删除后隐藏楼中楼
                        if (page.items.isEmpty()) close()
                    }
                )
            }
        }
    }

    private val sendCoinSheet = this land object : FloatingSheet() {
        @Composable
        override fun Content() {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Text(
                    text = "银币: ${app.config.userProfile?.coin ?: 0}",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        CoinLayout(
                            num = it + 1,
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            onClick = { num ->
                                close()
                                launch { onSendCoin(num) }
                            }
                        )
                    }
                }
            }
        }
    }

    private val moveTopicDialog = this land FloatingDialogChoice.fromItems(
        items = Comment.Section.MovableSection.fastMap { Comment.Section.sectionName(it) },
        title = "移动主题板块"
    )
}