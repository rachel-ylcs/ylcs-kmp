@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package love.yinlin.nativecompose.consumer

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import love.yinlin.nativecompose.consumer.resources.Res
import love.yinlin.nativecompose.consumer.resources.publication_greeting
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.skottie.Animation
import kotlin.test.*

/** Foundation, Material3, Navigation3, Resources and Skottie use the published dependency graph. */
class ComposeRuntimePublicationTest {
    @BeforeTest
    fun installGraphicsBackend() { registerSkikoComposeImplementation() }

    @Test
    fun materialDatePickerRendersAndUpdatesItsSelectionFromMaven() = runPublishedComposeTest {
        var state: DatePickerState? = null
        PublishedSceneFixture(coroutineContext).use { scene ->
            scene.content {
                MaterialTheme {
                    val picker = rememberDatePickerState(initialSelectedDateMillis = 0L)
                    SideEffect { state = picker }
                    DatePicker(picker)
                }
            }
            scene.settle()
            val picker = assertNotNull(state)
            assertEquals(0L, picker.selectedDateMillis)
            picker.selectedDateMillis = 86_400_000L
            scene.settle()
            assertEquals(86_400_000L, picker.selectedDateMillis)
        }
    }

    @Test
    fun navigationEntriesRetainTextAndIsolateAndClearViewModels() = runPublishedComposeTest {
        val stack = mutableStateListOf(0)
        val models: MutableMap<Int, PublishedEntryViewModel> = mutableMapOf()
        val texts: MutableMap<Int, TextFieldState> = mutableMapOf()
        PublishedSceneFixture(coroutineContext).use { scene ->
            scene.content {
                NavDisplay(
                    backStack = stack,
                    onBack = { stack.removeLast() },
                    entryDecorators = [rememberSaveableStateHolderNavEntryDecorator<Int>(), rememberViewModelStoreNavEntryDecorator<Int>()],
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    entryProvider = { id ->
                        NavEntry(id) {
                            val model = viewModel { PublishedEntryViewModel() }
                            val text = rememberTextFieldState("initial-$id")
                            SideEffect {
                                models[id] = model
                                texts[id] = text
                            }
                            Column {
                                BasicText("Entry $id")
                                BasicTextField(text)
                            }
                        }
                    }
                )
            }
            scene.settle()
            val first = models.getValue(0)
            texts.getValue(0).edit { replace(0, length, "retained 中文 😀") }
            stack.add(1)
            scene.settle()
            val second = models.getValue(1)
            assertNotSame(first, second)
            assertFalse(first.cleared)
            stack.removeLast()
            scene.settle()
            assertSame(first, models.getValue(0))
            assertEquals("retained 中文 😀", texts.getValue(0).text.toString())
            assertTrue(second.cleared)
            assertFalse(first.cleared)
        }
        assertTrue(models.getValue(0).cleared)
    }

    @Test
    fun generatedResourcesAreDeployedBesideTheNativeTestBinary() = runPublishedComposeTest {
        assertEquals("Native 资源 😀", getString(Res.string.publication_greeting))
        assertEquals("Maven resource 中文 😀", Res.readBytes("files/目录 含空格.txt").decodeToString().trim())
    }

    @Test
    fun skottieExtensionRendersAndEncodesImagesFromMaven() {
        Surface.makeRasterN32Premul(64, 64).use { surface ->
            surface.canvas.clear(Color.WHITE)
            Animation.makeFromString(SkottieFixture).use { animation ->
                assertEquals(64f, animation.width)
                assertEquals(64f, animation.height)
                animation.seek(.5f).render(surface.canvas)
            }
            Bitmap().use { pixels ->
                assertTrue(pixels.allocPixels(ImageInfo.makeN32Premul(64, 64)))
                assertTrue(surface.readPixels(pixels, 0, 0))
                assertEquals(Color.BLUE, pixels.getColor(16, 16))
                assertEquals(Color.WHITE, pixels.getColor(0, 0))
            }
            surface.makeImageSnapshot().use { image ->
                assertNotNull(image.encodeToData(EncodedImageFormat.PNG)).use { data ->
                    Image.makeFromEncoded(data.bytes).use { decoded ->
                        assertEquals(64, decoded.width)
                        assertEquals(64, decoded.height)
                    }
                }
            }
        }
    }
}

private const val SkottieFixture = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":64,"h":64,"ddd":0,"assets":[],"layers":[{"ddd":0,"ind":1,"ty":1,"nm":"Blue","sr":1,"ks":{"o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[8,8,0]},"a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},"sw":32,"sh":32,"sc":"#0000ff","ip":0,"op":60,"st":0,"bm":0}]}"""
