package love.yinlin

import love.yinlin.extension.catching
import love.yinlin.platform.OS
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
        if (entryName.startsWith("app/")) entryName = entryName.replaceFirst("app/", "")
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

    private fun unixScript(currentDir: Path, newName: String) = arrayOf(
        "sh", "-c", "sleep 3 && cd $currentDir && (if [ -d $newName ]; then rm -rf app && mv $newName app; fi)"
    )

    private fun startScript(currentDir: Path, newName: String) {
        ProcessBuilder(*OS.ifPlatform(
            Windows,
            ifTrue = { windowsScript(newName) },
            ifFalse = { unixScript(currentDir, newName) })
        ).inheritIO().start()
    }

    fun start(filename: String) = catching {
        val currentDir = Paths.get(System.getProperty("compose.application.resources.dir"))
            .parent.parent.toAbsolutePath()

        // 1. 解压更新包到当前目录
        val zipPath = File(filename)
        val newName = System.currentTimeMillis().toString()
        val unzipPath = currentDir.resolve(newName)
        unzipPackage(unzipPath, zipPath)
        // 2. 启动脚本
        startScript(currentDir, newName)
        // 3. 结束自身进程
        exitProcess(0)
    }
}