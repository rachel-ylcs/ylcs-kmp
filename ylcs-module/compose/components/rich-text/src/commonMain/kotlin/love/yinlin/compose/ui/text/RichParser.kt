package love.yinlin.compose.ui.text

import androidx.compose.runtime.Stable
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJson

@Stable
class RichParser(converters: List<RichConverter>) {
    private val richConverters = mutableMapOf<String, RichConverter>().apply {
        put(RichType.Root.value, RichString.Converter)
        put(RichType.Text.value, RichNodeText.Converter)
        put(RichType.Emoji.value, RichNodeEmoji.Converter)
        put(RichType.Br.value, RichNodeBr.Converter)
        put(RichType.Image.value, RichNodeImage.Converter)
        put(RichType.Link.value, RichNodeLink.Converter)
        put(RichType.Topic.value, RichNodeTopic.Converter)
        put(RichType.At.value, RichNodeAt.Converter)
        put(RichType.Style.value, RichNodeStyle.Converter)
        for (converter in converters) put(converter.type, converter)
    }

    fun addConverter(converter: RichConverter) {
        richConverters[converter.type] = converter
    }

    fun parse(encodedText: String): RichString = catchingDefault({ RichString() }) {
        buildRichString {
            if (encodedText.startsWith('{') && encodedText.endsWith('}')) {
                val scope = RichParseScope(richConverters)
                scope.parseIn(this, encodedText.parseJson)
            }
            else text(encodedText)
        }
    }

    companion object {
        val Default = RichParser(emptyList())
    }
}