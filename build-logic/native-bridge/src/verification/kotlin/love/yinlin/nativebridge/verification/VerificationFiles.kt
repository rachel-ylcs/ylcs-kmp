package love.yinlin.nativebridge.verification

import groovy.json.JsonSlurper
import org.w3c.dom.Document
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipFile
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

internal typealias JsonObject = Map<String, Any?>

@Suppress("UNCHECKED_CAST")
internal fun Any?.jsonObject(): JsonObject = this as? Map<String, Any?> ?: error("Expected a JSON object")

internal fun JsonObject.objects(name: String): List<JsonObject> = (this[name] as? List<*>)?.map { it.jsonObject() } ?: emptyList()

internal fun File.readJson(): JsonObject {
    check(isFile && length() <= 32L * 1024 * 1024) { "Missing or oversized JSON input: $this" }
    return JsonSlurper().parse(this).jsonObject()
}

internal fun File.contained(relative: String): File {
    check(relative.isNotEmpty() && '\\' !in relative && ':' !in relative && !relative.startsWith('/') && relative.split('/').none { it == ".." }) { "Unsafe relative path: $relative" }
    val result = resolve(relative).canonicalFile
    check(result.toPath().startsWith(canonicalFile.toPath()) && result != canonicalFile) { "File escapes its directory: $relative" }
    return result
}

internal fun File.digest(algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm)
    inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

internal fun ZipFile.entryText(name: String): String {
    val entry = getEntry(name) ?: error("Missing archive entry: $name")
    check(entry.size in 0 .. 4L * 1024 * 1024) { "Oversized text archive entry: $name" }
    return getInputStream(entry).use { input ->
        val bytes = input.readNBytes(4 * 1024 * 1024 + 1)
        check(bytes.size <= 4 * 1024 * 1024) { "Oversized text archive entry: $name" }
        bytes.toString(Charsets.UTF_8)
    }
}

internal fun File.safeXml(): Document {
    check(length() <= 8L * 1024 * 1024) { "Oversized XML: $this" }
    val factory = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        isXIncludeAware = false
        isExpandEntityReferences = false
    }
    return factory.newDocumentBuilder().parse(this)
}

internal fun File.copyTreeTo(destination: File) {
    check(isDirectory && !Files.isSymbolicLink(toPath())) { "Missing or linked source directory: $this" }
    for (source in walkTopDown()) {
        check(!Files.isSymbolicLink(source.toPath())) { "Symlink in verification input: $source" }
        val target = destination.resolve(source.relativeTo(this).path)
        if (source.isDirectory) check(target.isDirectory || target.mkdirs()) { "Cannot create $target" }
        else {
            target.parentFile.mkdirs()
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }
    }
}

internal fun hostTarget(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    return when {
        os.startsWith("windows") && arch in setOf("amd64", "x86_64") -> "mingw_x64"
        os.startsWith("linux") && arch in setOf("amd64", "x86_64") -> "linux_x64"
        os.startsWith("mac") && arch in setOf("aarch64", "arm64") -> "macos_arm64"
        else -> error("Unsupported Native host: $os/$arch")
    }
}

internal val targetSuffixes = linkedMapOf("mingw_x64" to "mingwx64", "linux_x64" to "linuxx64", "macos_arm64" to "macosarm64")
internal val targetSourceNames = mapOf("mingw_x64" to "windows", "linux_x64" to "linux", "macos_arm64" to "macos")
