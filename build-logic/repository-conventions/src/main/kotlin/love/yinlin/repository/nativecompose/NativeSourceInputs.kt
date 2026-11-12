package love.yinlin.repository.nativecompose

import groovy.json.JsonSlurper
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Duration

/** The reference records are local download receipts, never a second source of versions. */
internal class NativeSourceInputs(private val vendor: File, private val offline: Boolean, private val staging: () -> File) {
    private val client by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build()
    }

    private val releaseDigests = mutableMapOf<String, Map<String, String>>()

    class Archive(val metadata: SourceObject, resolve: () -> File) {
        val file by lazy(resolve)
    }

    class GitTree(val metadata: SourceObject, resolve: () -> GitSnapshot) {
        val snapshot by lazy(resolve)
    }

    data class GitSnapshot(val directory: File, val files: Set<String>)

    private fun receipt(name: String, version: String, key: String): File =
        checkedSourcePath(vendor, vendor.resolve("metadata/${sourceRelativePath(name)}/${sourceRelativePath(version)}/$key.json"))

    fun archive(name: String, version: String, url: String, maximum: Long, existing: File? = null): Archive {
        val uri = URI(url)
        check(uri.scheme == "https" && uri.host in setOf("codeload.github.com", "github.com", "repo.maven.apache.org")) { "Unexpected upstream archive URL: $url" }
        val key = bytesHash(url.toByteArray())
        val receipt = receipt(name, version, key)
        val legacy = checkedSourcePath(vendor, vendor.resolve("archives/$name/$version/$key.json"))
        val previous = when {
            receipt.isFile -> receipt.sourceJson()
            legacy.isFile -> legacy.sourceJson()
            else -> null
        }
        if (previous != null) {
            check(previous["url"] == url && previous["version"] == version) { "Invalid archive receipt: $receipt" }
            val hash = previous["sha256"] as String
            check(Regex("[a-f0-9]{64}").matches(hash) && (previous["publishedSha256"] == null || previous["publishedSha256"] == hash)) { "Invalid archive digest: $receipt" }
            writeSource(vendor, receipt, sourceBytes(previous))
            return Archive(previous) {
                val legacyZip = checkedSourcePath(vendor, legacy.resolveSibling("$hash.zip"))
                val cached = existing?.takeIf { it.isFile } ?: legacyZip.takeIf { it.isFile }
                if (cached == null) download(uri, version, maximum, key, hash).file
                else {
                    check(cached.length() in 1 .. maximum && cached.sourceHash() == hash) { "Cached archive SHA-256 mismatch: $cached" }
                    cached
                }
            }
        }
        val archive = download(uri, version, maximum, key, null)
        writeSource(vendor, receipt, sourceBytes(archive.metadata))
        return archive
    }

    private fun download(uri: URI, version: String, maximum: Long, key: String, expected: String?): Archive {
        check(!offline) { "Offline upstream archive is missing: $uri" }
        val temporary = staging().resolve("$key.zip")
        val releaseHash = releaseDigest(uri)
        val response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
        val advertised = releaseHash ?: response.headers().firstValue("X-Checksum-Sha256").orElse(null)
        val digest = MessageDigest.getInstance("SHA-256")
        response.body().use { body ->
            check(response.statusCode() == 200 && response.uri().scheme == "https") { "Source download returned HTTP ${response.statusCode()}: $uri" }
            DigestInputStream(body, digest).use { input ->
                temporary.outputStream().use { output ->
                    check(input.transferToLimited(output, maximum) > 0) { "Empty source archive: $uri" }
                }
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        check(temporary.sourceHash() == hash && (expected == null || expected == hash)) { "Downloaded archive SHA-256 mismatch: $uri" }
        check(advertised == null || advertised.equals(hash, ignoreCase = true)) { "Upstream archive SHA-256 mismatch: $uri" }
        val metadata = buildMap {
            put("version", version)
            put("url", uri.toString())
            put("sha256", hash)
            if (advertised != null) put("publishedSha256", advertised.lowercase())
        }
        return Archive(metadata) { temporary }
    }

    private fun releaseDigest(uri: URI): String? {
        if (uri.host != "github.com") return null
        val path = Regex("^/([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+)/releases/download/([A-Za-z0-9_.-]+)/[^/]+$").matchEntire(uri.path)
            ?: return null
        val endpoint = "https://api.github.com/repos/${path.groupValues[1]}/releases/tags/${path.groupValues[2]}"
        return releaseDigests.getOrPut(endpoint) {
            // Some releases do not advertise a digest, and anonymous API requests can be rate limited.
            // In that case use HTTPS and the calculated checksum for subsequent local verification.
            val request = HttpRequest.newBuilder(URI(endpoint)).header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(20)).GET().build()
            val response = try { client.send(request, HttpResponse.BodyHandlers.ofInputStream()) }
            catch (_: java.io.IOException) { return@getOrPut emptyMap() }
            response.body().use { body ->
                if (response.statusCode() != 200) return@use emptyMap()
                val bytes = body.readNBytes(4 * 1024 * 1024 + 1)
                check(bytes.size <= 4 * 1024 * 1024) { "Oversized GitHub release metadata: $endpoint" }
                val assets = JsonSlurper().parseText(bytes.toString(Charsets.UTF_8)).sourceObject()["assets"] as List<*>
                assets.mapNotNull { asset ->
                    val entry = asset.sourceObject()
                    val url = entry["browser_download_url"] as? String ?: return@mapNotNull null
                    val digest = (entry["digest"] as? String)?.removePrefix("sha256:") ?: return@mapNotNull null
                    if (Regex("[a-fA-F0-9]{64}").matches(digest)) url to digest else null
                }.toMap()
            }
        }[uri.toString()]
    }

    fun gitTree(name: String, repository: String, version: String, paths: List<String>): GitTree {
        val ref = "refs/tags/v$version"
        val url = "https://github.com/$repository.git"
        val selected = (paths.map(::sourceRelativePath) + "LICENSE.txt").distinct().sorted()
        val selector = bytesHash(sourceBytes(listOf(url, ref, selected)))
        val receipt = receipt(name, version, selector)
        val legacy = checkedSourcePath(vendor, vendor.resolve("git/$name/$version/$selector.json"))
        val record = when {
            receipt.isFile -> receipt.sourceJson()
            legacy.isFile -> legacy.sourceJson()
            else -> null
        }
        if (record != null) {
            check(record["repository"] == repository && record["version"] == version && record["ref"] == ref && record["paths"] == selected &&
                (record["sha256"] as? String)?.matches(Regex("[a-f0-9]{64}")) == true) { "Invalid Git source receipt: $receipt" }
            val metadata = record - "files"
            writeSource(vendor, receipt, sourceBytes(metadata))
            return GitTree(metadata) {
                val legacyTree = checkedSourcePath(vendor, legacy.resolveSibling(metadata["sha256"] as String))
                if (legacyTree.isDirectory && legacy.isFile) {
                    val hashes = legacy.sourceJson()["files"].sourceObject()
                    check(hashes.isNotEmpty() && bytesHash(sourceBytes(hashes.toSortedMap())) == metadata["sha256"]) { "Invalid Git source inventory: $legacy" }
                    for ((relative, hash) in hashes) {
                        val file = checkedSourcePath(legacyTree, legacyTree.resolve(sourceRelativePath(relative)))
                        check(file.isFile && file.sourceHash() == hash) { "Cached Git source changed: $file" }
                    }
                    GitSnapshot(legacyTree, hashes.keys)
                }
                else fetchGitTree(name, repository, version, url, ref, paths, selected, metadata).snapshot
            }
        }
        val tree = fetchGitTree(name, repository, version, url, ref, paths, selected, null)
        writeSource(vendor, receipt, sourceBytes(tree.metadata))
        return tree
    }

    private fun fetchGitTree(name: String, repository: String, version: String, url: String, ref: String, paths: List<String>, selected: List<String>, expected: SourceObject?): GitTree {
        check(!offline) { "Offline Git sources are missing: $repository@$ref" }
        val checkout = staging().resolve("git-${sourceRelativePath(name)}").apply { mkdirs() }
        sourceGit(checkout, "init", "-q")
        sourceGit(checkout, "config", "core.autocrlf", "false")
        sourceGit(checkout, "remote", "add", "origin", url)
        sourceGit(checkout, "config", "remote.origin.promisor", "true")
        sourceGit(checkout, "config", "remote.origin.partialclonefilter", "blob:none")
        sourceGit(checkout, "sparse-checkout", "set", "--cone", "--", *paths.toTypedArray())
        sourceGit(checkout, "fetch", "--depth=1", "--filter=blob:none", "--no-tags", "origin", ref)
        val commit = sourceGit(checkout, "rev-parse", "FETCH_HEAD^{commit}").trim()
        check(Regex("[a-f0-9]{40}").matches(commit)) { "Invalid Git commit for $repository@$ref" }
        sourceGit(checkout, "checkout", "--detach", commit)
        val files = sourceGit(checkout, "ls-files", "-z", "--", *selected.toTypedArray()).split('\u0000').filter { it.isNotEmpty() }
        check("LICENSE.txt" in files && paths.all { path -> files.any { it.startsWith("$path/") } }) { "Missing selected Git sources in $repository@$ref" }
        val hashes = files.associate { relative ->
            val original = checkedSourcePath(checkout, checkout.resolve(sourceRelativePath(relative)))
            check(original.isFile) { "Missing Git source: $original" }
            relative to original.sourceHash()
        }.toSortedMap()
        val hash = bytesHash(sourceBytes(hashes))
        val metadata = mapOf("repository" to repository, "ref" to ref, "commit" to commit, "version" to version, "paths" to selected, "sha256" to hash)
        check(expected == null || metadata == expected) { "Git source changed for $repository@$ref" }
        return GitTree(metadata) { GitSnapshot(checkout, hashes.keys) }
    }
}

private fun java.io.InputStream.transferToLimited(output: java.io.OutputStream, limit: Long): Long {
    var count = 0L
    val buffer = ByteArray(64 * 1024)
    while (true) {
        val read = read(buffer)
        if (read < 0) return count
        count += read
        check(count <= limit) { "Source archive exceeds its size limit" }
        output.write(buffer, 0, read)
    }
}
