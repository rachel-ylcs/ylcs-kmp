package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import love.yinlin.GalleryActions
import love.yinlin.GalleryBadge
import love.yinlin.GalleryCaption
import love.yinlin.GalleryParagraph
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.data.CropRegion
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.ImageLoadStatus
import love.yinlin.compose.image.data.ImageOptions
import love.yinlin.compose.image.input.ImageSource
import love.yinlin.compose.ui.image.*
import love.yinlin.compose.ui.input.CheckBox
import love.yinlin.compose.ui.input.ComboBox
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.Slider
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.node.cropImageRegion
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.extension.catchingError
import love.yinlin.fs.File
import love.yinlin.fs.PlatformFileSystem
import love.yinlin.gallery.resources.Res

@Stable
object ImagePage : Page() {
    @Stable
    private data class NetworkSample(val format: String, val size: String, val dimensions: String)

    // 2026-10-21 实际 GET 核对，两处公开 HTTP 测试服务提供相同样本，并响应跨域 Origin。
    private val networkSamples: List<NetworkSample> = [
        NetworkSample("png", "7.9 KiB", "100 × 100"),
        NetworkSample("jpeg", "34.8 KiB", "239 × 178"),
        NetworkSample("webp", "10.3 KiB", "274 × 367"),
    ]
    private val networkHosts: List<String> = ["httpbingo.org", "httpbin.org"]

    @Composable
    override fun Content() {
        ComponentColumn {
            ResourceExamples()
            NetworkExamples()
            LocalExamples()
            PlaceholderExamples()
            LayoutExamples()
            AppearanceExamples()
            AnimationExamples()
            ZoomExamples()
            CacheExamples()
            CropExamples()
        }
    }

    @Composable
    private fun ColumnScope.ResourceExamples() {
        Component("ResourceFileImage") {
            val samples = remember {
                [
                    "JPEG · EXIF 方向" to "files/image-exif.jpg",
                    "PNG" to "files/image-demo.png",
                    "BMP" to "files/image-demo.bmp",
                    "静态 WebP" to "files/image-static.webp",
                    "交错 GIF · 首帧" to "files/image-interlaced.gif",
                    "资源配图" to "drawable/img0.webp",
                ]
            }
            ExampleRow {
                for ([description, uri] in samples) {
                    Example(description) {
                        val state = rememberImageState()
                        ResourceFileImage(
                            Res.getUri(uri),
                            contentDescription = description,
                            modifier = Modifier.size(Theme.size.image2),
                            state = state,
                            options = ImageOptions(animated = false),
                        )
                        GalleryCaption("原图 ${imageSizeText(state.originalSize)}")
                    }
                }
            }
            GalleryParagraph("这些格式样本随 Gallery 打包，可离线查看。JPEG 样本携带 EXIF 方向；动图、透明叠帧与循环控制在下方演示。")
        }
    }

    @Composable
    private fun ColumnScope.NetworkExamples() {
        Component("WebImage") {
            val address = rememberInputState()
            var host by rememberValueState(0)
            var source by rememberState { "" }
            var version by rememberValueState(0)
            var headers by rememberFalse()
            val state = rememberImageState()
            val valid = address.text.trim().let { it.startsWith("https://", true) || it.startsWith("http://", true) }

            GalleryActions {
                ComboBox(networkHosts, { host = it }, index = host, modifier = Modifier.width(Theme.size.input1))
                for ((val format, val size) in networkSamples) {
                    TextButton("${format.uppercase()} · $size") {
                        val url = "https://${networkHosts[host]}/image/$format"
                        address.text = url
                        source = url
                        version++
                    }
                }
            }
            GalleryCaption("公开样例：${networkSamples.joinToString(" / ") { (val format, val dimensions) -> "${format.uppercase()} $dimensions" }}")
            Input(state = address, hint = "输入可访问的 HTTP(S) 图片地址", modifier = Modifier.fillMaxWidth())
            GalleryActions {
                PrimaryButton("加载", enabled = valid) {
                    source = address.text.trim()
                    version++
                }
                TextButton("重试", enabled = source.isNotEmpty()) { state.retry() }
                TextButton("更新版本", enabled = source.isNotEmpty()) { version++ }
                TextButton("清空") { source = "" }
                CheckBox(headers, { headers = it }, "附带 Accept: image/* 请求头")
            }
            if (headers) {
                val network = remember(source) {
                    source.takeIf { it.isNotEmpty() }?.let { ImageSource.Network(it, mapOf("Accept" to "image/*")) }
                }
                BasicImage(
                    network, contentDescription = "带请求头的网络图片", state = state, version = version.toString(),
                    modifier = Modifier.fillMaxWidth().height(Theme.size.image1),
                )
            }
            else {
                WebImage(
                    source, contentDescription = "网络图片", state = state, version = version.toString(),
                    modifier = Modifier.fillMaxWidth().height(Theme.size.image1),
                )
            }
            ImageStateDetails(state)
            GalleryCaption("版本 $version · 相同地址和版本复用缓存；更新版本保持 URL 原样。")
            GalleryParagraph("预设来自 httpbingo.org / httpbin.org 的公开 HTTP 测试样例，体积为本次实测近似值；点击格式按钮即可加载，也可输入自己的地址后点击加载。浏览器需要服务允许跨域访问，且不启用图片磁盘缓存。")
        }
    }

