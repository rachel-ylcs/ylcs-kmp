package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import love.yinlin.app
import love.yinlin.common.DataSourceDiscovery
import love.yinlin.compose.Device
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.BoxText
import love.yinlin.compose.ui.common.UserBar
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.Menus
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.NineGrid
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationArgs
import love.yinlin.compose.ui.layout.PaginationColumn
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.RachelRichParser
import love.yinlin.compose.ui.text.RachelRichText
import love.yinlin.compose.ui.text.RichEditor
import love.yinlin.compose.ui.text.RichEditorState
import love.yinlin.compose.ui.text.RichString
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.tool.UnsupportedPlatformComponent
import love.yinlin.cs.*
import love.yinlin.data.compose.Picture
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.SubComment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.rachel.topic.TopicDetails
import love.yinlin.extension.DateEx
import love.yinlin.extension.findAssign

@Stable
class ScreenTopic(currentTopic: Topic) : Screen() {
    private var currentDetails: TopicDetails? by mutableRefStateOf(null)
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

    @Stable
    private data class AtInfo(val uid: Int, val name: String) {
        override fun equals(other: Any?): Boolean = other is AtInfo && other.uid == uid
        override fun hashCode(): Int = uid

        val avatarPath: String by lazy { ServerRes.Users.User(uid).avatar.url }
    }

    private suspend fun requestMoreComments() {
        ApiTopicGetTopicComments.request(topic.tid, topic.rawSection, pageComments.arg1, pageComments.offset, pageComments.pageNum) {
            pageComments.moreData(it)
        }
    }

    private suspend fun requestSubComments(pid: Int, cid: Int, num: Int): List<SubComment>? =
        ApiTopicGetTopicSubComments.requestNull(pid, topic.rawSection, cid, num)?.o1

    private fun onAvatarClick(uid: Int) {
        navigate(::ScreenUserCard, uid)
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
        if (slot.confirm.open(content = "删除主题?")) {
            ApiTopicDeleteTopic.request(app.config.userToken, topic.tid) {
                DataSourceDiscovery.page.items.removeAll { it.tid == topic.tid }
                pop()
            }.errorTip
        }
    }

    private suspend fun onMoveTopic() {
        moveTopicDialog.open()?.let { index ->
            val newSection = Comment.Section.MovableSection[index]
            currentDetails?.let { oldDetails ->
                val oldSection = oldDetails.section
                if (newSection == oldSection) {
                    slot.tip.warning("不能与原板块相同哦")
                    return@let
                }
                ApiTopicMoveTopic.request(app.config.userToken, topic.tid, newSection) {
                    if (DataSourceDiscovery.currentSection == oldSection) DataSourceDiscovery.page.items.removeAll { it.tid == topic.tid }
                    currentDetails = oldDetails.copy(section = newSection)
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
            DataSourceDiscovery.page.items.findAssign(predicate = { it.tid == topic.tid }) {
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
                val err = ApiTopicSendComment.request(app.config.userToken, topic.tid, topic.rawSection, content) { cid ->
                    DataSourceDiscovery.page.items.findAssign(predicate = { it.tid == topic.tid }) {
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
                }.errorTip
                listState.animateScrollToItem(pageComments.items.size - 1)
                err == null
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
        }.errorTip
        listState.scrollToItem(pageComments.items.indexOfFirst { it.cid == cid })
    }

    private suspend fun onDeleteComment(cid: Int) {
        if (slot.confirm.open(content = "删除回复(楼中楼会同步删除)")) {
            ApiTopicDeleteComment.request(app.config.userToken, topic.tid, cid, topic.rawSection) {
                DataSourceDiscovery.page.items.findAssign(predicate = { it.tid == topic.tid }) {
                    it.copy(commentNum = it.commentNum - 1)
                }
                pageComments.items.removeAll { it.cid == cid }
            }.errorTip
        }
    }

    private suspend fun onDeleteSubComment(pid: Int, cid: Int, onDelete: suspend () -> Unit) {
        if (slot.confirm.open(content = "删除回复")) {
            ApiTopicDeleteSubComment.request(app.config.userToken, topic.tid, pid, topic.rawSection, cid, onDelete).errorTip
        }
    }

    override val title: String = "主题"

    override suspend fun initialize() {
        supervisorScope {
            this.launch {
                ApiTopicGetTopicDetails.request(topic.tid) {
                    currentDetails = it
                }
            }
            this.launch {
                ApiTopicGetTopicComments.request(topic.tid, topic.rawSection, pageComments.default1, pageComments.default, pageComments.pageNum) {
                    pageComments.newData(it)
                }
            }
        }
    }

    private val sendCommentState = object : RichEditorState(maxLength = 256) {
        override val useImage: Boolean = true
        override val useAt: Boolean = true

        private val userList by derivedStateOf {
            (pageComments.items.asSequence().map { AtInfo(it.uid, it.name) }
                    + AtInfo(topic.uid, topic.name) // 添加楼主
                    - AtInfo(app.config.userProfile?.uid ?: 0, "")) // 去除自己
                .distinct().toList() // 去重回复
        }

        @Composable
        override fun AtLayout(modifier: Modifier) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell1),
                modifier = modifier
            ) {
                items(
                    items = userList,
                    key = { it.uid }
                ) { info ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            closeLayout("[at|${info.uid}|@${info.name}]")
                        }.padding(Theme.padding.value),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WebImage(
                            uri = info.avatarPath,
                            key = remember { DateEx.TodayString },
                            contentScale = ContentScale.Crop,
                            circle = true,
                            modifier = Modifier.size(Theme.size.image10)
                        )
                        SimpleEllipsisText(info.name)
                    }
                }
            }
        }

