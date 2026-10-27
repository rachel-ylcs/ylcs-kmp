package love.yinlin.repository.nativecompose

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
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
            val cached = ProcessBuilder(java.path, "-cp", classpath, NativeSourcesMain::class.java.name, "cache-restore", native.path).redirectErrorStream(true).redirectOutput(log).start()
            check(cached.waitFor(30, TimeUnit.SECONDS)) {
                cached.destroyForcibly()
                "Source cache launcher timed out"
            }
            assertEquals(0, cached.exitValue(), log.readText())
            assertTrue("No prepared source snapshot" in log.readText())

            work.resolve("libs.versions.toml").writeText("[versions]\ncmp = \"1.13.0-beta01\"\nskiko = \"0.156.0\"\n[libraries]\n")
            work.resolve("settings.gradle.kts").writeText("repository { rachel { jvm { version = 25 } } }")
            native.resolve("upstream/patches").mkdirs()
            native.resolve("upstream/compose.json").writeText("""{"version":"deliberate-mismatch","modules":{}}""")
            native.resolve("upstream/lock.json").writeText("""{"versions":{"skiko":"0.156.0"}}""")
            native.resolve("upstream/patches/manifest.json").writeText("{}")
            val prepare = ProcessBuilder(java.path, "-cp", classpath, NativeSourcesMain::class.java.name, "prepare", native.path, "offline").redirectErrorStream(true).redirectOutput(log).start()
            check(prepare.waitFor(30, TimeUnit.SECONDS)) {
                prepare.destroyForcibly()
                "Source preparation launcher timed out"
            }
            assertTrue(prepare.exitValue() != 0)
            assertTrue("Update the pinned upstream sources" in log.readText(), log.readText())
        } finally { assertTrue(work.deleteRecursively()) }
    }
}
