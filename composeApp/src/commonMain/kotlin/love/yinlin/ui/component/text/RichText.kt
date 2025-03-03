package love.yinlin.ui.component.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.*
import love.yinlin.Colors
import love.yinlin.extension.Array
import love.yinlin.extension.Boolean
import love.yinlin.extension.Int
import love.yinlin.extension.String
import love.yinlin.extension.json
import love.yinlin.extension.makeObject
import love.yinlin.extension.parseJson
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.toJsonString
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.image.WebImageQuality

// RichString DSL

private const val RICH_ARG_TYPE = "t"
private const val RICH_ARG_MEMBER = "m"
private const val RICH_ARG_URI = "uri"
private const val RICH_ARG_TEXT = "text"
private const val RICH_ARG_TEXT_SIZE = "s"
private const val RICH_ARG_COLOR = "c"
private const val RICH_ARG_BOLD = "b"
private const val RICH_ARG_ITALIC = "i"
private const val RICH_ARG_UNDERLINE = "u"
private const val RICH_ARG_STRIKETHROUGH = "d"
private const val RICH_TYPE_ROOT = "r"
private const val RICH_TYPE_IMAGE = "img"
private const val RICH_TYPE_LINK = "lk"
private const val RICH_TYPE_TOPIC = "tp"
private const val RICH_TYPE_AT = "at"
private const val RICH_TYPE_STYLE = "s"

@Stable
interface RichDrawable {
	@Composable fun draw()
}

class RichContext(
	val onLinkClick: ((String) -> Unit)?,
	val onTopicClick: ((String) -> Unit)?,
	val onAtClick: ((String) -> Unit)?
) {
	var idValue: Int = 0
	val id: String get() {
		val str = idValue.toString()
		idValue++
		return str
	}
	val builder = AnnotatedString.Builder()
	val content = mutableMapOf<String, RichDrawable>()
}

@Stable
data class RichState(
	val text: AnnotatedString,
	val content: Map<String, RichDrawable>
)

@Stable
interface RichItem {
	val json: JsonElement
	fun build(context: RichContext)
}

@Stable
abstract class RichObject(protected val type: String) : RichItem {
	protected open val map: JsonObject get() = makeObject { RICH_ARG_TYPE with type }

	override val json: JsonElement get() = map
}

@Stable
abstract class RichContainer(type: String) : RichObject(type) {
	protected val items = mutableListOf<RichItem>()

	override val map: JsonObject get() = makeObject {
		merge(super.map)
		arr(RICH_ARG_MEMBER) {
			for (item in items) add(item.json)
		}
	}

	override fun build(context: RichContext) {
		for (item in items) item.build(context)
	}

	override fun toString(): String = json.toJsonString()

	protected fun makeItem(item: RichItem) {
		items += item
	}

	protected fun makeContainer(item: RichContainer, content: RichContainer.() -> Unit) {
		item.content()
		items += item
	}

	@Stable
	protected class Text(private val text: String) : RichItem {
		override val json: JsonElement get() = text.json

		override fun build(context: RichContext) {
			context.builder.append(text)
		}
	}
	fun text(str: String) = makeItem(Text(str))

	@Stable
	protected class Emoji(private val id: Int) : RichItem {
		override val json: JsonElement get() = id.json

		override fun build(context: RichContext) {
			TODO()
		}
	}
	fun emoji(id: Int) = makeItem(Emoji(id))

	@Stable
	protected class Br : RichItem {
		override val json: JsonElement = JsonNull

		override fun build(context: RichContext) {
			context.builder.appendLine()
		}
	}
	fun br() = makeItem(Br())

	@Stable
	protected class Image(private val uri: String) : RichObject(RICH_TYPE_IMAGE), RichDrawable {
		override val map: JsonObject get() = makeObject {
			merge(super.map)
			RICH_ARG_URI with uri
		}

		override fun build(context: RichContext) {
			val id = context.id
			context.builder.appendInlineContent(id, " ")
			context.content.put(id, this)
		}

