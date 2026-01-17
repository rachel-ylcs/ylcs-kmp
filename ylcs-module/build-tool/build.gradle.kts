import love.yinlin.task.BuildDesktopNativeTask

afterEvaluate {
    val buildDesktopNativePAG by tasks.registering(BuildDesktopNativeTask::class) {
        val projectDir = projects.ylcsModule.compose.ui.pagKmp.projectDir(this@afterEvaluate).asFile
        val sourceDir = projectDir.resolve("src/desktopMain/cpp")
    }
}