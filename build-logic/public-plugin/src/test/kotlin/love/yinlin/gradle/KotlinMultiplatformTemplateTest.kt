package love.yinlin.gradle

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinMultiplatformTemplateTest {
    @Test
    fun unspecifiedPlatformsFollowTheDesktopDefault() {
        val template = KotlinMultiplatformTemplate()
        assertFalse(template.nativeDesktopTarget)
        assertFalse(template.windowsTarget)
        assertFalse(template.linuxTarget)
        assertFalse(template.macosTarget)
        template.nativeDesktopTarget = true
        assertTrue(template.windowsTarget)
        assertTrue(template.linuxTarget)
        assertTrue(template.macosTarget)
        template.nativeDesktopTarget = false
        assertFalse(template.windowsTarget)
        assertFalse(template.linuxTarget)
        assertFalse(template.macosTarget)
    }

    @Test
    fun explicitPlatformValuesOverrideTheDefaultInEitherAssignmentOrder() {
        for (defaultFirst in listOf(false, true)) {
            val template = KotlinMultiplatformTemplate()
            if (defaultFirst) template.nativeDesktopTarget = true
            template.windowsTarget = false
            if (!defaultFirst) template.nativeDesktopTarget = true
            assertFalse(template.windowsTarget)
            assertTrue(template.linuxTarget)
            assertTrue(template.macosTarget)

            if (defaultFirst) template.nativeDesktopTarget = false
            template.macosTarget = true
            if (!defaultFirst) template.nativeDesktopTarget = false
            assertFalse(template.windowsTarget)
            assertFalse(template.linuxTarget)
            assertTrue(template.macosTarget)

            template.linuxTarget = false
            template.macosTarget = false
            template.nativeDesktopTarget = true
            assertFalse(template.windowsTarget)
            assertFalse(template.linuxTarget)
            assertFalse(template.macosTarget)
        }
    }
}