		@Composable
		override fun draw() {
			WebImage(
				uri = uri,
				quality = WebImageQuality.Low,
				modifier = Modifier.fillMaxSize()
			)
		}
	}
	fun image(uri: String) = makeItem(Image(uri))

	@Stable
	protected class Link(private val uri: String, private val text: String) : RichObject(RICH_TYPE_LINK) {
		override val map: JsonObject get() = makeObject {
			merge(super.map)
			RICH_ARG_URI with uri
			RICH_ARG_TEXT with text
		}

		override fun build(context: RichContext) {
			context.builder.append(' ')
			context.builder.withLink(link = LinkAnnotation.Url(
				url = uri,
				styles = TextLinkStyles(
					style = SpanStyle(color = Colors.Yellow6)
				),
				linkInteractionListener = {
					context.onLinkClick?.invoke(uri)
				}
			)) {
				append(text)
			}
			context.builder.append(' ')
		}
	}
	fun link(uri: String, text: String) = makeItem(Link(uri, text))

	@Stable
	protected class Topic(private val uri: String, private val text: String) : RichObject(RICH_TYPE_TOPIC) {
		override val map: JsonObject get() = makeObject {
			merge(super.map)
			RICH_ARG_URI with uri
			RICH_ARG_TEXT with text
		}

		override fun build(context: RichContext) {
			context.builder.append(' ')
			context.builder.withLink(link = LinkAnnotation.Url(
				url = uri,
				styles = TextLinkStyles(
					style = SpanStyle(color = Colors.Cyan4)
				),
				linkInteractionListener = {
					context.onTopicClick?.invoke(uri)
				}
			)) {
				append(text)
			}
			context.builder.append(' ')
		}
	}
	fun topic(uri: String, text: String) = makeItem(Topic(uri, text))

	@Stable
	protected class At(private val uri: String, private val text: String) : RichObject(RICH_TYPE_AT)  {
		override val map: JsonObject get() = makeObject {
			merge(super.map)
			RICH_ARG_URI with uri
			RICH_ARG_TEXT with text
		}

		override fun build(context: RichContext) {
			context.builder.append(' ')
			context.builder.withLink(link = LinkAnnotation.Url(
				url = uri,
				styles = TextLinkStyles(
					style = SpanStyle(color = Colors.Red4)
				),
				linkInteractionListener = {
					context.onAtClick?.invoke(uri)
				}
			)) {
				append(text)
			}
			context.builder.append(' ')
		}
	}
	fun at(uri: String, text: String) = makeItem(At(uri, text))

	@Stable
	protected class Style(
		private val textSize: TextUnit?,
		private val color: Color?,
		private val bold: Boolean,
		private val italic: Boolean,
		private val underline: Boolean,
		private val strikethrough: Boolean
	) : RichContainer(RICH_TYPE_STYLE) {
		override val map: JsonObject get() = makeObject {
			merge(super.map)
			if (textSize != null) RICH_ARG_TEXT_SIZE with textSize.value
			if (color != null) RICH_ARG_COLOR with color.toArgb()
			if (bold) RICH_ARG_BOLD with bold
			if (italic) RICH_ARG_ITALIC with italic
			if (underline) RICH_ARG_UNDERLINE with underline
			if (strikethrough) RICH_ARG_STRIKETHROUGH with strikethrough
		}

		override fun build(context: RichContext) {
			val textDecorations = mutableListOf<TextDecoration>()
			if (underline) textDecorations += TextDecoration.Underline
			if (strikethrough) textDecorations += TextDecoration.LineThrough
			context.builder.withStyle(SpanStyle(
				color = color ?: Colors.Unspecified,
				fontSize = textSize ?: TextUnit.Unspecified,
				fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Light,
				fontStyle = if (italic) FontStyle.Italic else null,
				textDecoration = if (textDecorations.isEmpty()) TextDecoration.None else TextDecoration.combine(textDecorations)
			)) {
				super.build(context)
			}
		}
	}
	fun style(
		textSize: TextUnit? = null,
		color: Color? = null,
		bold: Boolean = false,
		italic: Boolean = false,
		underline: Boolean = false,
		strikethrough: Boolean = false,
		content: RichContainer.() -> Unit
	) = makeContainer(Style(
		textSize = textSize,
		color = color,
		bold = bold,
		italic = italic,
		underline = underline,
		strikethrough = strikethrough,
	), content)
}

