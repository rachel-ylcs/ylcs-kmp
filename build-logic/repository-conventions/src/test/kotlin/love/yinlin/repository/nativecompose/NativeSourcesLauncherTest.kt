package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

class NativeSourcesLauncherTest {
    @Test
    fun sourceMaintenanceEntryRunsWithItsStandaloneRuntimeClasspath() {
        val work = Files.createTempDirectory("native-sources-launcher").toFile()
        try {
            val native = work.resolve("native-compose").apply { mkdirs() }
            val java = File(System.getProperty("java.home"), "bin/${if (File.separatorChar == '\\') "java.exe" else "java"}")
            val classpath = checkNotNull(System.getProperty("native.sources.runtimeClasspath"))
            val log = work.resolve("launcher.log")
            val original = File(checkNotNull(System.getProperty("native.sources.repositoryRoot")))
            for (relative in listOf("native-compose/build.gradle.kts", ".gitattributes", "gradle.properties",
                "gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.jar", "gradlew", "gradlew.bat")) {
                original.resolve(relative).copyTo(work.resolve(relative))
            }
            work.resolve("libs.versions.toml").writeText("[versions]\ncmp = \"1.13.0-beta02\"\nskiko = \"0.156.1\"\nsdl = \"3.4.20\"\nicu = \"78.2\"\n[libraries]\n")
            work.resolve("settings.gradle.kts").writeText("repository { rachel { jvm { version = 25 } } }")
            native.resolve("upstream/patches").mkdirs()
            native.resolve("upstream/compose.json").writeText("""{"repository":"JetBrains/compose-multiplatform-core","modules":{}}""")
            native.resolve("upstream/skiko.json").writeText("""{"group":"love.yinlin.native.skiko"}""")
            for (name in listOf("sdl", "icu", "patches/manifest")) native.resolve("upstream/$name.json").writeText("{}")
            val prepare = ProcessBuilder(java.path, "-cp", classpath, NativeSourcesMain::class.java.name, native.path, "offline").redirectErrorStream(true).redirectOutput(log).start()
            check(prepare.waitFor(30, TimeUnit.SECONDS)) {
                prepare.destroyForcibly()
                "Source preparation launcher timed out"
            }
            assertTrue(prepare.exitValue() != 0)
            assertTrue("Offline Git sources are missing" in log.readText(), log.readText())
            assertTrue(!native.resolve("vendor/current.json").exists())
        } finally { assertTrue(work.deleteRecursively()) }
    }
}
