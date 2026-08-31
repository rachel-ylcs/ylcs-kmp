package love.yinlin.cs

import io.ktor.util.logging.LogLevel
import io.ktor.util.logging.Logger
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalDate
import love.yinlin.concurrent.Lock
import love.yinlin.extension.DateEx
import love.yinlin.fs.File
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
class ServerLogger(
    private val console: Boolean, // 开启控制台输出
    private val file: File?, // 开启文件输出
    defaultLevel: LogLevel = LogLevel.TRACE,
) : Logger {
    companion object {
        val Default = ServerLogger(true, null)
    }

    override var level: LogLevel = defaultLevel

    private val lock = Lock()
    private var currentDate: LocalDate? = null
    private var fileHandle: CPointer<FILE>? = null

    private fun writeToFile(rootPath: File, date: LocalDate, content: String) {
        lock.synchronized {
            if (!rootPath.existsSync()) rootPath.mkdirSync()

            if (currentDate != date || fileHandle == null) {
                val dateStr = DateEx.Formatter.standardDate.format(date)

                if (dateStr != null) {
                    fileHandle?.let { fclose(it) }
                    fileHandle = fopen("${rootPath.path}/$dateStr.log", "a")
                    currentDate = date
                }
            }

            fileHandle?.let { fp ->
                fputs(content, fp)
                fflush(fp)
            }
        }
    }

    private fun log(targetLevel: LogLevel, message: String, cause: Throwable? = null) {
        if (targetLevel.ordinal < level.ordinal) return

        val datetime = DateEx.Current
        val date = datetime.date
        val time = DateEx.Formatter.standardTime.format(datetime.time) ?: return

        val fullMessage = buildString {
            append("[$time ${targetLevel.name}] $message")
            if (cause != null) {
                appendLine()
                append(cause.stackTraceToString())
            }
            appendLine()
        }

        if (console) print(fullMessage)
        if (file != null) writeToFile(file, date, fullMessage)
    }

    internal fun close() {
        lock.synchronized {
            fileHandle?.let(::fclose)
            fileHandle = null
        }
    }

    override fun error(message: String) = log(LogLevel.ERROR, message)
    override fun error(message: String, cause: Throwable) = log(LogLevel.ERROR, message, cause)
    override fun warn(message: String) = log(LogLevel.WARN, message)
    override fun warn(message: String, cause: Throwable) = log(LogLevel.WARN, message, cause)
    override fun info(message: String) = log(LogLevel.INFO, message)
    override fun info(message: String, cause: Throwable) = log(LogLevel.INFO, message, cause)
    override fun debug(message: String) = log(LogLevel.DEBUG, message)
    override fun debug(message: String, cause: Throwable) = log(LogLevel.DEBUG, message, cause)
    override fun trace(message: String) = log(LogLevel.TRACE, message)
    override fun trace(message: String, cause: Throwable) = log(LogLevel.TRACE, message, cause)
}