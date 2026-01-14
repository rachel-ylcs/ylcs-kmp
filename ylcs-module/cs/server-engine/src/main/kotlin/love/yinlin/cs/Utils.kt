package love.yinlin.cs

import love.yinlin.extension.catchingDefault
import java.io.File
import java.net.JarURLConnection
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.sql.Timestamp

internal fun copyResources(classLoader: ClassLoader, root: String) {
    fun copyResourcesFromFile(source: File, target: File) {
        if (!target.exists()) target.mkdirs()
        source.listFiles()?.forEach { file ->
            val destFile = File(target, file.name)
            if (file.isDirectory) copyResourcesFromFile(file, destFile)
            else Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    val targetFile = File(System.getProperty("user.dir"), root)
    val publicResourceUrl = classLoader.getResource(root)!!
    if (publicResourceUrl.protocol == "jar") {
        val jarFile = (publicResourceUrl.openConnection() as JarURLConnection).jarFile
        val entries = jarFile.entries()
        if (!targetFile.exists()) {
            targetFile.mkdirs()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith(root) && !entry.isDirectory) {
                    val writeFile = File(targetFile, entry.name.substring(root.length + 1))
                    writeFile.parentFile?.mkdirs()
                    jarFile.getInputStream(entry).use { input ->
                        Files.copy(input, writeFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }
    else copyResourcesFromFile(File(publicResourceUrl.toURI()), targetFile)
}

val String.md5: String get() = catchingDefault("") {
    val md = MessageDigest.getInstance("MD5")
    md.update(this.toByteArray(StandardCharsets.UTF_8))
    val hexString = StringBuilder()
    for (b in md.digest()) {
        val hex = Integer.toHexString(0xff and b.toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
    }
    hexString.toString()
}

val currentTS: Timestamp get() = Timestamp(System.currentTimeMillis())

fun currentUniqueId(id: Any? = null) = "${System.currentTimeMillis()}${id}"