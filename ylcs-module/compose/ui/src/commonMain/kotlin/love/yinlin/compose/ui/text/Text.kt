package love.yinlin.compose.ui.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.collection.StableMap
import love.yinlin.compose.collection.emptyStableMap

@Stable
data class AutoSize(private val min: TextUnit, private val max: TextUnit, private val step: TextUnit) {
    internal val delegate: TextAutoSize = TextAutoSize.StepBased(min, max, step)
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: AutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalStyle.current
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            color = color.takeOrElse { style.color.takeOrElse { LocalColor.current } },
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
        ),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        autoSize = autoSize?.delegate,
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: AutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: StableMap<String, InlineTextContent> = emptyStableMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalStyle.current,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            color = color.takeOrElse { style.color.takeOrElse { LocalColor.current } },
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
        ),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        autoSize = autoSize?.delegate,
    )
}

@Composable
fun SimpleClipText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalStyle.current
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            color = color.takeOrElse { style.color.takeOrElse { LocalColor.current } },
            fontSize = TextUnit.Unspecified,
            fontWeight = null,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = TextUnit.Unspecified,
            fontFamily = null,
            textDecoration = textDecoration,
            fontStyle = null,
            letterSpacing = TextUnit.Unspecified,
        ),
        onTextLayout = null,
        overflow = TextOverflow.Clip,
        softWrap = true,
        maxLines = 1,
        minLines = 1,
        autoSize = null,
    )
}

@Composable
fun SimpleEllipsisText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalStyle.current
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            color = color.takeOrElse { style.color.takeOrElse { LocalColor.current } },
            fontSize = TextUnit.Unspecified,
            fontWeight = null,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = TextUnit.Unspecified,
            fontFamily = null,
            textDecoration = textDecoration,
            fontStyle = null,
            letterSpacing = TextUnit.Unspecified,
        ),
        onTextLayout = null,
        overflow = TextOverflow.Ellipsis,
        softWrap = true,
        maxLines = 1,
        minLines = 1,
        autoSize = null,
    )
}