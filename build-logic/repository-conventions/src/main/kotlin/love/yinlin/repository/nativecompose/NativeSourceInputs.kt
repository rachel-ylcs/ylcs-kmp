package love.yinlin.repository.nativecompose

import groovy.json.JsonSlurper
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Duration

/** The reference records are local download receipts, never a second source of versions. */
internal class NativeSourceInputs(private val sync: NativeUpstreamSync, private val offline: Boolean) {
    private val vendor = sync.vendor
    private val client by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build()
    }

    private val releaseDigests = mutableMapOf<String, Map<String, String>>()

    data class Archive(val file: File, val metadata: SourceObject)
    data class GitTree(val directory: File, val files: Map<String, String>, val metadata: SourceObject)

    fun archive(name: String, version: String, url: String, maximum: Long): Archive {
        val uri = URI(url)
        check(uri.scheme == "https" && uri.host in setOf("codeload.github.com", "github.com", "repo.maven.apache.org")) { "Unexpected upstream archive URL: $url" }
        val key = bytesHash(url.toByteArray())
        val owner = checkedSourcePath(vendor, vendor.resolve("archives/${sourceRelativePath(name)}/${sourceRelativePath(version)}"))
        val receipt = checkedSourcePath(vendor, owner.resolve("$key.json"))
        fun content(hash: String): File {
            check(Regex("[a-f0-9]{64}").matches(hash)) { "Invalid SHA-256 in $receipt" }
            return checkedSourcePath(vendor, owner.resolve("$hash.zip"))
        }
        if (receipt.isFile) {
            val metadata = receipt.sourceJson()
            check(metadata["url"] == url && metadata["version"] == version) { "Invalid archive receipt: $receipt" }
            val hash = metadata["sha256"] as String
            check(metadata["publishedSha256"] == null || metadata["publishedSha256"] == hash) { "Cached archive conflicts with its published SHA-256: $receipt" }
            val file = content(hash)
            if (file.isFile) {
                check(file.length() in 1 .. maximum && file.sourceHash() == hash) { "Cached archive SHA-256 mismatch: $file" }
                return Archive(file, metadata)
            }
        }
        check(!offline) { "Offline upstream archive is missing: $url" }
        var resolved: Archive? = null
        sync.stage(".download-") { staging ->
            val temporary = staging.resolve("archive.zip")
            val releaseHash = releaseDigest(uri)
            val response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
            val advertised = releaseHash ?: response.headers().firstValue("X-Checksum-Sha256").orElse(null)
            val digest = MessageDigest.getInstance("SHA-256")
            response.body().use { body ->
                check(response.statusCode() == 200 && response.uri().scheme == "https") { "Source download returned HTTP ${response.statusCode()}: $url" }
                DigestInputStream(body, digest).use { input ->
                    temporary.outputStream().use { output ->
                        val count = input.transferToLimited(output, maximum)
                        check(count > 0) { "Empty source archive: $url" }
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            check(temporary.sourceHash() == hash) { "Downloaded archive SHA-256 mismatch: $url" }
            check(advertised == null || advertised.equals(hash, ignoreCase = true)) { "Upstream archive SHA-256 mismatch: $url" }
            val file = content(hash)
            owner.mkdirs()
            if (file.exists()) check(file.isFile && file.sourceHash() == hash) { "Cached archive SHA-256 mismatch: $file" }
            else Files.move(temporary.toPath(), file.toPath())
            val metadata = buildMap {
                put("version", version)
                put("url", url)
                put("sha256", hash)
                if (advertised != null) put("publishedSha256", advertised.lowercase())
            }
            writeSource(vendor, receipt, sourceBytes(metadata))
            resolved = Archive(file, metadata)
        }
        return checkNotNull(resolved)
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
        val owner = checkedSourcePath(vendor, vendor.resolve("git/$name/$version"))
        val receipt = checkedSourcePath(vendor, owner.resolve("$selector.json"))
        fun content(hash: String): File {
            check(Regex("[a-f0-9]{64}").matches(hash)) { "Invalid Git tree SHA-256: $hash" }
            return checkedSourcePath(vendor, owner.resolve(hash))
        }
        if (receipt.isFile) {
            val record = receipt.sourceJson()
            check(record["repository"] == repository && record["version"] == version && record["ref"] == ref && record["paths"] == selected) { "Invalid Git source receipt: $receipt" }
            val hashes = record["files"].sourceObject().mapValues { (_, hash) -> hash as String }
            val metadata = record - "files"
            val expected = metadata["sha256"] as String
            val directory = content(expected)
            if (directory.isDirectory) {
                check(bytesHash(sourceBytes(hashes.toSortedMap())) == expected && hashes.isNotEmpty()) { "Invalid Git source inventory: $receipt" }
                for ((relative, hash) in hashes) {
                    val file = checkedSourcePath(directory, directory.resolve(sourceRelativePath(relative)))
                    check(file.isFile && file.sourceHash() == hash) { "Cached Git source changed: $file" }
                }
                return GitTree(directory, hashes, metadata)
            }
        }
        check(!offline) { "Offline Git sources are missing: $repository@$ref" }
        var resolved: GitTree? = null
        sync.stage(".git-source-") { staging ->
            val checkout = staging.resolve("git").apply { mkdirs() }
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
            val snapshot = staging.resolve("tree").apply { mkdirs() }
            val files = sourceGit(checkout, "ls-files", "-z", "--", *selected.toTypedArray()).split('\u0000').filter { it.isNotEmpty() }
            check("LICENSE.txt" in files && paths.all { path -> files.any { it.startsWith("$path/") } }) { "Missing selected Git sources in $repository@$ref" }
            val hashes = files.associate { relative ->
                val original = checkedSourcePath(checkout, checkout.resolve(sourceRelativePath(relative)))
                check(original.isFile) { "Missing Git source: $original" }
                val copy = original.copyTo(checkedSourcePath(snapshot, snapshot.resolve(relative)))
                val hash = original.sourceHash()
                check(copy.sourceHash() == hash) { "Git snapshot SHA-256 mismatch: $relative" }
                relative to hash
            }.toSortedMap()
            val hash = bytesHash(sourceBytes(hashes))
            val directory = content(hash)
            owner.mkdirs()
            if (directory.exists()) {
                for ((relative, expected) in hashes) {
                    val existing = checkedSourcePath(directory, directory.resolve(relative))
                    check(existing.isFile && existing.sourceHash() == expected) { "Unrecognized Git cache content: $existing" }
                }
            }
            else Files.move(snapshot.toPath(), directory.toPath())
            val metadata = mapOf("repository" to repository, "ref" to ref, "commit" to commit, "version" to version, "paths" to selected, "sha256" to hash)
            writeSource(vendor, receipt, sourceBytes(metadata + ("files" to hashes)))
            resolved = GitTree(directory, hashes, metadata)
        }
        return checkNotNull(resolved)
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
