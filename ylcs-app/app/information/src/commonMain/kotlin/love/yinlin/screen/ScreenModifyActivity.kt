package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import love.yinlin.app
import love.yinlin.common.DataSourceInformation
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.graphics.PlatformImage
import love.yinlin.compose.graphics.crop
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.graphics.encode
import love.yinlin.compose.graphics.thumbnail
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.collection.TagView
import love.yinlin.compose.ui.container.AdderBox
import love.yinlin.compose.ui.container.ReplaceableBox
import love.yinlin.compose.ui.floating.DialogCrop
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.DialogPairInput
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.cs.*
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.data.rachel.activity.ActivityLink
import love.yinlin.data.rachel.activity.ActivityPrice
import love.yinlin.extension.*

@Stable
class ScreenModifyActivity(private val aid: Int) : Screen() {
    private val activities = DataSourceInformation.activities

    private val activity: Activity by derivedStateOf { activities.find { it.aid == aid }!! }

    // 活动信息状态
    private val shortTitle = InputState(initText = activity.shortTitle ?: "", maxLength = 5)
    private val fullTitle = InputState(initText = activity.title ?: "", maxLength = 32)
    private val date = InputState(initText = activity.ts ?: "", maxLength = 12)
    private val timeInfo = InputState(initText = activity.tsInfo ?: "", maxLength = 128)
    private val location = InputState(initText = activity.location ?: "", maxLength = 32)
    private val content = InputState(initText = activity.content ?: "", maxLength = 512)
    private val price = activity.price.toMutableStateList()
    private val saleTime = activity.saleTime.toMutableStateList()
    private val lineup = activity.lineup.toMutableStateList()
    private val playlist = activity.playlist.toMutableStateList()
    private val showstart = InputState(initText = activity.link.showstart ?: "", maxLength = 1024)
    private val damai = InputState(initText = activity.link.damai ?: "", maxLength = 16)
    private val maoyan = InputState(initText = activity.link.maoyan ?: "", maxLength = 16)
    private val link = InputState(initText = activity.link.link ?: "", maxLength = 256)
    private val qqGroupPhone = InputState(initText = activity.link.qqGroupPhone ?: "", maxLength = 16)
    private val qqGroupLink = InputState(initText = activity.link.qqGroupLink ?: "", maxLength = 128)
    private var hide: Boolean by mutableStateOf(activity.hide)
    private var photo by mutableRefStateOf(activity.photo)

    private val canSubmit by derivedStateOf {
        shortTitle.isSafe && fullTitle.isSafe && timeInfo.isSafe && location.isSafe && content.isSafe
                && showstart.isSafe && damai.isSafe && maoyan.isSafe && link.isSafe
                && qqGroupPhone.isSafe && qqGroupLink.isSafe
    }

    private suspend fun updateActivity() {
        // 检查
        val ts = date.text
        if (catchingError { DateEx.Formatter.standardDate.parse(ts) } != null) {
            slot.tip.warning("日期格式应为YYYY-MM-DD")
            return
        }
        val tsInfo = timeInfo.text
        val location = location.text
        val shortTitle = shortTitle.text
        val fullTitle = fullTitle.text
        val content = content.text
        val link = ActivityLink(
            showstart = showstart.text,
            damai = damai.text,
            maoyan = maoyan.text,
            link = link.text,
            qqGroupPhone = qqGroupPhone.text,
            qqGroupLink = qqGroupLink.text,
        )
        val newActivity = Activity(
            aid = aid,
            ts = ts,
            tsInfo = tsInfo,
            location = location,
            shortTitle = shortTitle,
            title = fullTitle,
            content = content,
            price = price,
            saleTime = saleTime,
            lineup = lineup,
            link = link,
            playlist = playlist,
            hide = hide
        )
        ApiActivityUpdateActivityInfo.request(app.config.userToken, newActivity) {
            activities.findAssign(predicate = { it.aid == aid }) {
                it.copy(
                    ts = ts,
                    tsInfo = tsInfo,
                    location = location,
                    shortTitle = shortTitle,
                    title = fullTitle,
                    content = content,
                    price = price,
                    saleTime = saleTime,
                    lineup = lineup,
                    link = link,
                    playlist = playlist
                )
            }
            pop()
        }.errorTip
    }

    private suspend fun updatePhoto(key: String, path: Path) {
        slot.loading.open(content = "正在上传...") {
            ApiActivityUpdateActivityPhoto.request(app.config.userToken, aid, key, apiFile(path)) { newPic ->
                activities.findAssign(predicate = { it.aid == aid }) {
                    photo = photo.toJson().Object.toMutableMap().let { map ->
                        map[key] = newPic.json
                        map.toJson().to()
                    }
                    it.copy(photo = photo)
                }
            }.errorTip
        }
    }

