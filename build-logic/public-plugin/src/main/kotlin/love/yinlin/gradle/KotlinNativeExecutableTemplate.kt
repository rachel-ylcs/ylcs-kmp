package love.yinlin.gradle

import love.yinlin.nativebinary.NativeReleaseSymbols
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

open class KotlinNativeExecutableTemplate : KotlinNativeTemplate() {
    var commandArgs: List<String> = emptyList()

    private var executableBlock: Executable.() -> Unit = {}
    fun executable(block: Executable.() -> Unit) {
        val previous = executableBlock
        executableBlock = {
            previous()
            block()
        }
    }
    internal fun Executable.executable() { executableBlock(this) }

    override fun KotlinNativeTarget.baseNative() {
        binaries {
            executable {
                baseName = uniqueSafeName
                entryPoint = "main"
                val runArguments = commandArgs
                runTaskProvider?.configure { setArgs(runArguments) }

                executable()
            }
        }

        native()
        NativeReleaseSymbols.configure(project, this)
    }
}
