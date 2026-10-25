"""Reviewed source changes applied to the one pinned Compose working directory."""
import re


def replace_once(text, before, after):
    if text.count(before) != 1:
        raise ValueError(f"Compose upstream drift: expected one occurrence of {before!r}")
    return text.replace(before, after, 1)


def legacy_paragraph_bridges(text):
    """Retain old APIs and their locale snapshot, limiting deprecation suppression to those bridges."""
    pattern = re.compile(r"^actual fun Paragraph(?:Intrinsics)?\(\n(?:    .*\n)+\): Paragraph(?:Intrinsics)? =\n(?:[ \t].*\n|\n)+", re.MULTILINE)

    def adapt(match):
        declaration = match.group()
        start = text.rfind("\n\n", 0, match.start())
        annotations = text[start + 2 if start >= 0 else 0:match.start()]
        if "LocaleList.current" not in declaration or '@Suppress("DEPRECATION")' in annotations:
            return declaration
        return '@Suppress("DEPRECATION")\n' + declaration

    return pattern.sub(adapt, text)


def text_source_edits(workspace, native_root):
    base = "compose/ui/ui-text/src/"
    replacement = native_root / "adaptation/org.jetbrains.compose.ui_ui-text/src/nativeMain/kotlin/androidx/compose/ui/text/platform/Synchronization.native.kt"
    if not replacement.is_file():
        raise ValueError("Compose synchronization adaptation is missing; preserve the upstream implementation")
    # IDEA imports source directories without Gradle's individual file exclusions.
    # Remove the shadow in the prepared checkout so both tools see one actual.
    edits = {base + "nonAndroidMain/kotlin/androidx/compose/ui/text/platform/Synchronization.nonAndroid.kt": None}
    path = base + "commonMain/kotlin/androidx/compose/ui/text/AnnotatedString.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    for name in ["spanStyles", "paragraphStyles"]:
        text = replace_once(text, f"{name}!!.add(annotation", f"{name}.add(annotation")
    edits[path] = text
    path = base + "nonJvmMain/kotlin/androidx/compose/ui/text/AnnotatedString.nonJvm.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    text = replace_once(text, "    transitions.windowed(size = 2) { (start, end) ->\n", """    for (index in 0 ..< transitions.lastIndex) {
        val start = transitions[index]
        val end = transitions[index + 1]
""")
    edits[path] = text
    for name in ["Paragraph", "ParagraphIntrisics"]:
        path = base + f"nonAndroidMain/kotlin/androidx/compose/ui/text/{name}.nonAndroid.kt"
        original = (workspace / path).read_text(encoding="utf-8")
        text = legacy_paragraph_bridges(original)
        if text == original:
            raise ValueError(f"Compose upstream drift: no legacy paragraph bridges found in {path}")
        edits[path] = text
    path = base + "nonAndroidMain/kotlin/androidx/compose/ui/text/font/FontFamilyResolver.nonAndroid.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    for signature in ["fun createFontFamilyResolver()", "fun createFontFamilyResolver(coroutineContext: CoroutineContext)"]:
        text = replace_once(text, signature, "actual " + signature)
    edits[path] = text
    return edits


def skiko_source_edits(workspace, native_root):
    base = "compose/ui/ui-skiko/src/"
    adaptation = native_root / "adaptation/org.jetbrains.compose.ui_ui-skiko/src"
    for target in ["windows", "linux", "macos"]:
        for suffix in [f"graphics/NativeImageAsset.{target}.kt", f"text/platform/NativeFont.{target}.kt"]:
            if not (adaptation / f"{target}Main/kotlin/androidx/compose/ui" / suffix).is_file():
                raise ValueError(f"Compose Skiko adaptation is missing: {target}/{suffix}")
    edits = {base + "nativeMain/kotlin/androidx/compose/ui/graphics/NativeImageAsset.native.kt": None,
             base + "nativeMain/kotlin/androidx/compose/ui/text/platform/NativeFont.native.kt": None}
    path = base + "nonAndroidMain/kotlin/androidx/compose/ui/platform/ComposeUiSkikoRuntime.nonAndroid.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    for name in ["registerSkikoComposeImplementation", "clearSkikoComposeImplementation"]:
        text = replace_once(text, f"fun {name}()", f"actual fun {name}()")
    edits[path] = text
    path = base + "nonAndroidMain/kotlin/androidx/compose/ui/graphics/SkiaBackedCanvas.nonAndroid.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    edits[path] = replace_once(text, "fun SkCanvas.asComposeCanvas(): Canvas", "actual fun SkCanvas.asComposeCanvas(): Canvas")
    path = base + "nativeMain/kotlin/androidx/compose/ui/text/Cache.native.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    text = replace_once(text, "ref.value != null", "ref.get() != null")
    edits[path] = replace_once(text, "            if (other == null) return false\n            other as Key<*>", "            if (other !is Key<*>) return false")
    path = base + "nonAndroidMain/kotlin/androidx/compose/ui/text/SkikoCharHelpers.nonAndroid.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    for operation in ["preceding", "following"]:
        text = replace_once(text, f"    val iterator = BreakIterator.makeCharacterInstance()\n    iterator.setText(text)\n    return iterator.{operation}(index)",
                            f"    return BreakIterator.makeCharacterInstance().use {{ iterator ->\n        iterator.setText(text)\n        iterator.{operation}(index)\n    }}")
    text = text.replace("import org.jetbrains.skia.BreakIterator\n", "import org.jetbrains.skia.BreakIterator\nimport org.jetbrains.skia.impl.use\n")
    edits[path] = text
    path = base + "nonAndroidMain/kotlin/androidx/compose/ui/text/platform/SkikoParagraphIntrinsics.nonAndroid.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    edits[path] = replace_once(text, "    private var resolvedTypefaces: TypefaceDirtyTrackerLinkedList? = null",
                               "    internal val boundaryLocale: String? get() = (style.localeList ?: defaultLocaleList).firstOrNull()?.toLanguageTag()\n\n    private var resolvedTypefaces: TypefaceDirtyTrackerLinkedList? = null")
    path = base + "nonAndroidMain/kotlin/androidx/compose/ui/text/SkikoParagraph.nonAndroid.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    text = replace_once(text, "        // TODO(https://youtrack.jetbrains.com/issue/CMP-1255)\n        return TextRange.Zero",
                        "        return nativeRangeForRect(paragraph, text, paragraphIntrinsics.boundaryLocale, rect, granularity, inclusionStrategy)")
    text = replace_once(text, '''        println("Compose Multiplatform doesn't support fillBoundingBoxes` yet. " +
            "Follow https://youtrack.jetbrains.com/issue/CMP-720")
        // TODO(https://youtrack.jetbrains.com/issue/CMP-720) implement fillBoundingBoxes''',
                        "        nativeFillBoundingBoxes(this, paragraph, text, paragraphIntrinsics.boundaryLocale, range, array, arrayStart)")
    edits[path] = text
    path = base + "nonAndroidMain/kotlin/androidx/compose/ui/graphics/SkikoGraphics.nonAndroid.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    edits[path] = replace_once(text, "    // TODO: https://youtrack.jetbrains.com/issue/CMP-739\n    override fun colorMatrixFromFilter(filter: PlatformColorFilter): ColorMatrix = ColorMatrix()",
                               "    override fun colorMatrixFromFilter(filter: PlatformColorFilter): ColorMatrix = colorMatrixFromSkiko(filter)")
    return edits


def ui_source_edits(workspace, native_root):
    base = "compose/ui/ui/src/"
    replacement = native_root / "adaptation/org.jetbrains.compose.ui_ui/src/nativeMain/kotlin/androidx/compose/ui/platform/Synchronization.native.kt"
    if not replacement.is_file():
        raise ValueError("Compose UI synchronization adaptation is missing; preserve the upstream implementation")
    edits = {base + "skikoMain/kotlin/androidx/compose/ui/platform/Synchronization.skiko.kt": None}
    path = base + "skikoMain/kotlin/androidx/compose/ui/platform/PlatformClipboard.skiko.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    edits[path] = replace_once(text, "// TODO: https://youtrack.jetbrains.com/issue/CMP-1260\nactual class ClipMetadata private constructor()\n", "")
    path = base + "nonJvmMain/kotlin/androidx/compose/ui/Actuals.nonJvm.kt"
    text = (workspace / path).read_text(encoding="utf-8")
    text = replace_once(text, "import kotlinx.coroutines.Dispatchers", "import androidx.compose.ui.platform.nativeUiDispatcher")
    edits[path] = replace_once(text, "get() = Dispatchers.Main", "get() = nativeUiDispatcher().context")
    return edits


def foundation_source_edits(workspace, native_root):
    base = "compose/foundation/foundation/src/"
    adaptation = native_root / "adaptation/org.jetbrains.compose.foundation_foundation/src/nativeMain/kotlin/androidx/compose/foundation"
    shadows = {
        "skikoMain/kotlin/androidx/compose/foundation/platform/Synchronization.skiko.kt": "platform/Synchronization.native.kt",
        "nativeMain/kotlin/androidx/compose/foundation/text/KeyEventHelpers.native.kt": "text/KeyEventHelpers.native.kt",
        "nativeMain/kotlin/androidx/compose/foundation/text/contextmenu/internal/ProvideDefaultPlatformTextContextMenuProviders.native.kt": "text/contextmenu/internal/PlatformDefaultTextContextMenuProviders.native.kt",
        "nativeMain/kotlin/androidx/compose/foundation/text/selection/TextFieldSelectionManager.native.kt": "text/selection/TextFieldSelectionManager.native.kt",
        "skikoMain/kotlin/androidx/compose/foundation/content/TransferableContent.skiko.kt": "content/TransferableContent.native.kt",
        "skikoMain/kotlin/androidx/compose/foundation/content/MediaType.skiko.kt": "content/MediaType.native.kt",
        "skikoMain/kotlin/androidx/compose/foundation/content/internal/ReceiveContentDragAndDropNode.skiko.kt": "content/internal/ReceiveContentDragAndDropNode.native.kt",
    }
    edits = {}
    for source, replacement in shadows.items():
        if not (adaptation / replacement).is_file():
            raise ValueError(f"Foundation adaptation is missing: {replacement}")
        edits[base + source] = None
    replacements = {
        "commonMain/kotlin/androidx/compose/foundation/lazy/staggeredgrid/LazyStaggeredGridCacheWindowLogic.kt":
            ("override fun getNextStartItemIndexInLane(lane: Int, currentItemIndex: Int) =", "override fun getNextStartItemIndexInLane(lane: Int, currentItemIndex: Int): Int ="),
        "commonMain/kotlin/androidx/compose/foundation/pager/PagerState.kt":
            ("private fun tryRunPrefetch(result: PagerMeasureResult) =", "private fun tryRunPrefetch(result: PagerMeasureResult): Unit ="),
        "skikoMain/kotlin/androidx/compose/foundation/text/ContextMenuStrings.skiko.kt":
            ("private fun getTranslation(string: ContextMenuStrings, locale: Locale): String", "internal fun getTranslation(string: ContextMenuStrings, locale: Locale): String"),
    }
    for relative, [before, after] in replacements.items():
        path = base + relative
        edits[path] = replace_once((workspace / path).read_text(encoding="utf-8"), before, after)
    return edits


def source_edits(workspace, native_root, modules):
    edits = {}
    if "ui-text" in modules:
        edits.update(text_source_edits(workspace, native_root))
    if "ui-skiko" in modules:
        edits.update(skiko_source_edits(workspace, native_root))
    if "ui" in modules:
        edits.update(ui_source_edits(workspace, native_root))
    if "foundation" in modules:
        edits.update(foundation_source_edits(workspace, native_root))
    return edits
