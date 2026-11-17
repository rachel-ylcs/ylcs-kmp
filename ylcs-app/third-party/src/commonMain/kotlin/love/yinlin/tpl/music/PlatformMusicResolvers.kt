package love.yinlin.tpl.music

import love.yinlin.data.music.PlatformMusicType
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri

object PlatformMusicResolvers {
    val all: List<PlatformMusicAPI> = [QQMusicAPI, NetEaseCloudAPI, KugouMusicAPI, SodaMusicAPI, MiguMusicAPI]

    private val shareLink = Regex("""https?://[^\s<>"“”。，、；！？）】]+""", RegexOption.IGNORE_CASE)

    operator fun get(type: PlatformMusicType): PlatformMusicAPI = all.first { it.type == type }

    fun links(input: String): Sequence<Uri> = shareLink.findAll(input).mapNotNull { Uri.parse(it.value) }

    fun identify(input: String): PlatformMusicAPI? {
        val links = links(input).toList()
        return all.firstOrNull { resolver -> links.any(resolver::matchesLink) || resolver.matchesTrackId(input.trim()) }
    }
}

internal fun Uri.isMusicLinkOn(domain: String): Boolean {
    if (scheme != Scheme.Http && scheme != Scheme.Https) return false
    val hostname = host?.lowercase() ?: return false
    return hostname == domain || hostname.endsWith(".$domain")
}

internal fun Uri.parameterId(name: String): String? = params[name]?.substringBefore('#')
internal fun Uri.lastPathSegment(): String? = path?.trimEnd('/')?.substringAfterLast('/')?.takeIf(String::isNotEmpty)
internal val numericTrackId = Regex("[0-9]{1,19}")
