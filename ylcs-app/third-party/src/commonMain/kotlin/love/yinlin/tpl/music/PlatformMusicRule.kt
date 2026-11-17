package love.yinlin.tpl.music

import androidx.compose.runtime.Stable
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri

@Stable
class PlatformMusicRule private constructor(
    val description: String,
    val kind: Kind,
    private val extract: (Input) -> String?,
) {
    enum class Kind { Track, Playlist, Album, Redirect }

    @Stable
    data class Match(val rule: PlatformMusicRule, val value: String)

    internal fun match(input: Input): Match? = extract(input)?.let { Match(this, it) }

    internal class Input(val text: String, val links: List<Uri>) {
        constructor(text: String) : this(text.trim(), shareLink.findAll(text).mapNotNull { Uri.parse(it.value) }.toList())
        constructor(uri: Uri) : this(uri.toString(), [uri])
    }

    companion object {
        private val shareLink = Regex("""https?://[^\s<>"“”。，、；！？）】]+""", RegexOption.IGNORE_CASE)

        internal fun isMusicLinkOn(uri: Uri, domain: String): Boolean {
            val hostname = uri.host?.lowercase() ?: return false
            return (uri.scheme == Scheme.Http || uri.scheme == Scheme.Https) && (hostname == domain || hostname.endsWith(".$domain"))
        }

        internal fun link(description: String, kind: Kind, vararg domains: String, extract: (Uri) -> String?) = PlatformMusicRule(description, kind) { input ->
            input.links.firstNotNullOfOrNull { uri -> if (domains.any { isMusicLinkOn(uri, it) }) extract(uri) else null }
        }

        internal fun trackId(description: String, pattern: Regex) = PlatformMusicRule(description, Kind.Track) { input ->
            input.text.takeIf(pattern::matches)
        }

        internal fun redirect(description: String, domain: String, matches: (Uri) -> Boolean = { true }) = link(description, Kind.Redirect, domain) { uri -> uri.takeIf(matches)?.toString() }
    }
}
