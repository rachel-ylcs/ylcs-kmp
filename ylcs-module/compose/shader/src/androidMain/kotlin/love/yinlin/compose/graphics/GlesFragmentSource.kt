package love.yinlin.compose.graphics

private val sourceToken = Regex("""//[^\r\n]*|/\*[\s\S]*?\*/|[A-Za-z_][A-Za-z_0-9]*""")
private val vectorType = Regex("""(?:half|float)([2-4])""")
private val matrixType = Regex("""(?:half|float)([2-4])x\1""")
private val unsupportedTokens: Set<String> = ["shader", "layout", "toLinearSrgb", "fromLinearSrgb", "sk_FragCoord"]

internal fun toGlesFragmentSource(source: String): String {
    val translated = sourceToken.replace(source) { match ->
        val token = match.value
        when {
            token.startsWith("/") -> token
            token.startsWith("rachelShader") -> error("The rachelShader prefix is reserved in shader source.")
            token == "main" -> "rachelShaderMain"
            token == "half" -> "float"
            token in unsupportedTokens -> error("'$token' is unsupported by the Android 9–12 shader backend.")
            else -> vectorType.matchEntire(token)?.let { "vec${it.groupValues[1]}" } ?: matrixType.matchEntire(token)?.let { "mat${it.groupValues[1]}" } ?: token
        }
    }

    return """
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        precision highp float;
        #else
        precision mediump float;
        #endif
        uniform vec2 rachelShaderPixelScale;
        #line 1
        $translated
        void main() {
            gl_FragColor = rachelShaderMain(gl_FragCoord.xy * rachelShaderPixelScale);
        }
    """.trimIndent()
}
