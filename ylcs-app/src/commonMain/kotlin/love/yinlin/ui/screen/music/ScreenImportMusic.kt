package love.yinlin.ui.screen.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.utils.io.core.readText
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.data.Data
import love.yinlin.data.MimeType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.fileSizeString
import love.yinlin.extension.itemKey
import love.yinlin.extension.parseJsonValue
import love.yinlin.mod.ModFactory
import love.yinlin.platform.*
import love.yinlin.ui.component.layout.LoadingAnimation
import love.yinlin.ui.component.platform.DragFlag
import love.yinlin.ui.component.platform.DropResult
import love.yinlin.ui.component.platform.dragAndDrop
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen

expect fun processImportMusicDeepLink(deepLink: String): ImplicitPath

@Stable
class ScreenImportMusic(model: AppModel, private val args: Args) : Screen<ScreenImportMusic.Args>(model) {
    @Stable
    @Serializable
    data class Args(val deepLink: String?) : Screen.Args

    @Stable
    private sealed interface Step {
        @Stable
        data class Initial(val message: String = "未加载文件", val isError: Boolean = false) : Step
        @Stable
        data class Prepare(val path: ImplicitPath) : Step
        @Stable
        data class Preview(val path: ImplicitPath, val preview: ModFactory.Preview.PreviewResult?) : Step
        @Stable
        data class Processing(val message: String) : Step
    }

    private var step: Step by mutableStateOf(Step.Initial())

    private fun reset() {
        step = Step.Initial()
    }

    private suspend fun loadModFile() {
        OS.runNotWeb(web = {
            slot.tip.warning(UnsupportedPlatformText)
        }) {
            Picker.pickPath(mimeType = listOf(MimeType.BINARY), filter = listOf("*.rachel"))?.let {
                step = Step.Prepare(it)
            }
        }
    }

    private suspend fun previewMod(path: ImplicitPath) {
        step = Step.Preview(path, null)
        val data = try {
            path.source.use { source ->
                ModFactory.Preview(source).process()
            }
        }
        catch (e: Throwable) {
            Data.Error(throwable = e)
        }
        step = when (data) {
            is Data.Success -> Step.Preview(path, data.data)
            is Data.Error -> Step.Initial(data.throwable?.message ?: "未知错误", true)
        }
    }

    private suspend fun processMod(path: ImplicitPath) {
        val data = try {
            path.source.use { source ->
                ModFactory.Release(source, OS.Storage.musicPath).process { current, total, id ->
                    step = Step.Processing(message = "解压中... [$id] $current / $total")
                }
            }
        }
        catch (e: Throwable) {
            Data.Error(throwable = e)
        }
        when (data) {
            is Data.Success -> {
                val musicLibrary = app.musicFactory.musicLibrary
                for (id in data.data.medias) {
                    val modifierCount = musicLibrary[id]?.modifier ?: 0
                    val info = Coroutines.io {
                        try {
                            val configPath = Path(OS.Storage.musicPath, id, MusicResourceType.Config.defaultFilename)
                            SystemFileSystem.source(configPath).buffered().use { it.readText().parseJsonValue<MusicInfo>() }!!
                        }
                        catch (_: Throwable) {
                            null
                        }
                    }
                    if (info != null) musicLibrary[id] = info.copy(modifier = modifierCount + 1)
                }
                slot.tip.success("解压成功")
                step = Step.Initial()
            }
            is Data.Error -> step = Step.Initial(data.throwable?.message ?: "未知错误", true)
        }
    }

    @Composable
    private fun PreviewList(
        preview: ModFactory.Preview.PreviewResult,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item("metadata".itemKey) {
                val metadata = preview.metadata
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 5.dp,
                    border = BorderStroke(width = 1.dp, color = Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
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
                    shadowElevation = 5.dp,
                    border = BorderStroke(width = 1.dp, color = Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Text(
                            text = "资源表",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        for (resourceItem in mediaItem.resources) {
                            Row(
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray).padding(5.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = "${resourceItem.type.description}(${resourceItem.name})",
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = resourceItem.length.toLong().fileSizeString)
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

    @Composable
    override fun content() {
        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = "导入歌曲",
            onBack = { pop() },
            actions = {
                when (val currentStep = step) {
                    is Step.Initial -> {
                        ActionSuspend(
                            icon = Icons.Outlined.Add,
                            onClick = { loadModFile() }
                        )
                    }
                    is Step.Prepare -> {
                        Action(
                            icon = Icons.Outlined.Refresh,
                            onClick = { reset() }
                        )
                        Action(
                            icon = Icons.Outlined.Preview,
                            onClick = { launch { previewMod(currentStep.path) } }
                        )
                        Action(
                            icon = Icons.Outlined.Check,
                            onClick = { launch { processMod(currentStep.path) } }
                        )
                    }
                    is Step.Preview -> {
                        if (currentStep.preview != null) {
                            Action(
                                icon = Icons.Outlined.Refresh,
                                onClick = { reset() }
                            )
                            Action(
                                icon = Icons.Outlined.Check,
                                onClick = { launch { processMod(currentStep.path) } }
                            )
                        }
                    }
                    is Step.Processing -> {}
                }
            },
            leftActions = {
                Action(Icons.Outlined.Extension) {

                }
            },
            slot = slot
        ) {
            Box(
                modifier = Modifier.fillMaxSize().dragAndDrop(
                    enabled = step is Step.Initial || step is Step.Prepare,
                    flag = DragFlag.FILE,
                    onDrop = {
                        val files = (it as? DropResult.File)?.path
                        if (files != null) {
                            if (files.size == 1) step = Step.Prepare(NormalPath(files[0].toString()))
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
                            color = if (currentStep.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
                            Column {
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
                        Column {
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
}