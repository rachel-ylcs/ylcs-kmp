package love.yinlin.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import love.yinlin.Page
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

@Stable
object FloatingPage : Page() {
    val tip = Tip(mainScope)

    val dialogInfo = DialogInfo()

    val dialogConfirm = DialogConfirm()

    val dialogInput = DialogInput(hint = "Input something here", trailing = InputDecoration.Icon.Clear)

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
            Text(text = "hello world", modifier = Modifier.fillMaxSize())
        }
    }

    val sheetWithArgs = object : SheetContent<Int>() {
        @Composable
        override fun Content(args: Int) {
            Text(text = args.toString())
        }
    }

    val sheetLazyColumn = object : Sheet() {
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
    val fab = object : FAB(mainScope) {
        override val action: FABAction? by derivedStateOf {
            if (showFab) {
                FABAction(
                    icon = if (canFabExpand) Icons.Add else Icons.Refresh,
                    onClick = { tip.info("Refresh") }
                )
            } else null
        }

        override val expandable: Boolean by derivedStateOf { canFabExpand }
        override val menus: List<FABAction> = listOf(
            FABAction(
                icon = Icons.Check,
                onClick = { tip.success("Success") }
            ),
            FABAction(
                icon = Icons.Warning,
                onClick = { tip.warning("Warning") }
            ),
            FABAction(
                icon = Icons.Error,
                onClick = { tip.error("Error") }
            )
        )
    }

    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Dialog") {
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

                    Example("List Choice") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogChoice.open()
                        })
                    }

                    Example("Loading") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogLoading.open("Please wait 3s!") {
                                delay(3000L)
                            }
                        })
                    }

                    Example("Progress") {
                        PrimaryLoadingButton("open", onClick = {
                            dialogProgress.open {
                                total = "100"
                                for (i in 0 .. 100) {
                                    delay(50)
                                    current = i.toString()
                                    progress = i / 100f
                                }
                            }
                        })
                    }
                }
            }
            Component("Sheet") {
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
            Component("Flyout") {
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
                }
            }
            Component("Tip") {
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
            Component("FAB") {
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