    @Composable
    private fun ColumnScope.LocalExamples() {
        Component("LocalFileImage / Bytes") {
            val context = if (ImageManager.isReady) ImageManager.context else null
            val sampleFile = remember(context) {
                context?.let { File(PlatformFileSystem.cachePath(it, "RachelGallery"), "gallery-image-sample.webp") }
            }
            val path = rememberInputState()
            var file by rememberState<File?> { null }
            var version by rememberValueState(0)
            var bytes by rememberState<ImageSource.Bytes?> { null }
            var message by rememberState<String?> { null }
            LaunchedEffect(Unit) {
                val error = catchingError { bytes = ImageSource.Bytes(Res.readBytes("drawable/img1.webp")) }
                message = error?.message
            }

            Input(state = path, hint = "本地文件路径（Web 使用浏览器文件系统路径）", modifier = Modifier.fillMaxWidth())
            GalleryActions {
                PrimaryButton("读取路径", enabled = path.text.isNotBlank()) {
                    file = File(path.text.trim())
                    version++
                }
                PrimaryLoadingButton("准备内置本地文件", enabled = sampleFile != null) {
                    val target = sampleFile ?: return@PrimaryLoadingButton
                    message = null
                    val error = catchingError {
                        val data = Res.readBytes("drawable/img0.webp")
                        target.parent?.mkdir()
                        target.write { it.write(data) }
                        path.text = target.path
                        file = target
                        version++
                    }
                    message = error?.message
                }
                TextButton("重新读取", enabled = file != null) { version++ }
                TextButton("清空路径预览") { file = null }
            }
            if (message != null) Text(message!!, color = Theme.color.error)
            ExampleRow(maxItemsInEachRow = 2) {
                Example("LocalFileImage · 默认 Full 画质", Modifier.weight(1f)) {
                    val state = rememberImageState()
                    val selected = file
                    if (selected == null) BasicImage(null, state = state, modifier = Modifier.size(Theme.size.image2))
                    else LocalFileImage(
                        selected, contentDescription = "本地文件预览", state = state, version = version.toString(),
                        modifier = Modifier.size(Theme.size.image2),
                    )
                    ImageStateDetails(state)
                }
                Example("BasicImage + ImageSource.Bytes", Modifier.weight(1f)) {
                    val state = rememberImageState()
                    BasicImage(bytes, contentDescription = "内置图片的字节输入", state = state, modifier = Modifier.size(Theme.size.image2))
                    ImageStateDetails(state)
                }
            }
            GalleryParagraph("准备按钮只写入 Gallery 缓存目录中的专用示例文件。也可读取自己的文件，修改文件后点击重新读取；字节输入使用独立的显示状态。")
        }
    }