@Stable
class RichString : RichContainer(RICH_TYPE_ROOT) {
	fun asState(
		onLinkClick: ((String) -> Unit)?,
		onTopicClick: ((String) -> Unit)?,
		onAtClick: ((String) -> Unit)?
	): RichState {
		val context = RichContext(
			onLinkClick = onLinkClick,
			onTopicClick = onTopicClick,
			onAtClick = onAtClick
		)
		build(context)
		return RichState(context.builder.toAnnotatedString(), context.content)
	}

	companion object {
		private fun parseElement(obj: JsonElement, container: RichContainer) {
			when (obj) {
				is JsonNull -> container.br()
				is JsonPrimitive -> {
					if (obj.isString) container.text(obj.content)
					else container.emoji(obj.int)
				}
				is JsonObject -> {
					when (obj[RICH_ARG_TYPE].String) {
						RICH_TYPE_IMAGE -> container.image(obj[RICH_ARG_URI].String)
						RICH_TYPE_LINK -> container.link(obj[RICH_ARG_URI].String, obj[RICH_ARG_TEXT].String)
						RICH_TYPE_TOPIC -> container.topic(obj[RICH_ARG_URI].String, obj[RICH_ARG_TEXT].String)
						RICH_TYPE_AT -> container.at(obj[RICH_ARG_URI].String, obj[RICH_ARG_TEXT].String)
						RICH_TYPE_STYLE -> {
							container.style(
								textSize = obj[RICH_ARG_TEXT_SIZE]?.Int?.sp,
								color = obj[RICH_ARG_COLOR]?.Int?.let { Color(it) },
								bold = obj[RICH_ARG_BOLD]?.Boolean == true,
								italic = obj[RICH_ARG_ITALIC]?.Boolean == true,
								underline = obj[RICH_ARG_UNDERLINE]?.Boolean == true,
								strikethrough = obj[RICH_ARG_STRIKETHROUGH]?.Boolean == true
							) {
								for (item in obj[RICH_ARG_MEMBER].Array) parseElement(item, this)
							}
						}
						RICH_TYPE_ROOT -> {
							for (item in obj[RICH_ARG_MEMBER].Array) parseElement(item, container)
						}
					}
				}
				else -> error("")
			}
		}

		fun parse(data: String): RichString = try {
			buildRichString {
				parseElement(data.parseJson, this)
			}
		}
		catch (_: Throwable) {
			RichString()
		}
	}
}

inline fun buildRichString(content: RichContainer.() -> Unit): RichString {
	val richString = RichString()
	richString.content()
	return richString
}

@Composable
fun RichText(
	text: RichString,
	modifier: Modifier = Modifier,
	onLinkClick: ((String) -> Unit)? = null,
	onTopicClick: ((String) -> Unit)? = null,
	onAtClick: ((String) -> Unit)? = null,
	overflow: TextOverflow = TextOverflow.Clip,
	maxLines: Int = Int.MAX_VALUE,
) {
	val state by rememberDerivedState { text.asState(
		onLinkClick = onLinkClick,
		onTopicClick = onTopicClick,
		onAtClick = onAtClick
	) }
	val fontSize = LocalTextStyle.current.fontSize
	val inlineTextContent by rememberDerivedState {
		state.content.mapValues { drawable ->
			InlineTextContent(
				placeholder = Placeholder(
					width = fontSize * 1.25f,
					height = fontSize,
					placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
				)
			) {
				drawable.value.draw()
			}
		}
	}

	Text(
		text = state.text,
		modifier = modifier,
		overflow = overflow,
		maxLines = maxLines,
		inlineContent = inlineTextContent
	)
}