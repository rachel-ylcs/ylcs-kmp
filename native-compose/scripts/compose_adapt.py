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


def source_edits(workspace, native_root, modules):
    if "ui-text" not in modules:
        return {}
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
    return edits