    @Composable
    private fun ColumnScope.PlaceholderExamples() {
        Component("占位与重试") {
            var mode by rememberValueState(4)
            var version by rememberValueState(0)
            val source = remember(mode) {
                when (mode) {
                    0 -> null
                    1 -> ImageSource.Network("https://${networkHosts[0]}/image/png")
                    2 -> ImageSource.resource(Res.getUri("files/image-demo.png") + ".missing")
                    3 -> ImageSource.Bytes("invalid image".encodeToByteArray())
                    else -> ImageSource.resource(Res.getUri("drawable/img0.webp"))
                }
            }
            GalleryActions {
                for (index in 0 .. 4) {
                    TextButton(["空内容", "网络加载", "读取失败", "损坏数据", "恢复"][index]) {
                        mode = index
                        version++
                    }
                }
            }
            Example("默认占位 · 空内容 / 进度 / 错误重试", Modifier.fillMaxWidth()) {
                val state = rememberImageState()
                BasicImage(source, contentDescription = "默认占位预览", state = state, version = version.toString(), modifier = Modifier.fillMaxWidth().height(Theme.size.image2))
                ImageStateDetails(state)
            }
            ExampleRow(maxItemsInEachRow = 3) {
                for (size in [Theme.size.icon / 2, Theme.size.icon, Theme.size.input7]) {
                    Example("小图 · ${size.value.toInt()} dp", Modifier.weight(1f)) {
                        val state = rememberImageState()
                        Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.h), verticalAlignment = Alignment.CenterVertically) {
                            Text("左侧")
                            BasicImage(source, contentDescription = "小尺寸默认占位预览", state = state, version = version.toString(), modifier = Modifier.size(size))
                            Text("右侧")
                        }
                    }
                }
            }
            GalleryParagraph("网络加载使用公开 PNG 样例展示真实下载进度，加载时长取决于网络。读取失败使用不存在的资源路径，显示警告色刷新图标，可点击重试；损坏数据使用无法解码的字节，显示不可点击的错误色图标。点击恢复加载内置资源，空内容不会发起读取。下方小图用于检查占位和重试范围是否影响相邻文字。")
        }
    }

    @Composable
    private fun ColumnScope.LayoutExamples() {
        Component("布局与点击") {
            val scales = remember { [ContentScale.Fit, ContentScale.Crop, ContentScale.Inside, ContentScale.FillWidth,
                ContentScale.FillHeight, ContentScale.FillBounds, ContentScale.None] }
            val alignments = remember { [Alignment.Center, Alignment.TopStart, Alignment.BottomEnd] }
            var scale by rememberValueState(0)
            var alignment by rememberValueState(0)
            var clicks by rememberValueState(0)
            val state = rememberImageState()
            GalleryActions {
                ComboBox(["Fit", "Crop", "Inside", "FillWidth", "FillHeight", "FillBounds", "None"], { scale = it },
                    index = scale, modifier = Modifier.width(Theme.size.input2))
                ComboBox(["Center", "TopStart", "BottomEnd"], { alignment = it },
                    index = alignment, modifier = Modifier.width(Theme.size.input2))
                GalleryBadge("有效点击 $clicks")
                TextButton("清零") { clicks = 0 }
            }
            ResourceFileImage(
                Res.getUri("drawable/img0.webp"), contentDescription = "点击实际图片区域以计数", state = state,
                contentScale = scales[scale], alignment = alignments[alignment], onClick = { clicks++ },
                modifier = Modifier.fillMaxWidth().height(Theme.size.image1),
            )
            GalleryParagraph("Fit / Inside 的留白不计入点击；可用 Tab 聚焦后按 Enter 或空格激活图片。切换窗口宽度观察固定高度的绘制范围。")
            ExampleRow {
                Example("无显式尺寸 · 主题默认占位") {
                    ResourceFileImage(Res.getUri("drawable/img0.webp"), contentDescription = "默认尺寸图片")
                }
                Example("只固定宽度 · 默认占位") {
                    ResourceFileImage(Res.getUri("drawable/img0.webp"), contentDescription = "宽度约束图片",
                        modifier = Modifier.width(Theme.size.image2))
                }
                Example("只固定高度 · 默认占位") {
                    ResourceFileImage(Res.getUri("drawable/img0.webp"), contentDescription = "高度约束图片",
                        modifier = Modifier.height(Theme.size.image3))
                }
            }
            ImageStateDetails(state)
        }
    }

    @Composable
    private fun ColumnScope.AppearanceExamples() {
        Component("外观与画质") {
            var quality by rememberValueState(1)
            var circular by rememberFalse()
            var tint by rememberFalse()
            var alpha by rememberValueState(1f)
            var version by rememberValueState(0)
            val qualities = remember { [ImageQuality.Low, ImageQuality.Medium, ImageQuality.High, ImageQuality.Full] }
            val state = rememberImageState()
            GalleryActions {
                ComboBox(["Low", "Medium", "High", "Full"], { quality = it }, index = quality,
                    modifier = Modifier.width(Theme.size.input2))
                CheckBox(circular, { circular = it }, "圆形裁剪")
                CheckBox(tint, { tint = it }, "主题着色")
                TextButton("重新加载以比较淡入") { version++ }
            }
            GalleryCaption("透明度 ${(alpha * 100).toInt()}%")
            Slider(alpha, { alpha = it }, modifier = Modifier.fillMaxWidth(), onValueChange = { alpha = it })
            ResourceFileImage(
                Res.getUri("drawable/img0.webp"),
                contentDescription = "可调整外观和采样质量的图片",
                state = state,
                version = version.toString(),
                modifier = Modifier.size(Theme.size.image1), contentScale = ContentScale.Crop,
                alpha = alpha,
                options = ImageOptions(
                    quality = qualities[quality],
                    circular = circular,
                    colorFilter = if (tint) ColorFilter.tint(Theme.color.primary) else null
                )
            )
            ImageStateDetails(state)
        }
    }

    @Composable
    private fun ColumnScope.AnimationExamples() {
        Component("动图与生命周期") {
            var mounted by rememberTrue()
            var stress by rememberFalse()
            var replay by rememberValueState(0)
            GalleryActions {
                CheckBox(mounted, { mounted = it }, "挂载动图")
                CheckBox(stress, { stress = it }, "80 帧样本")
                TextButton("从头重播") { replay++ }
            }
            if (mounted) key(replay, stress) {
                ExampleRow(maxItemsInEachRow = 2) {
                    for (extension in ["gif", "webp"]) {
                        val path = "files/image-${if (stress) "stress" else "demo"}.$extension"
                        Example("${extension.uppercase()} · 点击暂停 / 继续", Modifier.weight(1f)) {
                            val state = rememberImageState()
                            ResourceFileImage(
                                Res.getUri(path), contentDescription = "$extension 动画", state = state,
                                modifier = Modifier.size(Theme.size.image2),
                                onClick = { state.isPlaying = !state.isPlaying },
                            )
                            TextButton(if (state.isPlaying) "暂停" else "继续") { state.isPlaying = !state.isPlaying }
                            ImageStateDetails(state)
                        }
                        Example("${extension.uppercase()} · 首帧模式", Modifier.weight(1f)) {
                            ResourceFileImage(Res.getUri(path), contentDescription = "$extension 首帧",
                                modifier = Modifier.size(Theme.size.image2), options = ImageOptions(animated = false))
                        }
                    }
                }
            }
            else GalleryParagraph("动图已离开组合。重新挂载后会创建独立的播放状态。")
            GalleryParagraph("GIF 与 WebP 使用各自的播放状态，并遵循文件循环次数；有限循环结束后可从头重播。向下滚动使图片离屏、切换页面或最小化窗口，可检查暂停与恢复。")
        }
    }

    @Composable
    private fun ColumnScope.ZoomExamples() {
        Component("图片缩放 / Pager") {
            val paths = remember { ["drawable/img0.webp", "drawable/img1.webp", "files/image-stress.webp"] }
            val states = remember { List(paths.size) { ImageState() } }
            val pager = rememberPagerState { paths.size }
            val state = states[pager.currentPage]
            GalleryActions {
                GalleryBadge("${pager.currentPage + 1} / ${paths.size}")
                GalleryBadge("缩放 ${(state.zoom.scale * 100).toInt()}%")
                TextButton("放大") {
                    val size = state.size ?: return@TextButton
                    state.zoom.transform(Offset(size.width / 2f, size.height / 2f), zoom = 1.5f)
                }
                TextButton("缩小") {
                    val size = state.size ?: return@TextButton
                    state.zoom.transform(Offset(size.width / 2f, size.height / 2f), zoom = 1f / 1.5f)
                }
                TextButton("复位") { state.zoom.reset() }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(Theme.size.image1 * 2)) { index ->
                ResourceFileImage(
                    Res.getUri(paths[index]),
                    contentDescription = "可缩放图片 ${index + 1}",
                    state = states[index],
                    modifier = Modifier.fillMaxSize(),
                    options = ImageOptions(zoom = true, quality = ImageQuality.High)
                )
            }
            ImageStateDetails(state)
            GalleryParagraph("双击、双指、滚轮可缩放，放大后拖动平移；聚焦图片后可用 + / − / 0 / Esc。未缩放或平移到边缘后继续横拖，可切换 Pager；第三页为动图，可观察缩放时的播放。")
        }
    }

    @Composable
    private fun ColumnScope.CacheExamples() {
        Component("版本与缓存") {
            var version by rememberValueState(1)
            var mount by rememberValueState(0)
            var stateGeneration by rememberValueState(0)
            var mounted by rememberTrue()
            var memoryBytes by rememberValueState(ImageManager.memoryCacheSize)
            var message by rememberState<String?> { null }
            val source = remember { Res.getUri("drawable/img0.webp") }
            val states = remember(stateGeneration) { List(3) { ImageState() } }
            GalleryActions {
                TextButton("更新版本") { version++ }
                TextButton("保留状态重挂载") { mount++ }
                TextButton("新状态重挂载") {
                    stateGeneration++
                    mount++
                }
                CheckBox(mounted, { mounted = it }, "挂载三个消费者")
                TextButton("刷新内存占用") { memoryBytes = ImageManager.memoryCacheSize }
                PrimaryLoadingButton("清理图片缓存", enabled = ImageManager.isReady) {
                    message = null
                    val error = catchingError {
                        ImageManager.clearCache()
                        memoryBytes = ImageManager.memoryCacheSize
                        stateGeneration++
                        mount++
                    }
                    message = error?.message
                }
            }
            GalleryCaption("版本 $version · 内存缓存快照 $memoryBytes B")
            if (message != null) Text(message!!, color = Theme.color.error)
            if (mounted) key(mount) {
                ExampleRow {
                    for (index in 1 .. 3) {
                        Example("消费者 $index") {
                            val state = states[index - 1]
                            ResourceFileImage(source, contentDescription = "共享请求的图片 $index", state = state, version = "cache$version", modifier = Modifier.size(Theme.size.image2))
                            GalleryCaption("${state.status} · ${state.origin ?: "—"}")
                        }
                    }
                }
            }
            GalleryParagraph("三个位置使用相同内置资源、版本和尺寸，各自持有 ImageState。首次加载共享同一请求；保留状态重挂载复用 UI 结果，新状态重挂载可观察 Memory 命中。更新版本会重新读取内置资源，卸载后重新挂载可查看缓存复用。")
            GalleryParagraph("清理按钮等待 ImageManager.clearCache() 完成后重新挂载本组。上方 WebImage 保持相同地址和版本时可复用图片磁盘缓存，更新版本后重新下载。")
        }
    }

    @Composable
    private fun ColumnScope.CropExamples() {
        var ratio by rememberValueState(0)
        var sample by rememberValueState(0)
        val ratios = remember { [0f, 1f, 4f / 3f, 16f / 9f] }
        val state = remember { CropImageState() }
        val imageState = rememberImageState()
        Component("cropImageRegion") {
            GalleryActions {
                ComboBox(["内置资源", "网络图片"], { sample = it }, index = sample,
                    modifier = Modifier.width(Theme.size.input2))
                ComboBox(["自由比例", "1:1", "4:3", "16:9"], { ratio = it }, index = ratio,
                    modifier = Modifier.width(Theme.size.input2))
                TextButton("重置选区") { state.reset() }
            }
            val modifier = Modifier.fillMaxWidth().height(Theme.size.image1 * 2)
                .background(Theme.color.scrim).cropImageRegion(imageState, state, ratios[ratio])
            if (sample == 0) {
                ResourceFileImage(Res.getUri("drawable/img0.webp"), state = imageState,
                    contentDescription = "可裁剪的资源图片", contentScale = ContentScale.Inside, modifier = modifier)
            }
            else {
                WebImage("https://${networkHosts[0]}/image/jpeg", state = imageState,
                    contentDescription = "可裁剪的网络图片", contentScale = ContentScale.Fit, modifier = modifier)
            }
            if (!state.imageRect.isEmpty) CropDetails("当前选区", state.result)
            GalleryParagraph("在任意图片组件的 Modifier 后附加 cropImageRegion，并传入该图片使用的同一个 ImageState。拖动边角调整选区，拖动内部移动选区；修改比例和重置选区不会重新加载图片。结果为相对于原图的比例坐标。")
        }
    }

    @Composable
    private fun ImageStateDetails(state: ImageState) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Theme.padding.h)) {
            GalleryCaption("${state.status} · 来源 ${state.origin ?: "—"} · ${if (state.isAnimated) "动画" else "静态 / 未加载"}")
            GalleryCaption("原图 ${imageSizeText(state.originalSize)} · 解码 ${imageSizeText(state.imageSize)} · 容器 ${imageSizeText(state.size)}")
            if (state.status == ImageLoadStatus.Loading) {
                val progress = state.progress
                GalleryCaption("已接收 ${progress.received} B / ${progress.total?.toString() ?: "未知"} · ${progress.fraction?.let { "${(it * 100).toInt()}%" } ?: "进度未知"}")
            }
            state.error?.let {
                Text(it.message ?: "图片加载失败", color = if (state.canRetry) Theme.color.warning else Theme.color.error,
                    style = Theme.typography.v7)
            }
        }
    }

    private fun imageSizeText(size: IntSize?): String = size?.let { "${it.width} × ${it.height}" } ?: "—"

    @Composable
    private fun CropDetails(label: String, region: CropRegion) {
        GalleryCaption("$label · x ${(region.xPercent * 100).toInt()}% / y ${(region.yPercent * 100).toInt()}% / 宽 ${(region.widthPercent * 100).toInt()}% / 高 ${(region.heightPercent * 100).toInt()}%")
    }
}
