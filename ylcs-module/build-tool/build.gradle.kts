import love.yinlin.task.BuildDesktopNativeTask

afterEvaluate {
    val buildDesktopNativePAG by tasks.registering(BuildDesktopNativeTask::class) {
        val targetProject = findProject(projects.ylcsModule.compose.ui.pagKmp)!!
        val sourceDir = targetProject.desktopNativeKMPSourceDir.asFile
        onlyIf {
            sourceDir.exists() && sourceDir.resolve("native.ignore").exists()
        }
        inputDir = sourceDir
        nativeModuleName = targetProject.name
        nativeBuildDir = targetProject.desktopNativeBuildDir.asFile
        // https://github.com/Tencent/libpag
        preCommands = when (C.platform) {
            BuildPlatform.Windows -> listOf(
                "set \"CMAKE_MSVC_PATH=%VS_PATH%\\VC\"",
                "npm install -g depsync",
                "depsync --project %NATIVE_SOURCE_DIR%\\libpag",
            )
            BuildPlatform.Linux -> listOf(
                "npm install -g depsync",
                $$"depsync --project ${NATIVE_SOURCE_DIR}/libpag"
            )
            BuildPlatform.Mac -> listOf(
                $$"${NATIVE_SOURCE_DIR}/libpag/sync_deps.sh"
            )
        }
    }
}