package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import love.yinlin.GalleryActions
import love.yinlin.GalleryBadge
import love.yinlin.GalleryCaption
import love.yinlin.Page
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.scaleSize
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.*
import love.yinlin.compose.ui.text.Text
import kotlin.time.Duration.Companion.seconds

@Stable
object InputPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Button") {
                val sample = Modifier.widthIn(min = Theme.size.input4)
                val load: suspend () -> Unit = { delay(2.seconds) }

                ButtonGroup("填充按钮", "Button", content = {
                    PrimaryButton("主要", modifier = sample, onClick = {})
                    SecondaryButton("次要", modifier = sample, onClick = {})
                    TertiaryButton("辅助", modifier = sample, onClick = {})
                    TertiaryButton("带图标", icon = Icons.Home, modifier = sample, onClick = {})
                    PrimaryButton("已禁用", icon = Icons.Home, enabled = false, modifier = sample, onClick = {})
                }) {
                    PrimaryLoadingButton("主要", modifier = sample, onClick = load)
                    SecondaryLoadingButton("次要", modifier = sample, onClick = load)
                    TertiaryLoadingButton("辅助", modifier = sample, onClick = load)
                    PrimaryLoadingButton("带图标", icon = Icons.Home, modifier = sample, onClick = load)
                    PrimaryLoadingButton("已禁用", icon = Icons.Home, enabled = false, modifier = sample, onClick = {})
                }

                ButtonGroup("描边按钮", "OutlinedButton", content = {
                    PrimaryOutlinedButton("主要", modifier = sample, onClick = {})
                    SecondaryOutlinedButton("次要", modifier = sample, onClick = {})
                    TertiaryOutlinedButton("辅助", modifier = sample, onClick = {})
                    PrimaryOutlinedButton("带图标", icon = Icons.Home, modifier = sample, onClick = {})
                    PrimaryOutlinedButton("已禁用", icon = Icons.Home, enabled = false, modifier = sample, onClick = {})
                }) {
                    PrimaryLoadingOutlinedButton("主要", modifier = sample, onClick = load)
                    SecondaryLoadingOutlinedButton("次要", modifier = sample, onClick = load)
                    TertiaryLoadingOutlinedButton("辅助", modifier = sample, onClick = load)
                    PrimaryLoadingOutlinedButton("带图标", icon = Icons.Home, modifier = sample, onClick = load)
                    PrimaryLoadingOutlinedButton("已禁用", icon = Icons.Home, enabled = false, modifier = sample, onClick = {})
                }

                ButtonGroup("文字按钮", "TextButton", content = {
                    PrimaryTextButton("主要", modifier = sample, onClick = {})
                    SecondaryTextButton("次要", modifier = sample, onClick = {})
                    TertiaryTextButton("辅助", modifier = sample, onClick = {})
                    PrimaryTextButton("带图标", icon = Icons.Home, modifier = sample, onClick = {})
                    PrimaryTextButton("已禁用", icon = Icons.Home, enabled = false, modifier = sample, onClick = {})
                }) {
                    PrimaryLoadingTextButton("主要", modifier = sample, onClick = load)
                    SecondaryLoadingTextButton("次要", modifier = sample, onClick = load)
                    TertiaryLoadingTextButton("辅助", modifier = sample, onClick = load)
                    PrimaryLoadingTextButton("带图标", icon = Icons.Home, modifier = sample, onClick = load)
                    PrimaryLoadingTextButton("已禁用", icon = Icons.Home, enabled = false, modifier = sample, onClick = {})
                }
            }

            Component("Switch") {
                ExampleRow {
                    var checked by rememberFalse()

                    Example("Normal") {
                        Switch(checked, onCheckedChange = { checked = it })
                    }

                    Example("Normal") {
                        Switch(!checked, onCheckedChange = { checked = !it })
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
                                text = remember(converter, convertValue, activeColor, activeStyle) {
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

            Component("StarBar") {
                val normalState by rememberState { StarBarState(3) }
                val halfState by rememberState { StarBarState(2.5) }

                ExampleRow {
                    Example("整星 · ${normalState.value.toInt()}") {
                        StarBar(state = normalState)
                    }

                    Example("半星 · ${halfState.value}") {
                        StarBar(state = halfState, half = true)
                    }

                    Example("7 颗 / 大尺寸") {
                        StarBar(state = normalState, num = 7, modifier = Modifier.height(Theme.size.input8))
                    }

                    Example("已禁用") {
                        StarBar(state = halfState, half = true, enable = false)
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

            Component("DatePicker") {
                val sampleStyle = Theme.typography.v6
                ExampleRow {
                    Example("日期 · 点击选择") {
                        var chosen by rememberState { kotlinx.datetime.LocalDate(2026, 11, 14) }
                        DatePicker(date = chosen, onDateChanged = { chosen = it }, style = sampleStyle)
                    }

                    Example("年份 · 横向翻页") {
                        DatePicker(
                            date = kotlinx.datetime.LocalDate(2026, 11, 14),
                            onDateChanged = {},
                            style = sampleStyle,
                            initialView = DatePickerView.Years
                        )
                    }

                    Example("月份 · 跨年翻页") {
                        DatePicker(
                            date = kotlinx.datetime.LocalDate(2026, 11, 14),
                            onDateChanged = {},
                            style = sampleStyle,
                            initialView = DatePickerView.Months
                        )
                    }

                    Example("闰年 · 2024 年 2 月") {
                        var chosen by rememberState { kotlinx.datetime.LocalDate(2024, 2, 29) }
                        DatePicker(date = chosen, onDateChanged = { chosen = it }, style = sampleStyle)
                    }
                }
            }

            Component("ColorPicker") {
                ColorPicker(onColorChangeFinished = {})
            }
        }
    }

    @Composable
    private fun ButtonGroup(
        title: String,
        api: String,
        content: @Composable FlowRowScope.() -> Unit,
        loadingContent: @Composable FlowRowScope.() -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Theme.color.backgroundVariant.copy(alpha = 0.45f), Theme.shape.v3)
                .padding(Theme.padding.eValue6),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.padding.h), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = Theme.typography.v6.bold)
                GalleryBadge(api, Theme.color.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                GalleryCaption("普通")
                GalleryActions(content)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                GalleryCaption("加载")
                GalleryActions(loadingContent)
            }
        }
    }
}
