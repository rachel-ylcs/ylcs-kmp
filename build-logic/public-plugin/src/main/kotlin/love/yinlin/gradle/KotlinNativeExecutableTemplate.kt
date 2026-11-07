package love.yinlin.gradle

import love.yinlin.nativebinary.NativeReleaseSymbols
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

open class KotlinNativeExecutableTemplate : KotlinNativeTemplate() {
    var commandArgs: List<String> = emptyList()

    fun releaseExecutable(platform: BuildPlatform): Provider<RegularFile> {
        val targetName = when (platform) {
            BuildPlatform.Windows -> "windows"
            BuildPlatform.Linux -> "linux"
            BuildPlatform.Mac -> "macos"
        }
        val target = owner.extensions.getByType<KotlinMultiplatformExtension>().targets.getByName(targetName) as KotlinNativeTarget
        return owner.layout.file(target.binaries.getExecutable(NativeBuildType.RELEASE).linkTaskProvider.flatMap { it.outputFile })
    }

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
