package love.yinlin.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.*
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.Text
import love.yinlin.mainScope
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Stable
object FloatingPage : Page(
    id = "floating",
    icon = Icons.Cloud,
    title = "浮窗",
    english = "Floating",
    description = "在当前页面上打开对话框、抽屉和菜单。",
    keywords = "弹窗 抽屉 提示",
    group = PageGroup.Interaction,
) {
    private val demoDialog = component("Dialog", "需要确认、输入或选择时，在当前内容上弹出对话框。", "DialogInfo DialogConfirm DialogInput DialogChoice DialogLoading DialogProgress")
    private val demoSheet = component("Sheet", "用抽屉承载补充内容或可滚动的选项列表。", "SheetContent")
    private val demoFlyout = component("Flyout", "在触发控件附近显示悬停提示或菜单。", "BalloonTip Menus")
    private val demoTip = component("Tip", "给出简短的成功、警告或错误反馈。", "消息")
    private val demoFab = component("FAB", "把常用操作放在悬浮按钮里，也可展开一组快捷操作。", "悬浮按钮")

    val tip = Tip(mainScope)

    val dialogInfo = DialogInfo()

    val dialogConfirm = DialogConfirm()

    val dialogInput = DialogInput(hint = "Input something here", trailing = InputDecoration.Icon.Clear)

    val dialogChoiceSingle = DialogChoice.fromItems(["Single"])

    val dialogChoice = DialogChoice.fromItems(List(10) { "Item $it" })

    val dialogLoading = DialogLoading()

    val dialogProgress = DialogProgress<Unit>()

    val sheetNormal = object : Sheet() {
        @Composable
        override fun Content() {
            Text(text = "hello world")
        }
    }

    val sheetFillSize = object : Sheet() {
        @Composable
        override fun Content() {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(100) {
                    Text(text = "hello world $it")
                }
            }
        }
    }

    val sheetWithArgs = object : SheetContent<Int>() {
        @Composable
        override fun Content(args: Int) {
            Text(text = args.toString())
        }
    }

    val sheetLazyColumn = object : Sheet() {
        override val scrollable: Boolean = false

        @Composable
        override fun Content() {
            LazyColumn {
                items(100) {
                    Text("Item $it")
                }
            }
        }
    }

    var showFab by mutableStateOf(true)
    var canFabExpand by mutableStateOf(true)
    val fab = object : FAB() {
        override val action: FABAction? by derivedStateOf {
            if (showFab) {
                FABAction(
                    iconProvider = { if (canFabExpand) Icons.Add else Icons.Refresh },
                    onClick = { tip.info("Refresh") }
                )
            } else null
        }

        override val expandable: Boolean by derivedStateOf { canFabExpand }
        override val menus: List<FABAction> = [
            FABAction(
                iconProvider = { Icons.Check },
                onClick = { tip.success("Success") }
            ),
            FABAction(
                iconProvider = { Icons.Warning },
                onClick = { tip.warning("Warning") }
            ),
            FABAction(
                iconProvider = { Icons.Error },
                onClick = { tip.error("Error") }
            )
        ]
    }

    @Composable
    override fun Content() {
        ComponentColumn {
            Component(demoDialog) {
                ExampleRow {
                    Example("Info") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogInfo.open("Floating Dialog Info!")
                        })
                    }

                    Example("Confirm") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogConfirm.open("Floating Dialog Confirm!")
                        })
                    }

                    Example("Input") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogInput.open("Floating Dialog Input!")
                        })
                    }

                    Example("List Choice Single") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogChoiceSingle.open()
                        })
                    }

                    Example("List Choice") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogChoice.open()
                        })
                    }

                    Example("Loading") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogLoading.open("Please wait 3s!") {
                                delay(3.seconds)
                            }
                        })
                    }

                    Example("Progress") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogProgress.open {
                                total = "100"
                                for (i in 0 .. 100) {
                                    delay(50.milliseconds)
                                    current = i.toString()
                                    progress = i / 100f
                                }
                            }
                        })
                    }
                }
            }

            Component(demoSheet) {
                ExampleRow {
                    Example("Normal") {
                        PrimaryLoadingButton("open", onClick = {
                            sheetNormal.open()
                        })
                    }

                    Example("FillSize") {
                        PrimaryLoadingButton("open", onClick = {
                            sheetFillSize.open()
                        })
                    }

                    Example("WithArgs") {
                        PrimaryLoadingButton("open", onClick = {
                            sheetWithArgs.open(Random.nextInt())
                        })
                    }

                    Example("LazyColumn") {
                        PrimaryLoadingButton("open", onClick = {
                            sheetLazyColumn.open()
                        })
                    }
                }
            }

            Component(demoFlyout) {
                ExampleRow {
                    Example("BalloonTip") {
                        BalloonTip(enabled = true, text = "Click Me!") {
                            PrimaryButton(text = "Hovered", onClick = {})
                        }
                    }

                    Example("Flyout Card") {
                        var visible by rememberFalse()
                        Flyout(
                            visible = visible,
                            onClickOutside = { visible = false },
                            clip = true,
                            flyout = {
                                Surface(
                                    modifier = Modifier.size(Theme.size.cell1, Theme.size.cell3),
                                    shape = Theme.shape.v3,
                                    shadowElevation = Theme.shadow.v3,
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                                        Text(text = "hello world")
                                        SecondaryButton(text = "close", onClick = { visible = false })
                                    }
                                }
                            }
                        ) {
                            PrimaryButton("open", onClick = { visible = true })
                        }
                    }

                    Example("Menus") {
                        var visible by rememberFalse()

                        Menus(
                            visible = visible,
                            onClose = { visible = false },
                            menus = {
                                Menu("Home", icon = Icons.Home)
                                Menu("Edit", icon = Icons.Edit, enabled = false)
                                Menu(icon = Icons.Delete)
                                Menu("Exit")
                            }
                        ) {
                            PrimaryButton("Open", onClick = { visible = true })
                        }
                    }
                }
            }

            Component(demoTip) {
                ExampleRow {
                    Example("Info") {
                        PrimaryLoadingButton("open", onClick = {
                            tip.info("tip info!")
                        })
                    }

                    Example("Success") {
                        PrimaryLoadingButton("open", onClick = {
                            tip.success("tip success!")
                        })
                    }

                    Example("Warning") {
                        PrimaryLoadingButton("open", onClick = {
                            tip.warning("tip warning!")
                        })
                    }

                    Example("Error") {
                        PrimaryLoadingButton("open", onClick = {
                            tip.error("tip error!")
                        })
                    }
                }
            }

            Component(demoFab) {
                ExampleRow {
                    Example("enabled: ${fab.visible}") {
                        PrimaryLoadingButton("toggle", onClick = {
                            showFab = !showFab
                        })
                    }
                    Example("expandable: ${fab.expandable}") {
                        PrimaryLoadingButton("toggle", onClick = {
                            canFabExpand = !canFabExpand
                        })
                    }
                }
            }
        }

        dialogInfo.Land()
        dialogConfirm.Land()
        dialogInput.Land()
        dialogChoiceSingle.Land()
        dialogChoice.Land()
        dialogLoading.Land()
        dialogProgress.Land()

        sheetNormal.Land()
        sheetFillSize.Land()
        sheetWithArgs.Land()
        sheetLazyColumn.Land()

        tip.Land()

        fab.Land()
    }
}