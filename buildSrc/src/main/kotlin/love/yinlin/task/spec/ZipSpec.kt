package love.yinlin.task.spec

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

fun zip(from: Directory, into: RegularFile) {
    val sourcePath = from.asFile
    val rootDirName = sourcePath.name
    FileOutputStream(into.asFile).use { fos ->
        ZipOutputStream(fos).use { zos ->
            zos.setLevel(Deflater.BEST_COMPRESSION)
            val rootEntry = ZipEntry("$rootDirName/")
            zos.putNextEntry(rootEntry)
            zos.closeEntry()
            addDirectory(sourcePath, rootDirName, zos)
        }
    }
}