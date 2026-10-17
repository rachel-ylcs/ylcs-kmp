import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

afterEvaluate {
    val modules = project(":ylcs-module").subprojects.filter { it != project }

    modules.forEach { module ->
        module.tasks.withType<AbstractTestTask>().configureEach {
            val testPath = path
            val testLogger = logger
            testLogging {
                events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
                exceptionFormat = TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
                stackTraceFilters.clear()
            }
            addTestListener(object : TestListener {
                override fun beforeSuite(suite: TestDescriptor) { }
                override fun beforeTest(testDescriptor: TestDescriptor) { }
                override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) { }
                override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                    if (suite.parent == null) {
                        testLogger.lifecycle(
                            "TEST SUMMARY | $testPath | total=${result.testCount} " +
                            "passed=${result.successfulTestCount} failed=${result.failedTestCount} " +
                            "skipped=${result.skippedTestCount}"
                        )
                    }
                }
            })
        }
    }

    tasks.register("androidTest") {
        group = "verification"
        description = "运行所有库模块的Android单元测试"
        dependsOn(modules.map { module -> module.tasks.matching { it.name == "testAndroidHostTest" } })
    }

    tasks.register("iosTest") {
        group = "verification"
        description = "运行所有库模块的iOS ARM64模拟器单元测试"
        dependsOn(modules.map { module -> module.tasks.matching { it.name == "iosSimulatorArm64Test" } })
    }

    tasks.register("desktopTest") {
        group = "verification"
        description = "运行所有库模块的Desktop及JVM单元测试"
        dependsOn(modules.map { module ->
            module.tasks.matching {
                it.name == "desktopTest" || (it.name == "test" && module.plugins.hasPlugin("org.jetbrains.kotlin.jvm"))
            }
        })
    }

    tasks.register("jsTest") {
        group = "verification"
        description = "运行所有库模块的Web JS单元测试"
        dependsOn(modules.map { module -> module.tasks.matching { it.name == "jsTest" } })
    }

    tasks.register("wasmJsTest") {
        group = "verification"
        description = "运行所有库模块的Web Wasm单元测试"
        dependsOn(modules.map { module -> module.tasks.matching { it.name == "wasmJsTest" } })
    }

    tasks.register("windowsTest") {
        group = "verification"
        description = "运行所有库模块的Windows Native单元测试"
        dependsOn(modules.map { module -> module.tasks.matching { it.name == "windowsTest" } })
    }

    tasks.register("linuxTest") {
        group = "verification"
        description = "运行所有库模块的Linux Native单元测试"
        dependsOn(modules.map { module -> module.tasks.matching { it.name == "linuxTest" } })
    }

    tasks.register("macosTest") {
        group = "verification"
        description = "运行所有库模块的macOS Native单元测试"
        dependsOn(modules.map { module -> module.tasks.matching { it.name == "macosTest" } })
    }

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
