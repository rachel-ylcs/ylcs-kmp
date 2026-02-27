package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.app
import love.yinlin.common.PathMod
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.mod.ModPreviewLayout
import love.yinlin.compose.ui.node.DragFlag
import love.yinlin.compose.ui.node.DropResult
import love.yinlin.compose.ui.node.dragAndDrop
import love.yinlin.compose.ui.text.Text
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.MimeType
import love.yinlin.extension.catchingError
import love.yinlin.extension.lazyProvider
import love.yinlin.mod.ModFactory
import love.yinlin.platform.Platform
import love.yinlin.platform.UnsupportedPlatformText
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import love.yinlin.uri.Uri
import love.yinlin.uri.asFileImplicitUri

@Stable
class ScreenImportMusic(private val deeplink: Uri?) : Screen() {
    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

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
            *Platform.Web,
            ifTrue = { slot.tip.warning(UnsupportedPlatformText) },
            ifFalse = {
                app.picker.pickPath(mimeType = listOf(MimeType.BINARY), filter = listOf("*.rachel"))?.let {
                    step = Step.Prepare(it)
                }
            }
        )
    }

    private suspend fun previewMod(path: ImplicitUri) {
        step = Step.Preview(path, null)

        catchingError {
            val data = Coroutines.io {
                path.read { ModFactory.Preview(it).process() }
            }
            step = Step.Preview(path, data)
        }?.let {
            step = Step.Initial(it.message ?: "未知错误", isError = true)
        }
    }

    private suspend fun processMod(path: ImplicitUri) {
        mp?.let { player ->
            if (player.isReady) slot.tip.warning("请先停止播放器")
            else catchingError {
                val data = Coroutines.io {
                    path.read { source ->
                        ModFactory.Release(source, PathMod).process { current, total, id ->
                            step = Step.Processing(message = "解压中... [$id] $current / $total")
                        }
                    }
                }
                player.updateMusicLibraryInfo(data.medias)
                slot.tip.success("解压成功")
                step = Step.Initial()
            }?.let {
                step = Step.Initial(it.message ?: "未知错误", isError = true)
            }
        }
    }

    override val title: String = "导入MOD"

    override suspend fun initialize() {
        deeplink?.let { uri ->
            catchingError {
                step = Step.Prepare(uri.asFileImplicitUri(app.context))
            }?.let {
                step = Step.Initial(it.message ?: "未知错误", isError = true)
            }
        }
    }

    @Composable
    override fun RowScope.LeftActions() {
        if (step is Step.Prepare) Icon(icon = Icons.Refresh, tip = "重置", onClick = ::reset)
    }

    @Composable
    override fun RowScope.RightActions() {
        when (val currentStep = step) {
            is Step.Initial -> {
                LoadingIcon(icon = Icons.Add, tip = "添加", onClick = ::loadModFile)
            }
            is Step.Prepare -> {
                Icon(icon = Icons.Preview, tip = "预览", onClick = {
                    launch { previewMod(currentStep.path) }
                })
                Icon(icon = Icons.Check, tip = "导入", onClick = {
                    launch { processMod(currentStep.path) }
                })
            }
            is Step.Preview -> {
                if (currentStep.preview != null) {
                    Icon(icon = Icons.Refresh, tip = "刷新", onClick = ::reset)
                    Icon(icon = Icons.Check, tip = "导入", onClick = {
                        launch { processMod(currentStep.path) }
                    })
                }
            }
            is Step.Processing -> {}
        }
    }

    @Composable
    override fun Content() {
        Box(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .padding(Theme.padding.eValue9)
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
                        color = if (currentStep.isError) Theme.color.error else LocalColor.current,
                        textAlign = TextAlign.Center
                    )
                }
                is Step.Prepare -> Text(text = "已加载: ${currentStep.path.path}", textAlign = TextAlign.Center)
                is Step.Preview -> {
                    val result = currentStep.preview
                    if (result == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                        ) {
                            CircleLoading.Content()
                            Text(text = "预览中...", textAlign = TextAlign.Center)
                        }
                    }
                    else ModPreviewLayout(modifier = Modifier.fillMaxSize(), result = result)
                }
                is Step.Processing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        CircleLoading.Content()
                        Text(text = currentStep.message, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}