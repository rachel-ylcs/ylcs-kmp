package love.yinlin.compose

import androidx.compose.material3.Typography
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

@Stable
data class TextSystem(
    val displayLarge: ModeText,
    val displayMedium: ModeText,
    val displaySmall: ModeText,
    val headlineLarge: ModeText,
    val headlineMedium: ModeText,
    val headlineSmall: ModeText,
    val titleLarge: ModeText,
    val titleMedium: ModeText,
    val titleSmall: ModeText,
    val bodyLarge: ModeText,
    val bodyMedium: ModeText,
    val bodySmall: ModeText,
    val labelLarge: ModeText,
    val labelMedium: ModeText,
    val labelSmall: ModeText,
) {
    fun toTypography(font: FontFamily, size: Device.Size): Typography = when (size) {
        Device.Size.SMALL -> Typography(
            displayLarge = displayLarge.small(font),
            displayMedium = displayMedium.small(font),
            displaySmall = displaySmall.small(font),
            headlineLarge = headlineLarge.small(font),
            headlineMedium = headlineMedium.small(font),
            headlineSmall = headlineSmall.small(font),
            titleLarge = titleLarge.small(font),
            titleMedium = titleMedium.small(font),
            titleSmall = titleSmall.small(font),
            bodyLarge = bodyLarge.small(font),
            bodyMedium = bodyMedium.small(font),
            bodySmall = bodySmall.small(font),
            labelLarge = labelLarge.small(font),
            labelMedium = labelMedium.small(font),
            labelSmall = labelSmall.small(font),
        )
        Device.Size.MEDIUM -> Typography(
            displayLarge = displayLarge.medium(font),
            displayMedium = displayMedium.medium(font),
            displaySmall = displaySmall.medium(font),
            headlineLarge = headlineLarge.medium(font),
            headlineMedium = headlineMedium.medium(font),
            headlineSmall = headlineSmall.medium(font),
            titleLarge = titleLarge.medium(font),
            titleMedium = titleMedium.medium(font),
            titleSmall = titleSmall.medium(font),
            bodyLarge = bodyLarge.medium(font),
            bodyMedium = bodyMedium.medium(font),
            bodySmall = bodySmall.medium(font),
            labelLarge = labelLarge.medium(font),
            labelMedium = labelMedium.medium(font),
            labelSmall = labelSmall.medium(font),
        )
        Device.Size.LARGE -> Typography(
            displayLarge = displayLarge.large(font),
            displayMedium = displayMedium.large(font),
            displaySmall = displaySmall.large(font),
            headlineLarge = headlineLarge.large(font),
            headlineMedium = headlineMedium.large(font),
            headlineSmall = headlineSmall.large(font),
            titleLarge = titleLarge.large(font),
            titleMedium = titleMedium.large(font),
            titleSmall = titleSmall.large(font),
            bodyLarge = bodyLarge.large(font),
            bodyMedium = bodyMedium.large(font),
            bodySmall = bodySmall.large(font),
            labelLarge = labelLarge.large(font),
            labelMedium = labelMedium.large(font),
            labelSmall = labelSmall.large(font),
        )
    }
}

val DefaultTextSystem = TextSystem(
    displayLarge = ModeText(true, 28, 30, 32),
    displayMedium = ModeText(true, 24, 26, 28),
    displaySmall = ModeText(true, 20, 22, 24),
    headlineLarge = ModeText(false, 28, 30, 32),
    headlineMedium = ModeText(false, 24, 26, 28),
    headlineSmall = ModeText(false, 20, 22, 24),
    titleLarge = ModeText(true, 17, 18, 19),
    titleMedium = ModeText(true, 15, 16, 17),
    titleSmall = ModeText(true, 13, 14, 15),
    bodyLarge = ModeText(false, 16, 17, 18),
    bodyMedium = ModeText(false, 14, 15, 16),
    bodySmall = ModeText(false, 12, 13, 14),
    labelLarge = ModeText(true, 16, 17, 18),
    labelMedium = ModeText(true, 14, 15, 16),
    labelSmall = ModeText(true, 12, 13, 14),
)