    private suspend fun deletePhoto(key: String) {
        slot.loading.open(content = "正在删除...") {
            ApiActivityDeleteActivityPhoto.request(app.config.userToken, aid, key) {
                activities.findAssign(predicate = { it.aid == aid }) {
                    photo = photo.toJson().Object.toMutableMap().let { map ->
                        map.remove(key)
                        map.toJson().to()
                    }
                    it.copy(photo = photo)
                }
            }.errorTip
        }
    }

    private suspend fun addPhotos(key: String, files: List<Path>) {
        slot.loading.open(content = "正在上传...") {
            ApiActivityAddActivityPhotos.request(app.config.userToken, aid, key, apiFile(files)!!) { newPics ->
                activities.findAssign(predicate = { it.aid == aid }) { oldActivity ->
                    photo = photo.toJson().Object.toMutableMap().let { map ->
                        map[key] = map[key].ArrayEmpty.toMutableList().also { list ->
                            list += newPics.map { it.json }
                        }.toJson()
                        map.toJson().to()
                    }
                    oldActivity.copy(photo = photo)
                }
            }.errorTip
        }
    }

    private suspend fun updatePhotos(key: String, index: Int) {
        app.picker.pickPicture()?.use { source ->
            app.os.storage.createTempFile { sink ->
                val image = PlatformImage.decode(source.readByteArray())!!
                image.thumbnail()
                sink.write(image.encode(quality = ImageQuality.High)!!)
                true
            }
        }?.let { path ->
            slot.loading.open("正在更新...") {
                ApiActivityUpdateActivityPhotos.request(app.config.userToken, aid, key, index, apiFile(path)) { newPic ->
                    activities.findAssign(predicate = { it.aid == aid }) { oldActivity ->
                        photo = photo.toJson().Object.toMutableMap().let { map ->
                            map[key] = map[key].Array.toMutableList().also { list ->
                                list[index] = newPic.json
                            }.toJson()
                            map.toJson().to()
                        }
                        oldActivity.copy(photo = photo)
                    }
                }.errorTip
            }
        }
    }

    private suspend fun deletePhotos(key: String, index: Int) {
        slot.loading.open(content = "正在删除...") {
            ApiActivityDeleteActivityPhotos.request(app.config.userToken, aid, key, index) {
                activities.findAssign(predicate = { it.aid == aid }) { oldActivity ->
                    photo = photo.toJson().Object.toMutableMap().let { map ->
                        map[key] = map[key].Array.toMutableList().also { list ->
                            list.removeAt(index)
                        }.toJson()
                        map.toJson().to()
                    }
                    oldActivity.copy(photo = photo)
                }
            }.errorTip
        }
    }

    suspend fun pickPicture(onPicAdd: suspend (Path) -> Unit) {
        val path = app.picker.pickPicture()?.use { source ->
            app.os.storage.createTempFile { sink -> source.transferTo(sink) > 0L }
        }
        if (path != null) {
            cropDialog.open(url = path.toString(), aspectRatio = 2f)?.let { region ->
                app.os.storage.createTempFile { sink ->
                    val image = PlatformImage.decode(path.readByteArray()!!)!!
                    image.crop(region)
                    image.thumbnail()
                    sink.write(image.encode(quality = ImageQuality.High)!!)
                    true
                }?.let { onPicAdd(it) }
            }
        }
    }

    suspend fun pickPictures(currentSize: Int, onPicsAdd: suspend (List<Path>) -> Unit) {
        app.picker.pickPicture((9 - currentSize).coerceAtLeast(1))?.use { sources ->
            val path = mutableListOf<Path>()
            for (source in sources) {
                app.os.storage.createTempFile { sink ->
                    val image = PlatformImage.decode(source.readByteArray())!!
                    image.thumbnail()
                    sink.write(image.encode(quality = ImageQuality.High)!!)
                    true
                }?.let { path += it }
            }
            if (path.isNotEmpty()) onPicsAdd(path)
        }
    }

    override val title: String = "修改活动"

    @Composable
    override fun RowScope.RightActions() {
        Icon(
            icon = if (hide) Icons.Visibility else Icons.VisibilityOff,
            tip = if (hide) "公开" else "隐藏",
            onClick = { hide = !hide }
        )

        LoadingIcon(icon = Icons.Check, tip = "提交", enabled = canSubmit, onClick = {
            updateActivity()
        })
    }

