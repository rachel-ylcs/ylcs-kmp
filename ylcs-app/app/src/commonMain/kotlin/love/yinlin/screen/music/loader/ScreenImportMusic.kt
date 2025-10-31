package love.yinlin.screen.music.loader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlinx.serialization.Serializable
import love.yinlin.app
import love.yinlin.common.Paths
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.animation.LoadingAnimation
import love.yinlin.data.Data
import love.yinlin.data.compose.ItemKey
import love.yinlin.data.MimeType
import love.yinlin.extension.fileSizeString
import love.yinlin.mod.ModFactory
import love.yinlin.platform.*
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.node.DragFlag
import love.yinlin.compose.ui.node.DropResult
import love.yinlin.compose.ui.node.dragAndDrop

expect fun processImportMusicDeepLink(deepLink: String): ImplicitUri

@Stable
class ScreenImportMusic(manager: ScreenManager, private val args: Args) : Screen<ScreenImportMusic.Args>(manager) {
    @Stable
    @Serializable
    data class Args(val deepLink: String?)

    @Stable
    private sealed interface Step {
        @Stable
        data class Initial(val message: String = "未加载文件", val isError: Boolean = false) : Step
        @Stable
        data class Prepare(val path: ImplicitUri) : Step
        @Stable
        data class Preview(val path: ImplicitUri, val preview: ModFactory.Preview.PreviewResult?) : Step
        @Stable
        data class Processing(val message: String) : Step
    }

    private var step: Step by mutableRefStateOf(Step.Initial())

    private fun reset() {
        step = Step.Initial()
    }

    private suspend fun loadModFile() {
        Platform.use(
            Platform.WebWasm,
            ifTrue = {
                slot.tip.warning(UnsupportedPlatformText)
            },
            ifFalse = {
                app.picker.pickPath(mimeType = listOf(MimeType.BINARY), filter = listOf("*.rachel"))?.let {
                    step = Step.Prepare(it)
                }
            }
        )
    }

    private suspend fun previewMod(path: ImplicitUri) {
        step = Step.Preview(path, null)
        val data = try {
            path.source.use { source ->
                ModFactory.Preview(source).process()
            }
        }
        catch (e: Throwable) {
            Data.Failure(throwable = e)
        }
        step = when (data) {
            is Data.Success -> Step.Preview(path, data.data)
            is Data.Failure -> Step.Initial(data.throwable?.message ?: "未知错误", true)
        }
    }

    private suspend fun processMod(path: ImplicitUri) {
        val data = try {
            path.source.use { source ->
                ModFactory.Release(source, Paths.musicPath).process { current, total, id ->
                    step = Step.Processing(message = "解压中... [$id] $current / $total")
                }
            }
        }
        catch (e: Throwable) {
            Data.Failure(throwable = e)
        }
        when (data) {
            is Data.Success -> {
                app.musicFactory.instance.updateMusicLibraryInfo(data.data.medias)
                slot.tip.success("解压成功")
                step = Step.Initial()
            }
            is Data.Failure -> step = Step.Initial(data.throwable?.message ?: "未知错误", true)
        }
    }

    @Composable
    private fun PreviewList(
        preview: ModFactory.Preview.PreviewResult,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            item(ItemKey("Metadata")) {
                val metadata = preview.metadata
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = CustomTheme.shadow.surface,
                    border = BorderStroke(width = CustomTheme.border.small, color = Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(CustomTheme.padding.equalValue)) {
                        Text(
                            text = "MOD元信息",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(text = "MOD版本: ${metadata.version}")
                        Text(text = "媒体数: ${metadata.mediaNum}")
                        val info = metadata.info
                        Text(text = "作者: ${info.author}")
                    }
                }
            }
            items(
                items = preview.medias,
                key = { it.id }
            ) { mediaItem ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = CustomTheme.shadow.surface,
                    border = BorderStroke(width = CustomTheme.border.small, color = Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier.padding(CustomTheme.padding.equalValue),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                    ) {
                        val config = mediaItem.config
                        if (config != null) {
                            Text(
                                text = "媒体配置",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Text(text = "版本: ${config.version}")
                            Text(text = "作者: ${config.author}")
                            Text(text = "ID: ${config.id}")
                            Text(text = "名称: ${config.name}")
                            Text(text = "演唱: ${config.singer}")
                            Text(text = "作词: ${config.lyricist}")
                            Text(text = "作曲: ${config.composer}")
                            Text(text = "专辑: ${config.album}")
                            Text(text = "副歌点: ${config.chorus}")
                            Space()
                        }
                        Text(
                            text = "资源表",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        for ((resource, length) in mediaItem.resources) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .border(CustomTheme.border.small, Color.LightGray)
                                    .padding(CustomTheme.padding.equalValue),
                                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
                            ) {
                                Text(
                                    text = "${resource.type?.description ?: "未知资源"}(${resource.name})",
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = length.toLong().fileSizeString)
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun initialize() {
        args.deepLink?.let {
            step = Step.Prepare(processImportMusicDeepLink(it))
        }
    }

    override val title: String = "导入MOD"

    @Composable
    override fun ActionScope.LeftActions() {
        if (step is Step.Prepare) {
            Action(
                icon = Icons.Outlined.Refresh,
                tip = "重置",
                onClick = { reset() }
            )
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        when (val currentStep = step) {
            is Step.Initial -> {
                ActionSuspend(
                    icon = Icons.Outlined.Add,
                    tip = "添加",
                    onClick = { loadModFile() }
                )
            }
            is Step.Prepare -> {
                Action(
                    icon = Icons.Outlined.Preview,
                    tip = "预览",
                    onClick = { launch { previewMod(currentStep.path) } }
                )
                Action(
                    icon = Icons.Outlined.Check,
                    tip = "导入",
                    onClick = { launch { processMod(currentStep.path) } }
                )
            }
            is Step.Preview -> {
                if (currentStep.preview != null) {
                    Action(
                        icon = Icons.Outlined.Refresh,
                        tip = "刷新",
                        onClick = { reset() }
                    )
                    Action(
                        icon = Icons.Outlined.Check,
                        tip = "导入",
                        onClick = { launch { processMod(currentStep.path) } }
                    )
                }
            }
            is Step.Processing -> {}
        }
    }

    @Composable
    override fun Content(device: Device) {
        Box(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .padding(CustomTheme.padding.equalExtraValue)
            .dragAndDrop(
                enabled = step is Step.Initial || step is Step.Prepare,
                flag = DragFlag.File,
                onDrop = {
                    val files = (it as? DropResult.File)?.path
                    if (files != null) {
                        if (files.size == 1) step = Step.Prepare(RegularUri(files[0].toString()))
                        else slot.tip.warning("最多一次只能导入一个MOD")
                    }
                }
            ),
            contentAlignment = Alignment.Center
        ) {
            when (val currentStep = step) {
                is Step.Initial -> {
                    Text(
                        text = currentStep.message,
                        color = if (currentStep.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
                is Step.Prepare -> {
                    Text(
                        text = "已加载: ${currentStep.path.path}",
                        textAlign = TextAlign.Center
                    )
                }
                is Step.Preview -> {
                    val preview = currentStep.preview
                    if (preview == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                        ) {
                            LoadingAnimation()
                            Text(
                                text = "预览中...",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else {
                        PreviewList(
                            preview = preview,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is Step.Processing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                    ) {
                        LoadingAnimation()
                        Text(
                            text = currentStep.message,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}