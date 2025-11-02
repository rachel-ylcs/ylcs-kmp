package love.yinlin.screen.music.loader

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.app
import love.yinlin.common.Paths
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.animation.LoadingAnimation
import love.yinlin.compose.ui.mod.ModPreviewLayout
import love.yinlin.data.MimeType
import love.yinlin.mod.ModFactory
import love.yinlin.platform.*
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.node.DragFlag
import love.yinlin.compose.ui.node.DropResult
import love.yinlin.compose.ui.node.dragAndDrop
import love.yinlin.extension.catchingError
import love.yinlin.extension.toImplicitUri
import love.yinlin.uri.Uri

@Stable
class ScreenImportMusic(manager: ScreenManager, private val deeplink: Uri?) : Screen(manager) {
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

        catchingError {
            val data = Coroutines.io {
                path.read { source ->
                    ModFactory.Preview(source).process()
                }
            }
            step = Step.Preview(path, data)
        }?.let {
            step = Step.Initial(it.message ?: "未知错误", isError = true)
        }
    }

    private suspend fun processMod(path: ImplicitUri) {
        if (app.mp.isReady) slot.tip.warning("请先停止播放器")
        else catchingError {
            val data = Coroutines.io {
                path.read { source ->
                    ModFactory.Release(source, Paths.modPath).process { current, total, id ->
                        step = Step.Processing(message = "解压中... [$id] $current / $total")
                    }
                }
            }
            app.mp.updateMusicLibraryInfo(data.medias)
            slot.tip.success("解压成功")
            step = Step.Initial()
        }?.let {
            step = Step.Initial(it.message ?: "未知错误", isError = true)
        }
    }

    override suspend fun initialize() {
        deeplink?.let {  uri ->
            catchingError {
                step = Step.Prepare(uri.toImplicitUri(app.context))
            }?.let {
                step = Step.Initial(it.message ?: "未知错误", isError = true)
            }
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
                    val result = currentStep.preview
                    if (result == null) {
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
                        ModPreviewLayout(
                            modifier = Modifier.fillMaxSize(),
                            result = result
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