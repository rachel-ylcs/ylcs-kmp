package love.yinlin.screen.music

//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import love.yinlin.compose.*
//import love.yinlin.compose.ui.input.Switch
//import love.yinlin.compose.ui.input.DockedColorPicker
//import love.yinlin.compose.ui.input.ProgressSlider
//import love.yinlin.compose.ui.layout.SplitLayout
//import love.yinlin.fixup.FixupMacOSMouseClick
//
//@Composable
//actual fun ScreenFloatingLyrics.ActualContent(device: Device) {
//    var desktopConfig by rememberRefState { service.config.floatingLyricsDesktopConfig }
//
//    Column(modifier = Modifier
//        .padding(LocalImmersivePadding.current)
//        .fillMaxSize()
//        .padding(CustomTheme.padding.equalExtraValue)
//        .verticalScroll(rememberScrollState()),
//        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
//    ) {
//        (service.musicFactory.instance.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
//            DisposableEffect(Unit) {
//                desktopConfig = service.config.floatingLyricsDesktopConfig
//                floatingLyrics.canMove = true
//                FixupMacOSMouseClick.setupDelay(floatingLyrics.isAttached) { floatingLyrics.isAttached = it }
//                onDispose {
//                    floatingLyrics.canMove = false
//                    FixupMacOSMouseClick.setupDelay(floatingLyrics.isAttached) { floatingLyrics.isAttached = it }
//                }
//            }
//
//            RowLayout("悬浮歌词模式") {
//                Switch(
//                    checked = service.config.enabledFloatingLyrics,
//                    onCheckedChange = {
//                        floatingLyrics.isAttached = it
//                        service.config.enabledFloatingLyrics = it
//                    }
//                )
//            }
//
//            RowLayout("字体大小") {
//                ProgressSlider(
//                    value = desktopConfig.textSizeProgress,
//                    onValueChange = { desktopConfig = desktopConfig.copyTextSize(it) },
//                    onValueChangeFinished = { service.config.floatingLyricsDesktopConfig = desktopConfig },
//                    modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
//                )
//            }
//
//            SplitLayout(
//                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
//                horizontalArrangement = CustomTheme.padding.horizontalExtraSpace * 2,
//                left = {
//                    ColumnLayout("字体颜色") {
//                        DockedColorPicker(
//                            initialColor = Colors(service.config.floatingLyricsDesktopConfig.textColor),
//                            onColorChanged = { desktopConfig = desktopConfig.copy(textColor = it.value) },
//                            onColorChangeFinished = { service.config.floatingLyricsDesktopConfig = desktopConfig },
//                            modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
//                        )
//                    }
//                },
//                right = {
//                    ColumnLayout("背景颜色") {
//                        DockedColorPicker(
//                            initialColor = Colors(service.config.floatingLyricsDesktopConfig.backgroundColor),
//                            onColorChanged = { desktopConfig = desktopConfig.copy(backgroundColor = it.value) },
//                            onColorChangeFinished = { service.config.floatingLyricsDesktopConfig = desktopConfig },
//                            modifier = Modifier.widthIn(max = CustomTheme.size.cellWidth).fillMaxWidth()
//                        )
//                    }
//                }
//            )
//        }
//    }
//}