    @Composable
    private fun AdderLayout(list: SnapshotStateList<String>, text: String, initText: () -> String) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText(text = text, style = Theme.typography.v7.bold)
            Icon(icon = Icons.Add, onClick = {
                launch {
                    inputDialog.open(initText())?.let { list += it }
                }
            })
        }
        TagView(
            size = list.size,
            titleProvider = { list[it] },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h),
            onClick = { index ->
                launch {
                    inputDialog.open(list[index])?.let { list[index] = it }
                }
            },
            onDelete = { list.removeAt(it) }
        )
    }

    @Composable
    override fun Content() {
        Column(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .padding(Theme.padding.eValue)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
        ) {
            Input(state = shortTitle, hint = "短活动名称(可空)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = fullTitle, hint = "活动名称(可空)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = date, hint = "活动时间(可空, YYYY-MM-DD)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = timeInfo, hint = "活动时间补充信息(可空)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = location, hint = "活动地点(可空)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = content, hint = "活动内容(可空, 512字)", maxLines = 5, modifier = Modifier.fillMaxWidth())

            Text(
                text = "票价",
                style = Theme.typography.v7.bold,
                modifier = Modifier.padding(horizontal = Theme.padding.h)
            )
            AdderBox(
                maxNum = 16,
                items = price,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h),
                onAdd = { price += ActivityPrice("票种", 0) },
                onReplace = { index, item ->
                    launch {
                        pairInputDialog.open(item.name, item.value.toString())?.let { (newName, newValue) ->
                            price[index] = ActivityPrice(newName, newValue.toIntOrNull() ?: 0)
                        }
                    }
                },
                onDelete = { index, _ -> price.removeAt(index) }
            ) { _, item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    SimpleEllipsisText(text = item.value.toString(), style = Theme.typography.v5.bold, color = Theme.color.primary)
                    SimpleEllipsisText(text = item.name)
                }
            }

            AdderLayout(list = saleTime, text = "开售时间") { DateEx.TodayString }
            AdderLayout(list = lineup, text = "阵容艺人") { "" }
            AdderLayout(list = playlist, text = "歌单曲目") { "" }

            Input(state = showstart, hint = "秀动ID(可空)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = damai, hint = "大麦ID(可空)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = maoyan, hint = "猫眼ID(可空)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = link, hint = "活动链接(可空)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = qqGroupPhone, hint = "QQ群号(可空,仅手机端)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)
            Input(state = qqGroupLink, hint = "QQ群分享链接(可空,非手机端)", imeAction = ImeAction.Next, modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.LengthViewer)

            Text(
                text = "轮播图(可空)",
                style = Theme.typography.v7.bold,
                modifier = Modifier.padding(horizontal = Theme.padding.h)
            )
            ReplaceableBox(
                value = photo.coverPath,
                onReplace = {
                    launch {
                        pickPicture { updatePhoto("cover", it) }
                    }
                },
                onDelete = {
                    launch { deletePhoto("cover") }
                }
            ) {
                WebImage(
                    uri = it.url,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f)
                )
            }

            Text(
                text = "座位图(可空)",
                style = Theme.typography.v7.bold,
                modifier = Modifier.padding(horizontal = Theme.padding.h)
            )
            ReplaceableBox(
                value = photo.seatPath,
                onReplace = {
                    launch {
                        pickPicture { updatePhoto("seat", it) }
                    }
                },
                onDelete = {
                    launch { deletePhoto("seat") }
                }
            ) {
                WebImage(
                    uri = it.url,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f)
                )
            }

            Text(
                text = "活动海报",
                style = Theme.typography.v7.bold,
                modifier = Modifier.padding(horizontal = Theme.padding.h)
            )
            AdderBox(
                maxNum = 9,
                items = photo.posters,
                modifier = Modifier.fillMaxWidth(),
                onAdd = {
                    launch {
                        pickPictures(photo.posters.size) { addPhotos("posters", it) }
                    }
                },
                onReplace = { index, _ ->
                    launch { updatePhotos("posters", index) }
                },
                onDelete = { index, _ ->
                    launch { deletePhotos("posters", index) }
                }
            ) { _, item ->
                WebImage(
                    uri = photo.posterPath(item).url,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private val inputDialog = this land DialogInput(hint = "修改信息", maxLength = 64)
    private val pairInputDialog = this land DialogPairInput(hint1 = "票种名称", maxLength1 = 8, hint2 = "票种价格", maxLength2 = 5)
    private val cropDialog = this land DialogCrop()
}