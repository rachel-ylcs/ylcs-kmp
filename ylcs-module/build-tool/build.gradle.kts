import love.yinlin.task.BuildDesktopNativeTask

afterEvaluate {
//    tasks.register<BuildDesktopNativeTask>("buildDesktopNativeTemplate") {
//        description = "编译Native库模板"
//        val targetProject = findProject(projects.ylcsModule.plugin.pagKmp)!! // 模块
//        val sourceDir = targetProject.desktopNativeKMPSourceDir.asFile // 源代码路径
//        onlyIf {
//            ensureNativeBuild(sourceDir)
//        }
//        inputDir = sourceDir
//        nativeModuleName = targetProject.name // 目标模块名
//        nativeBuildDir = targetProject.desktopNativeBuildDir.asFile // 编译目录
//        preCommands = listOf("") // 预先执行的脚本
//    }
}