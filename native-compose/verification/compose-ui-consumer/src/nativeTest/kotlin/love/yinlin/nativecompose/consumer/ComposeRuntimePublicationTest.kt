@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package love.yinlin.nativecompose.consumer

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.runBlocking
import love.yinlin.nativecompose.consumer.resources.Res
import love.yinlin.nativecompose.consumer.resources.publication_greeting
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.skottie.Animation
import kotlin.test.*

/** Foundation, Navigation3, Resources and Skottie run through the published dependency graph. */
class ComposeRuntimePublicationTest {
    @BeforeTest
    fun installGraphicsBackend() { registerSkikoComposeImplementation() }

    @Test
    fun navigationEntriesRetainTextAndIsolateAndClearViewModels() = runBlocking {
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
    fun generatedResourcesAreDeployedBesideTheNativeTestBinary() = runBlocking {
        assertEquals("Native 资源 😀", getString(Res.string.publication_greeting))
        assertEquals("Maven resource 中文 😀", Res.readBytes("files/目录 含空格.txt").decodeToString().trim())
    }

    @Test
    fun skottieExtensionLinksItsCoreAndNativeBridgeFromMaven() {
        Animation.makeFromString("""{"v":"5.7.4","fr":30,"ip":0,"op":30,"w":16,"h":24,"layers":[]}""").use { animation ->
            assertEquals(16f, animation.width)
            assertEquals(24f, animation.height)
            assertEquals(1f, animation.duration)
        }
    }
}
