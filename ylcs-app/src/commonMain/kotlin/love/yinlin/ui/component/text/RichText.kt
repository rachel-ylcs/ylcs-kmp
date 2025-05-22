package love.yinlin.ui.component.text

import KottieAnimation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.github.panpf.sketch.fetch.newComposeResourceUri
import kotlinx.serialization.json.*
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition
import love.yinlin.common.Colors
import love.yinlin.common.EmojiManager
import love.yinlin.extension.*
import love.yinlin.platform.ImageQuality
import love.yinlin.resources.Res
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.image.WebImage
import org.jetbrains.compose.resources.painterResource
import utils.KottieConstants

// RichString DSL

internal const val RICH_ARG_TYPE = "t"
internal const val RICH_ARG_MEMBER = "m"
internal const val RICH_ARG_URI = "u"
internal const val RICH_ARG_TEXT = "tx"
internal const val RICH_ARG_WIDTH = "w"
internal const val RICH_ARG_HEIGHT = "h"
internal const val RICH_ARG_TEXT_SIZE = "s"
internal const val RICH_ARG_COLOR = "c"
internal const val RICH_ARG_BOLD = "b"
internal const val RICH_ARG_ITALIC = "i"
internal const val RICH_ARG_UNDERLINE = "u"
internal const val RICH_ARG_STRIKETHROUGH = "d"
internal const val RICH_TYPE_ROOT = "r"
internal const val RICH_TYPE_EMOJI = "em"
internal const val RICH_TYPE_IMAGE = "img"
internal const val RICH_TYPE_LINK = "lk"
internal const val RICH_TYPE_TOPIC = "tp"
internal const val RICH_TYPE_AT = "at"
internal const val RICH_TYPE_STYLE = "s"

@Stable
interface RichDrawable {
	val width: Float
	val height: Float
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
	protected open val map: JsonObject = makeObject { RICH_ARG_TYPE with type }

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
		override val json: JsonElement = text.json

