package love.yinlin.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay
import love.yinlin.Page
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.scaleSize
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.*
import love.yinlin.compose.ui.text.Text

@Stable
object InputPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Button") {
                ExampleRow {
                    Example("PrimaryButton") {
                        PrimaryButton("Click me", onClick = {})
                    }

                    Example("SecondaryButton") {
                        SecondaryButton("Click me", onClick = {})
                    }

                    Example("TertiaryButton") {
                        TertiaryButton("Click me", onClick = {})
                    }

                    Example("IconButton") {
                        TertiaryButton("Click me", icon = Icons.Home, onClick = {})
                    }

                    Example("Disabled") {
                        PrimaryButton("Click me", icon = Icons.Home, onClick = {}, enabled = false)
                    }

                    Example("LoadingButton") {
                        PrimaryLoadingButton("Click me", onClick = { delay(2000) })
                    }

                    Example("LoadingIconButton") {
                        PrimaryLoadingButton("Click me", icon = Icons.Home, onClick = { delay(2000) })
                    }

                    Example("Disabled") {
                        PrimaryLoadingButton("Click me", icon = Icons.Home, enabled = false, onClick = {})
                    }

                    Example("TextButton") {
                        PrimaryTextButton("Click me", onClick = {})
                    }

                    Example("IconTextButton") {
                        PrimaryTextButton("Click me", icon = Icons.Home, onClick = {})
                    }

                    Example("Disabled") {
                        PrimaryTextButton("Click me", icon = Icons.Home, onClick = {}, enabled = false)
                    }

                    Example("LoadingTextButton") {
                        PrimaryLoadingTextButton("Click me", onClick = { delay(2000) })
                    }

                    Example("LoadingIconTextButton") {
                        PrimaryLoadingTextButton("Click me", icon = Icons.Home, onClick = { delay(2000) })
                    }

                    Example("Disabled") {
                        PrimaryLoadingTextButton("Click me", icon = Icons.Home, onClick = {}, enabled = false)
                    }
                }
            }

            Component("Switch") {
                ExampleRow {
                    var checked by rememberFalse()

                    Example("Normal") {
                        Switch(checked, { checked = it })
                    }

                    Example("Normal") {
                        Switch(!checked, { checked = !it })
                    }

                    Example("Disabled") {
                        Switch(checked, enabled = false)
                    }
                }
            }

            Component("Radio / CheckBox") {
                ExampleRow {
                    Example("RadioGroup") {
                        val group = rememberRadioGroup(0)
                        Radio(group, 0, "选项1")
                        Radio(group, 1, "选项2")
                        Radio(group, 2, "选项3")
                        Radio(group, 3, "Disabled", enabled = false)
                    }

                    Example("CheckBox") {
                        var checked by rememberFalse()
                        CheckBox(checked, { checked = it }, "选项")
                        CheckBox(checked, { checked = it }, "Disabled", enabled = false)
                    }

                    Example("TriStateCheckBox") {
                        var state by rememberState { ToggleableState.Off }
                        TriStateCheckBox(state, { state = it }, "选项")
                        TriStateCheckBox(state, { state = it }, "Disabled", enabled = false)
                    }
                }
            }

            Component("Slider") {
                ExampleRow {
                    var value by rememberValueState(0f)

                    Example(value.toString(), modifier = Modifier.width(Theme.size.input2)) {
                        Slider(
                            value = value,
                            onValueChange = { value = it },
                            onValueChangeFinished = { value = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Example("NoThumb", modifier = Modifier.width(Theme.size.input2)) {
                        Slider(
                            value = value,
                            onValueChange = { value = it },
                            onValueChangeFinished = { value = it },
                            showThumb = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Example("Disabled", modifier = Modifier.width(Theme.size.input2)) {
                        Slider(
                            value = value,
                            onValueChange = { value = it },
                            onValueChangeFinished = { value = it },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Example("Convert", modifier = Modifier.width(Theme.size.input1)) {
                        var convertValue by rememberState { '天' }

                        val converter = remember { object : SliderConverter<Char> {
                            val text = "江流天地外，山色有无中"
                            override fun from(value: Char): Float = (text.indexOf(value) + 1) / text.length.toFloat()
                            override fun to(value: Float): Char = text.getOrElse((value * text.length).toInt() - 1) { Char.MIN_VALUE }
                        } }

                        val activeColor = Theme.color.primary
                        val activeStyle = LocalStyle.current.scaleSize(1.5f, true)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
                        ) {
                            Text(
                                text = remember(converter, convertValue) {
                                    buildAnnotatedString {
                                        val text = converter.text
                                        val ch = convertValue
                                        val index = text.indexOf(ch)
                                        if (index == -1) append(text)
                                        else {
                                            append(text.substring(0, index))
                                            withStyle(SpanStyle(
                                                color = activeColor,
                                                fontSize = activeStyle.fontSize,
                                                fontWeight = activeStyle.fontWeight
                                            )) { append(ch) }
                                            append(text.substring(index + 1))
                                        }
                                    }
                                }
                            )
                            Slider(
                                value = convertValue,
                                converter = converter,
                                onValueChange = { convertValue = it },
                                onValueChangeFinished = { convertValue = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Component("ComboBox") {
                ExampleRow {
                    val items = remember { (1 .. 20).map { "Item $it" } }
                    var current by rememberValueState(-1)

                    Example("Normal") {
                        ComboBox(
                            items = items,
                            hint = "Select Name",
                            onSelect = { current = it },
                            index = current,
                        )
                    }

                    Example("Width", modifier = Modifier.weight(1f)) {
                        ComboBox(
                            items = items,
                            hint = "Select Name",
                            onSelect = { current = it },
                            index = current,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Example("Disabled") {
                        ComboBox(
                            items = items,
                            hint = "Select Name",
                            onSelect = { current = it },
                            index = current,
                            enabled = false
                        )
                    }
                }
            }

            Component("Filter") {
                ExampleRow {
                    Example("SingleFilter", modifier = Modifier.weight(1f)) {
                        var currentIndex by rememberValueState(1)

                        Filter(
                            modifier = Modifier.fillMaxWidth(),
                            size = 4,
                            selectedProvider = { currentIndex == it },
                            titleProvider = { "Item $it" },
                            iconProvider = { if (it == 2) Icons.Home else null },
                            enabledProvider = { it != 3 },
                            onClick = { index, selected -> currentIndex = if (selected) index else -1 }
                        )
                    }

                    Example("MultiFilter", modifier = Modifier.weight(1f)) {
                        val selectedList = remember { mutableStateListOf(true, false, false, true) }

                        Filter(
                            modifier = Modifier.fillMaxWidth(),
                            size = selectedList.size,
                            selectedProvider = { selectedList.getOrNull(it) ?: false },
                            titleProvider = { "Item $it" },
                            iconProvider = { if (it == 2) Icons.Home else null },
                            enabledProvider = { it != 3 },
                            onClick = { index, selected -> selectedList[index] = selected }
                        )
                    }
                }
            }

            Component("ColorPicker") {
                ColorPicker(onColorChangeFinished = {})
            }
        }
    }
}