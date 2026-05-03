package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

@Stable
interface CharacterMetadata {
    val description: String

    @Stable
    @JvmInline
    value class MInt(val value: Int) : CharacterMetadata {
        override val description: String get() = value.toString()
    }

    @Stable
    @JvmInline
    value class MFloat(val value: Float) : CharacterMetadata {
        override val description: String get() = value.toString()
    }

    @Stable
    @JvmInline
    value class MString(override val description: String) : CharacterMetadata

    @Stable
    @JvmInline
    value class MPercent(val value: Float) : CharacterMetadata {
        override val description: String get() = "${(value * 100).roundToInt()}%"
    }
}