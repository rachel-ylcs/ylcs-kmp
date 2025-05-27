package love.yinlin

import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.system.exitProcess

object AutoUpdate {
    private fun handleEntry(zipFile: ZipFile, entry: ZipEntry, targetDir: Path) {
        var entryName = entry.getName()
        entryName = entryName.replace('\\', '/')
        while (entryName.startsWith("/")) entryName = entryName.substring(1)
        val entryPath: Path = targetDir.resolve(entryName).normalize()
        if (!entryPath.startsWith(targetDir)) error(entry.getName())
        if (entry.isDirectory) Files.createDirectories(entryPath)
        else {
            val parentDir: Path? = entryPath.parent
            if (parentDir != null && !Files.exists(parentDir)) Files.createDirectories(parentDir)
            zipFile.getInputStream(entry).use { stream ->
                Files.copy(stream, entryPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun unzipPackage(currentDir: Path, zipPath: File) {
        ZipFile(zipPath).use { zipFile ->
            val entries: Enumeration<out ZipEntry?> = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry: ZipEntry? = entries.nextElement()
                handleEntry(zipFile, entry!!, currentDir)
            }
        }
    }

    private fun windowsScript(newName: String) = arrayOf(
        "cmd", "/c", "start", "\"\"", "/B", "cmd", "/c",
        "ping -n 3 localhost >nul 2>&1 & (if exist $newName (rmdir /s /q app && ren $newName app))"
    )

    // TODO
    private fun linuxScript(newName: String) = emptyArray<String>()

    // TODO
    private fun macOSScript(newName: String) = emptyArray<String>()

    private fun startScript(currentDir: Path, newName: String) {
        ProcessBuilder(*windowsScript(newName)).inheritIO().start()
    }

    fun start(filename: String) {
        if (OS.platform != Platform.Windows) return
        try {
            val currentDir = Paths.get("").toAbsolutePath()

            // 1. 解压更新包到当前目录
            val zipPath = File(filename)
            unzipPackage(currentDir, zipPath)
            // 2. 重命名
            val unzipPath = currentDir.resolve(zipPath.nameWithoutExtension)
            val newName = System.currentTimeMillis().toString()
            val targetPath = currentDir.resolve(newName)
            unzipPath.toFile().renameTo(targetPath.toFile())
            // 3. 启动脚本
            startScript(currentDir, newName)
            // 4. 结束自身进程
            exitProcess(0)
        }
        catch (_: Throwable) { }
    }
}