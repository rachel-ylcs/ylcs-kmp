package love.yinlin.extension

import kotlinx.browser.document
import org.w3c.dom.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

@PublishedApi
internal val DomElementMap: Map<KClass<out HTMLElement>, String> by lazy { mapOf(
    HTMLHtmlElement::class to "html",
    HTMLHeadElement::class to "head",
    HTMLTitleElement::class to "title",
    HTMLBaseElement::class to "base",
    HTMLLinkElement::class to "link",
    HTMLMetaElement::class to "meta",
    HTMLStyleElement::class to "style",
    HTMLBodyElement::class to "body",
    HTMLParagraphElement::class to "p",
    HTMLHRElement::class to "hr",
    HTMLPreElement::class to "pre",
    HTMLOListElement::class to "ol",
    HTMLUListElement::class to "ul",
    HTMLLIElement::class to "li",
    HTMLDListElement::class to "dl",
    HTMLDivElement::class to "div",
    HTMLAnchorElement::class to "a",
    HTMLDataElement::class to "data",
    HTMLTimeElement::class to "time",
    HTMLSpanElement::class to "span",
    HTMLBRElement::class to "br",
    HTMLPictureElement::class to "picture",
    HTMLSourceElement::class to "source",
    HTMLImageElement::class to "img",
    HTMLIFrameElement::class to "iframe",
    HTMLEmbedElement::class to "embed",
    HTMLObjectElement::class to "object",
    HTMLParamElement::class to "param",
    HTMLVideoElement::class to "video",
    HTMLAudioElement::class to "audio",
    HTMLTrackElement::class to "track",
    HTMLMediaElement::class to "media",
    HTMLMapElement::class to "map",
    HTMLAreaElement::class to "area",
    HTMLTableElement::class to "table",
    HTMLTableCaptionElement::class to "caption",
    HTMLTableColElement::class to "col",
    HTMLTableRowElement::class to "row",
    HTMLFormElement::class to "form",
    HTMLLabelElement::class to "label",
    HTMLInputElement::class to "input",
    HTMLButtonElement::class to "button",
    HTMLSelectElement::class to "select",
    HTMLDataListElement::class to "datalist",
    HTMLOptGroupElement::class to "optgroup",
    HTMLOptionElement::class to "option",
    HTMLTextAreaElement::class to "textarea",
    HTMLOutputElement::class to "output",
    HTMLProgressElement::class to "progress",
    HTMLMeterElement::class to "meter",
    HTMLFieldSetElement::class to "fieldset",
    HTMLLegendElement::class to "legend",
    HTMLDetailsElement::class to "details",
    HTMLMenuElement::class to "menu",
    HTMLDialogElement::class to "dialog",
    HTMLScriptElement::class to "script",
    HTMLTemplateElement::class to "template",
    HTMLSlotElement::class to "slot",
    HTMLCanvasElement::class to "canvas",
    HTMLMarqueeElement::class to "marquee",
    HTMLFrameSetElement::class to "frameset",
    HTMLFontElement::class to "font",
) }

@OptIn(ExperimentalContracts::class)
inline fun <reified T : HTMLElement> createElement(block: T.() -> Unit = {}): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val element = document.createElement(DomElementMap[T::class] ?: "") as T
    element.block()
    return element
}

inline fun <reified T : HTMLElement> getElementById(id: String): T? = document.getElementById(id) as? T