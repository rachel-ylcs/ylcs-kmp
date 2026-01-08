package love.yinlin.task.spec

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipSpec {
    private lateinit var file: RegularFile
    private lateinit var folder: Directory

    fun from(folder: Directory) { this.folder = folder }
    fun into(file: RegularFile) { this.file = file }

    private fun addDirectory(directory: File, basePath: String, zos: ZipOutputStream) {
        for (file in directory.listFiles()!!) {
            val entryPath = "$basePath/${file.name}"
            if (file.isDirectory()) {
                val zipEntry = ZipEntry("$entryPath/")
                zos.putNextEntry(zipEntry)
                zos.closeEntry()
                addDirectory(file, entryPath, zos)
            } else {
                val zipEntry = ZipEntry(entryPath)
                zos.putNextEntry(zipEntry)
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun run() {
        val sourcePath = folder.asFile
        val rootDirName = sourcePath.name
        FileOutputStream(file.asFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                zos.setLevel(Deflater.BEST_COMPRESSION)
                val rootEntry = ZipEntry("$rootDirName/")
                zos.putNextEntry(rootEntry)
                zos.closeEntry()
                addDirectory(sourcePath, rootDirName, zos)
            }
        }
    }
}

inline fun zip(action: ZipSpec.() -> Unit) {
    val spec = ZipSpec()
    spec.action()
    spec.run()
}