        @Composable
        override fun ImageLayout(modifier: Modifier) {
            UnsupportedPlatformComponent(modifier = modifier)
        }
    }

    @Composable
    private fun RichTextLayout(text: RichString, modifier: Modifier = Modifier) {
        RachelRichText(
            text = text,
            onLinkClick = ::navigateScreenWebPage,
            onTopicClick = {},
            onAtClick = { navigate(::ScreenUserCard, it.toIntOrNull() ?: 0) },
            modifier = modifier,
            fixLineHeight = false
        )
    }

    @Composable
    private fun TopicLayout(details: TopicDetails, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            contentPadding = Theme.padding.eValue,
            contentAlignment = Alignment.TopCenter,
            shadowElevation = Theme.shadow.v3,
        ) {
            val pics = remember(details, topic) { details.pics.fastMap { Picture(topic.picPath(it).url) } }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                UserBar(
                    avatar = remember(topic) { topic.avatarPath.url },
                    name = topic.name,
                    time = details.ts,
                    label = details.label,
                    level = details.level,
                    onAvatarClick = { onAvatarClick(topic.uid) }
                )
                SelectionBox {
                    Text(
                        text = topic.title,
                        style = Theme.typography.v7.bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                RichTextLayout(
                    text = remember(details) { RachelRichParser.parse(details.content) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (pics.isNotEmpty()) {
                    NineGrid(
                        pics = pics,
                        modifier = Modifier.fillMaxWidth(),
                        onImageClick = { index, _ -> onImageClick(pics, index) }
                    ) { isSingle, pic, onClick ->
                        WebImage(
                            uri = pic.image,
                            contentScale = if (isSingle) ContentScale.Inside else ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().condition(!isSingle) { fillMaxHeight() },
                            onClick = onClick
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CommentBar(comment: Comment, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            UserBar(
                avatar = comment.avatarPath.url,
                name = comment.name,
                time = comment.ts,
                label = comment.label,
                level = comment.level,
                onAvatarClick = { onAvatarClick(comment.uid) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (comment.isTop) BoxText(text = "置顶", color = Theme.color.primary)
                    if (comment.uid == topic.uid) BoxText(text = "楼主", color = Theme.color.secondary)
                }

                var menuVisible by rememberFalse()
                Menus(
                    visible = menuVisible,
                    onClose = { menuVisible = false },
                    menus = {
                        Menu(text = "回复", icon = Icons.Send, onClick = {
                            currentSendComment = comment
                        })

                        app.config.userProfile?.let { user ->
                            if (user.canUpdateCommentTop(topic.uid)) {
                                Menu(
                                    text = if (comment.isTop) "取消置顶" else "置顶",
                                    icon = if (comment.isTop) Icons.MobiledataOff else Icons.VerticalAlignTop,
                                    onClick = {
                                        launch { onChangeCommentIsTop(comment.cid, !comment.isTop) }
                                    }
                                )
                            }
                            if (user.canDeleteComment(topic.uid, comment.uid)) {
                                Menu(text = "删除", icon = Icons.Delete, onClick = {
                                    launch { onDeleteComment(comment.cid) }
                                })
                            }
                        }
                    }
                ) {
                    Icon(icon = Icons.MoreHorizontal, onClick = { menuVisible = true })
                }
            }

            RichTextLayout(
                text = remember(comment) { RachelRichParser.parse(comment.content) },
                modifier = Modifier.fillMaxWidth()
            )

            if (comment.subCommentNum > 0) {
                Text(
                    text = ">> 查看${comment.subCommentNum}条回复",
                    style = Theme.typography.v7.bold,
                    color = Theme.color.primary,
                    modifier = Modifier.padding(top = Theme.padding.v).clickable { subCommentSheet.open(comment) }.padding(Theme.padding.g)
                )
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
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (subComment.uid == topic.uid) BoxText(text = "楼主", color = Theme.color.secondary)
                    if (subComment.uid == parentComment.uid) BoxText(text = "层主", color = Theme.color.tertiary)
                }

                var menuVisible by rememberFalse()
                Menus(
                    visible = menuVisible,
                    onClose = { menuVisible = false },
                    menus = {
                        app.config.userProfile?.let { user ->
                            if (user.canDeleteComment(topic.uid, subComment.uid)) {
                                Menu(text = "删除", icon = Icons.Delete, onClick = {
                                    launch { onDeleteSubComment(parentComment.cid, subComment.cid, onDelete) }
                                })
                            }
                        }
                    }
                ) {
                    Icon(icon = Icons.MoreHorizontal, onClick = { menuVisible = true })
                }
            }

            RichTextLayout(
                text = remember(subComment) { RachelRichParser.parse(subComment.content) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun BottomLayout(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            ActionScope.SplitContainer(
                modifier = Modifier.fillMaxWidth(),
                left = {
                    Icon(icon = Icons.Home, tip = "回复楼主", enabled = currentSendComment != null, onClick = { currentSendComment = null })

                },
                right = {
                    Icon(icon = Icons.Paid, tip = "投币", onClick = sendCoinSheet::open)
                    LoadingIcon(icon = Icons.Send, tip = "发送", enabled = sendCommentState.isSafe, onClick = {
                        if (onSendComment(sendCommentState.richString.toString())) {
                            sendCommentState.text = ""
                            sendCommentState.closePreview()
                        }
                    })
                }
            )
            RichEditor(
                state = sendCommentState,
                hint = remember(currentSendComment) { "回复 @${currentSendComment?.name ?: "主题"}" },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Composable
    private fun Portrait(details: TopicDetails) {
        PaginationColumn(
            items = pageComments.items,
            key = { it.cid },
            state = listState,
            canRefresh = false,
            canLoading = pageComments.canLoading,
            onLoading = { requestMoreComments() },
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
            header = { TopicLayout(details = details, modifier = Modifier.fillMaxWidth()) }
        ) {
            CommentBar(
                comment = it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.e)
            )
        }
    }

    @Composable
    private fun Landscape(details: TopicDetails) {
        Row(modifier = Modifier.fillMaxSize()) {
            TopicLayout(
                modifier = Modifier
                    .padding(LocalImmersivePadding.current.withoutEnd)
                    .width(Theme.size.cell1)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                details = details
            )

            PaginationColumn(
                items = pageComments.items,
                key = { it.cid },
                state = listState,
                canRefresh = false,
                canLoading = pageComments.canLoading,
                onLoading = { requestMoreComments() },
                modifier = Modifier
                    .padding(LocalImmersivePadding.current.withoutStart)
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                CommentBar(comment = it, modifier = Modifier.fillMaxWidth().padding(Theme.padding.value))
            }
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        if (currentDetails != null) {
            if (app.config.userProfile?.canUpdateTopicTop(topic.uid) == true) {
                LoadingIcon(
                    icon = if (topic.isTop) Icons.MobiledataOff else Icons.VerticalAlignTop,
                    tip = if (topic.isTop) "取消置顶" else "置顶",
                    onClick = { onChangeTopicIsTop(!topic.isTop) }
                )
            }
            if (app.config.userProfile?.hasPrivilegeVIPTopic == true) {
                LoadingIcon(icon = Icons.MoveUp, tip = "移动", onClick = ::onMoveTopic)
            }
            if (app.config.userProfile?.canDeleteTopic(topic.uid) == true) {
                LoadingIcon(icon = Icons.Delete, tip = "删除", onClick = ::onDeleteTopic)
            }
        }
    }

    @Composable
    override fun BottomBar() {
        if (currentDetails != null && app.config.userProfile != null) {
            BottomLayout(modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxWidth()
                .padding(Theme.padding.eValue)
            )
        }
    }

    @Composable
    override fun Content() {
        val details = currentDetails
        if (details != null) {
            when (LocalDevice.current.type) {
                Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait(details = details)
                Device.Type.LANDSCAPE -> Landscape(details = details)
            }
        }
    }

    private val moveTopicDialog = this land DialogChoice.fromItems(Comment.Section.MovableSection.fastMap { Comment.Section.sectionName(it) })

    private val subCommentSheet = this land object : SheetContent<Comment>() {
        override val scrollable: Boolean = false

        var page: Pagination<SubComment, Int, Int> by mutableRefStateOf(object : Pagination<SubComment, Int, Int>(
            default = 0,
            pageNum = APIConfig.MIN_PAGE_NUM
        ) {
            override fun distinctValue(item: SubComment): Int = item.cid
            override fun offset(item: SubComment): Int = item.cid
        })

        override suspend fun initialize(args: Comment) {
            page = object : Pagination<SubComment, Int, Int>(default = 0, pageNum = APIConfig.MIN_PAGE_NUM) {
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
            PaginationColumn(
                items = page.items,
                key = { it.cid },
                canRefresh = false,
                canLoading = page.canLoading,
                onLoading = {
                    requestSubComments(
                        pid = args.cid,
                        cid = page.offset,
                        num = page.pageNum
                    )?.let { page.moreData(it) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { subComment ->
                SubCommentBar(
                    subComment = subComment,
                    parentComment = args,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
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

    private val sendCoinSheet = this land object : Sheet() {
        @Composable
        override fun Content() {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.v5),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText(
                    text = "银币: ${app.config.userProfile?.coin ?: 0}",
                    style = Theme.typography.v6.bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val num = index + 1
                        Surface(
                            modifier = Modifier.weight(1f),
                            tonalLevel = 3,
                            contentPadding = Theme.padding.value9,
                            shadowElevation = Theme.shadow.v5,
                            onClick = {
                                launch { onSendCoin(num) }
                                close()
                            }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                            ) {
                                Icon(
                                    icon = Icons.Paid,
                                    color = when (num) {
                                        1 -> Theme.color.primary
                                        2 -> Theme.color.secondary
                                        else -> Theme.color.tertiary
                                    }
                                )
                                SimpleEllipsisText(
                                    text = when (num) {
                                        1 -> "幼时榆荫"
                                        2 -> "游仙夜话"
                                        else -> "落笔当下"
                                    },
                                    style = Theme.typography.v8,
                                    color = LocalColorVariant.current,
                                )
                                SimpleEllipsisText(text = "投币×$num", style = Theme.typography.v7.bold)
                            }
                        }
                    }
                }
            }
        }
    }
}