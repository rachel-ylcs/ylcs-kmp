import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    install(
        libs.plugins.kotlinMultiplatform,
        libs.plugins.androidLibraryNew,
    )
}

val desktopNativeList = listOf(
    projects.ylcsModule.foundation.os.desktopPlayer,
    projects.ylcsModule.foundation.service.mmkvKmp,
    projects.ylcsModule.foundation.service.picker,
)

template(object : KotlinMultiplatformTemplate() {
    override fun KotlinMultiplatformSourceSetsScope.source() {
        val jvmMain by create(commonMain)

        androidMain.configure(jvmMain)

        iosMain.configure(commonMain)

        iosMainList.configure(iosMain)

        desktopMain.configure(jvmMain)
    }

    override fun Project.actions() {
        val buildNativeLib by tasks.registering {
            // 编译 Kotlin Native 库
            dependsOn()

            // 编译 CMake Native 库
            doLast {
                val outputDir = C.root.artifacts.desktopNative.asFile
                outputDir.mkdirs()

                val execOpt = project.serviceOf<ExecOperations>()

                for (module in desktopNativeList) buildCMakeLists(execOpt, outputDir, module)
            }
        }
    }
})

private fun buildArgs(vararg args: String) = args.joinToString(" && ")

private fun ExecOperations.exec(currentDir: File, vararg args: String): Boolean {
    return exec {
        workingDir = currentDir
        standardOutput = System.out
        errorOutput = System.err
        commandLine(*args)
    }.exitValue == 0
}

private fun buildCMakeLists(execOpt: ExecOperations, outputDir: File, module: DelegatingProjectDependency) {
    // 确定输入和输出目录
    val modulePath = module.path.removePrefix(":").split(":").joinToString(File.separator)
    val moduleDir = rootProject.layout.projectDirectory.asFile.resolve(modulePath)

    var sourceDir = moduleDir.resolve("src/desktopMain/cpp") // Kotlin Multiplatform
    if (!sourceDir.exists()) sourceDir = moduleDir.resolve("src/main/cpp") // Kotlin Jvm
    if (!sourceDir.exists()) throw GradleException("This Project does not contain cpp directory!")

    val moduleName = module.name.replace('-', '_')
    val libName = System.mapLibraryName(moduleName)

    // Native build dir
    val nativeBuildDir = sourceDir.resolve("build")
    nativeBuildDir.deleteRecursively()
    nativeBuildDir.mkdir()

    val cmakeArgs = buildList {
        add("CMAKE_BUILD_TYPE" to "Release")
        add("NATIVE_JNI_DIR" to "\"${project.layout.projectDirectory.dir("include").asFile.absolutePath}\"")
        add("NATIVE_OUTPUT_DIR" to outputDir.absolutePath)
        add("NATIVE_OUTPUT_NAME" to moduleName)
        if (C.platform == BuildPlatform.Windows) {
            add("CMAKE_SHARED_LINKER_FLAGS" to "\"/NOEXP /NOIMPLIB\"")
        }
    }
    val cmakeArgsText = cmakeArgs.joinToString(" ") { (key, value) -> "-D$key=$value" }

    // 调用工具链编译 Native 库
    if (C.platform == BuildPlatform.Windows) {
        // 检查工具链
        val vsPath = System.getenv("VS_PATH") ?: throw GradleException("can not find Visual Studio from VS_PATH environment variable!")
        val vcPath = File(vsPath).resolve("VC/Auxiliary/Build/vcvars64.bat")
        if (!vcPath.exists()) throw GradleException("Can not find Visual Studio from \"VS_PATH\" environment variable!")

        // Windows 调用 Ninja 使用 MSBuild 构建
        execOpt.exec(nativeBuildDir, "cmd", "/c", buildArgs(
            "call $vcPath",
            "cmake .. -G Ninja $cmakeArgsText",
            "cmake --build . --config Release"
        ))
    }
    else {
        // Linux / macOS 直接 build
        execOpt.exec(nativeBuildDir, "sh", "-c", buildArgs(
            "cmake .. $cmakeArgsText",
            "make -j 4"
        ))
    }
    println("Build \"$libName\" for desktop targets complete.")
}