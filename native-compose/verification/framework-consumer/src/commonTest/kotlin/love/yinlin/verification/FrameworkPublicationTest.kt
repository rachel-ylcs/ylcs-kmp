package love.yinlin.verification

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameworkPublicationTest {
    @Test
    fun frameworkExportsItsComposeApiOnTheSelectedPlatform() {
        assertEquals(3f, frameworkDensity(Density(2f, 1.5f)))
    }

    @Test
    fun frameworkResourcesArriveThroughMavenAndRemainReadable() = runBlocking {
        assertEquals("Framework Maven resource 中文 😀", readFrameworkResource())
    }
}

@Composable
private fun ConsumerContent() {
    FrameworkContent(Modifier) {}
}
