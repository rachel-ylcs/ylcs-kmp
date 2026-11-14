package love.yinlin.page

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
import love.yinlin.PageGroup
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
import love.yinlin.compose.ui.icon.Icons
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
object ImagePage : Page(
    id = "image",
    icon = Icons.Image,
    title = "图像",
    english = "Image",
    description = "加载资源、本地或网络图片，也可播放、缩放和裁剪。",
    keywords = "图片 ImageSource ImageState",
    group = PageGroup.Layout,
) {
    private val demoResourceImage = component("Resource", "从应用资源读取图片，可离线查看。", "JPEG PNG BMP GIF WebP EXIF")
    private val demoWebImage = component("Web", "按网址加载图片，支持进度和重试。", "URL HTTP 缓存")
    private val demoLocalImage = component("Local", "从本地文件或内存字节读取图片。", "LocalFileImage Bytes ByteArrayImageInput", previewAlias = "LocalFileImage / Bytes")
    private val demoPlaceholder = component("占位", "在图片为空、加载中或失败时显示相应提示。", "ImagePlaceholder loading error retry", previewAlias = "占位与重试")
    private val demoImageLayout = component("排布", "控制图片在容器中的缩放、对齐和点击范围。", "ContentScale alignment contentRect", previewAlias = "布局与点击")
    private val demoAppearance = component("外观", "调整裁剪形状、着色和透明度，或设置加载淡入。", "ImageOptions ImageQuality fadeIn", previewAlias = "外观与画质")
    private val demoAnimatedImage = component("动图", "播放 GIF 和 WebP 动图，支持暂停或只显示首帧。", "GIF WebP isPlaying", previewAlias = "动图与生命周期")
    private val demoZoom = component("缩放", "放大图片查看细节，缩放时仍可在图集里翻页。", "ImageZoomState HorizontalPager", previewAlias = "图片缩放 / Pager")
    private val demoCache = component("缓存", "重复使用已加载的图片，内容更新时可用版本号重新读取。", "ImageManager clearCache version", previewAlias = "版本与缓存")
    private val demoCrop = component("裁剪", "拖动选区裁剪图片，可自由调整或锁定比例。", "cropImageRegion CropImageState CropRegion", previewAlias = "cropImageRegion")

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
        Component(demoResourceImage) {
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
            GalleryParagraph("内置样例可离线查看，JPEG 会按 EXIF 方向显示。")
        }
    }

    @Composable
    private fun ColumnScope.NetworkExamples() {
        Component(demoWebImage) {
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
            GalleryParagraph("可加载公开样例或输入自己的图片网址。浏览器需要图片服务允许跨域访问。")
        }
    }

    @Composable
    private fun ColumnScope.LocalExamples() {
        Component(demoLocalImage) {
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
                Example("LocalFileImage · 默认 Medium 画质", Modifier.weight(1f)) {
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
            GalleryParagraph("可读取自己的文件，也可先生成一张本地样图。")
        }
    }

    @Composable
    private fun ColumnScope.PlaceholderExamples() {
        Component(demoPlaceholder) {
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
            GalleryParagraph("加载失败时可点刷新重试。损坏的图片会显示错误图标。")
        }
    }

    @Composable
    private fun ColumnScope.LayoutExamples() {
        Component(demoImageLayout) {
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
            GalleryParagraph("点击只在实际绘制的图片范围内生效。")
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
        Component(demoAppearance) {
            var quality by rememberValueState(1)
            var circular by rememberFalse()
            var tint by rememberFalse()
            var fadeIn by rememberTrue()
            var alpha by rememberValueState(1f)
            var version by rememberValueState(0)
            val qualities = remember { [ImageQuality.Low, ImageQuality.Medium, ImageQuality.High, ImageQuality.Full] }
            val state = rememberImageState()
            GalleryActions {
                ComboBox(["Low", "Medium", "High", "Full"], { quality = it }, index = quality,
                    modifier = Modifier.width(Theme.size.input2))
                CheckBox(circular, { circular = it }, "圆形裁剪")
                CheckBox(tint, { tint = it }, "主题着色")
                CheckBox(fadeIn, { fadeIn = it }, "首次加载淡入")
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
                    colorFilter = if (tint) ColorFilter.tint(Theme.color.primary) else null,
                    fadeIn = fadeIn,
                )
            )
            ImageStateDetails(state)
            GalleryParagraph("画质影响缩放和绘制时的滤波，容器变化时会缩放已解码的图片。")
        }
    }

    @Composable
    private fun ColumnScope.AnimationExamples() {
        Component(demoAnimatedImage) {
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
            GalleryParagraph("动图按文件设置循环，播完后可以重播。")
        }
    }

    @Composable
    private fun ColumnScope.ZoomExamples() {
        Component(demoZoom) {
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
            GalleryParagraph("双击、捏合或用滚轮缩放。放大后可以拖动图片，拖到边缘可继续翻页。")
        }
    }

    @Composable
    private fun ColumnScope.CacheExamples() {
        Component(demoCache) {
            var version by rememberValueState(1)
            var useVersion by rememberFalse()
            var mount by rememberValueState(0)
            var stateGeneration by rememberValueState(0)
            var mounted by rememberTrue()
            var memoryBytes by rememberValueState(ImageManager.memoryCacheSize)
            var message by rememberState<String?> { null }
            val source = remember { Res.getUri("drawable/img0.webp") }
            val states = remember(stateGeneration) { List(3) { ImageState() } }
            GalleryActions {
                CheckBox(useVersion, { useVersion = it }, "附加版本键")
                TextButton("更新版本", enabled = useVersion) { version++ }
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
            GalleryCaption("${if (useVersion) "版本 $version" else "无附加版本"} · 内存缓存快照 $memoryBytes B")
            if (message != null) Text(message!!, color = Theme.color.error)
            if (mounted) key(mount) {
                ExampleRow {
                    for (index in 1 .. 3) {
                        Example("消费者 $index") {
                            val state = states[index - 1]
                            ResourceFileImage(source, contentDescription = "共享请求的图片 $index", state = state,
                                version = if (useVersion) "cache$version" else null, modifier = Modifier.size(Theme.size.image2))
                            GalleryCaption("${state.status} · ${state.origin ?: "—"}")
                        }
                    }
                }
            }
            GalleryParagraph("相同图片可共享读取结果。更新版本号可在地址不变时重新加载。")
        }
    }

    @Composable
    private fun ColumnScope.CropExamples() {
        var ratio by rememberValueState(0)
        var sample by rememberValueState(0)
        val ratios = remember { [0f, 1f, 4f / 3f, 16f / 9f] }
        val state = remember { CropImageState() }
        val imageState = rememberImageState()
        Component(demoCrop) {
            GalleryActions {
                ComboBox(["内置资源", "网络图片"], { sample = it }, index = sample,
                    modifier = Modifier.width(Theme.size.input2))
                ComboBox(["自由比例", "1:1", "4:3", "16:9"], { ratio = it }, index = ratio,
                    modifier = Modifier.width(Theme.size.input2))
                TextButton("重置选区") { state.reset() }
            }
            val modifier = Modifier.fillMaxWidth().height(Theme.size.image1 * 2)
                .cropImageRegion(imageState, state, ratios[ratio])
            if (sample == 0) {
                ResourceFileImage(Res.getUri("drawable/img0.webp"), state = imageState,
                    contentDescription = "可裁剪的资源图片", contentScale = ContentScale.Inside, modifier = modifier)
            }
            else {
                WebImage("https://${networkHosts[0]}/image/jpeg", state = imageState,
                    contentDescription = "可裁剪的网络图片", contentScale = ContentScale.Fit, modifier = modifier)
            }
            if (!state.imageRect.isEmpty) CropDetails("当前选区", state.result)
            GalleryParagraph("拖动边角调整选区，拖动内部移动选区。裁剪坐标按原图比例显示。")
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