		override fun build(context: RichContext) {
			context.builder.append(text)
		}
	}
	fun text(str: String) = makeItem(Text(str))

	@Stable
	protected class Emoji(private val id: Int) : RichItem, RichDrawable {
		override val width: Float get() = when (EmojiManager[id]) {
			null, is love.yinlin.common.Emoji.Lottie -> 1f
			else -> 3f
		}

		override val height: Float get() = when (EmojiManager[id]) {
			null, is love.yinlin.common.Emoji.Lottie -> 1f
			else -> 3f
		}

		override val json: JsonElement = id.json

		override fun build(context: RichContext) {
			val id = context.id
			context.builder.appendInlineContent(id, "\uFFFD")
			context.content[id] = this
		}

		@Composable
		override fun draw() {
			when (val emoji = EmojiManager[id]) {
				null -> Box(modifier = Modifier.fillMaxSize())
				is love.yinlin.common.Emoji.Static -> {
					MiniImage(
						painter = painterResource(emoji.res),
						modifier = Modifier.fillMaxSize()
					)
				}
				is love.yinlin.common.Emoji.Dynamic -> {
					WebImage(
						uri = newComposeResourceUri(Res.getUri("drawable/emoji${emoji.id}.webp")),
						quality = ImageQuality.Low,
						placeholder = null,
						modifier = Modifier.fillMaxSize()
					)
				}
				is love.yinlin.common.Emoji.Lottie -> {
					val isForeground = rememberOffScreenState()
					val composition = rememberKottieComposition(spec = KottieCompositionSpec.JsonString(emoji.data))
					val animationState by animateKottieCompositionAsState(
						composition = composition,
						isPlaying = isForeground,
						iterations = KottieConstants.IterateForever
					)
					KottieAnimation(
						composition = composition,
						progress = { animationState.progress },
						modifier = Modifier.fillMaxSize()
					)
				}
			}
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
	protected class Image(
        private val uri: String,
        override val width: Float,
        override val height: Float
	) : RichObject(RICH_TYPE_IMAGE), RichDrawable {
		override val map: JsonObject = makeObject {
			merge(super.map)
			RICH_ARG_URI with uri
			RICH_ARG_WIDTH with width
			RICH_ARG_HEIGHT with height
		}

		override fun build(context: RichContext) {
			val id = context.id
			context.builder.appendInlineContent(id, "\uFFFD")
            context.content[id] = this
        }

		@Composable
		override fun draw() {
			Box(modifier = Modifier.fillMaxSize().padding(horizontal = 0.5.dp * width)) {
				WebImage(
					uri = uri,
					quality = ImageQuality.Low,
					modifier = Modifier.fillMaxSize()
				)
			}
		}
	}
	fun image(uri: String, width: Float = 1f, height: Float = 1f) = makeItem(Image(uri, width, height))

	@Stable
	protected class Link(private val uri: String, private val text: String) : RichObject(RICH_TYPE_LINK) {
		override val map: JsonObject = makeObject {
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

	protected class Topic(private val uri: String, private val text: String) : RichObject(RICH_TYPE_TOPIC) {
		override val map: JsonObject = makeObject {
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
		override val map: JsonObject = makeObject {
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
						RICH_TYPE_IMAGE -> container.image(
							uri = obj[RICH_ARG_URI].String,
							width = obj[RICH_ARG_WIDTH].FloatNull ?: 1f,
							height = obj[RICH_ARG_HEIGHT].FloatNull ?: 1f
						)
						RICH_TYPE_LINK -> container.link(obj[RICH_ARG_URI].String, obj[RICH_ARG_TEXT].String)
						RICH_TYPE_TOPIC -> container.topic(obj[RICH_ARG_URI].String, obj[RICH_ARG_TEXT].String)
						RICH_TYPE_AT -> container.at(obj[RICH_ARG_URI].String, obj[RICH_ARG_TEXT].String)
						RICH_TYPE_STYLE -> {
							container.style(
								textSize = obj[RICH_ARG_TEXT_SIZE]?.Int?.sp,
								color = obj[RICH_ARG_COLOR]?.Int?.let { Colors.from(it) },
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
				else -> {}
			}
		}

		fun parse(data: String): RichString = try {
			buildRichString {
				if (data.startsWith('{') && data.endsWith('}')) {
					parseElement(data.parseJson, this)
				}
				else text(data)
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
private fun SelectableContainer(
	enabled: Boolean,
	content: @Composable () -> Unit
) {
	if (enabled) SelectionContainer(content = content)
	else content()
}

@Composable
fun RichText(
	text: RichString,
	modifier: Modifier = Modifier,
	onLinkClick: ((String) -> Unit)? = null,
	onTopicClick: ((String) -> Unit)? = null,
	onAtClick: ((String) -> Unit)? = null,
	style: TextStyle = LocalTextStyle.current,
	color: Color = Colors.Unspecified,
	overflow: TextOverflow = TextOverflow.Clip,
	maxLines: Int = Int.MAX_VALUE,
	canSelected: Boolean = true,
	fixLineHeight: Boolean = false,
) {
	val state = remember(text) { text.asState(
		onLinkClick = onLinkClick,
		onTopicClick = onTopicClick,
		onAtClick = onAtClick
	) }
	val fontSize = LocalTextStyle.current.fontSize
	val inlineTextContent = remember(state) {
		state.content.mapValues { (_, drawable) ->
			InlineTextContent(
				placeholder = Placeholder(
					width = fontSize * drawable.width,
					height = fontSize * drawable.height,
					placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
				)
			) {
				drawable.draw()
			}
		}
	}

	SelectableContainer(canSelected) {
		Text(
			text = state.text,
			color = color,
			style = if (fixLineHeight) style.copy(lineHeight = TextUnit.Unspecified) else style, // what the fuck bug
			modifier = modifier,
			overflow = overflow,
			maxLines = maxLines,
			inlineContent = inlineTextContent
		)
	}
}