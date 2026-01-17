import love.yinlin.task.BuildDesktopNativeTask

afterEvaluate {
    println(projects.ylcsModule.compose.ui.pagKmp.path)
    val buildDesktopNativePAG by tasks.registering(BuildDesktopNativeTask::class) {

    }
}