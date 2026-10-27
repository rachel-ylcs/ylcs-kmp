package love.yinlin.nativebridge.verification

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativePublicationVerificationTest {
    private fun fixture(work: File): NativePublicationVerification {
        val native = work.resolve("native-compose").apply { mkdirs() }
        work.resolve("libs.versions.toml").writeText("[versions]\nkotlin = \"2.5.0-Beta2\"\ncmp = \"1.13.0-beta01\"\nskiko = \"0.156.0\"\nsdl = \"3.4.18\"\n[libraries]\n")
        native.resolve("upstream").mkdirs()
        native.resolve("upstream/compose.json").writeText("""{"version":"1.13.0-beta01","modules":{}}""")
        native.resolve("upstream/lock.json").writeText("""{"versions":{"skiko":"0.156.0"}}""")
        return NativePublicationVerification(native)
    }

    @Test
    fun mavenChecksumsAndFilePathsAreValidated() {
        val work = Files.createTempDirectory("native-publication-files").toFile()
        try {
            val verifier = fixture(work)
            val directory = work.resolve("publication").apply { mkdirs() }
            val artifact = directory.resolve("fixture.klib").apply { writeText("binary fixture") }
            val file = mapOf("url" to artifact.name, "size" to artifact.length(), "sha256" to artifact.digest("sha256"))
            val metadata = mapOf("variants" to listOf(mapOf("files" to listOf(file))))
            verifier.verifyFiles(metadata, directory)
            artifact.writeText("modified bytes")
            assertFailsWith<IllegalStateException> { verifier.verifyFiles(metadata, directory) }
            val unsafe = mapOf("variants" to listOf(mapOf("files" to listOf(file + ("url" to "../outside.klib")))))
            assertFailsWith<IllegalStateException> { verifier.verifyFiles(unsafe, directory) }
        } finally { assertTrue(work.deleteRecursively()) }
    }

    @Test
    fun leafPomUsesItsOwnCoordinatesWhenMetadataIdentifiesTheRootComponent() {
        val work = Files.createTempDirectory("native-publication-leaf").toFile()
        try {
            val verifier = fixture(work)
            val directory = work.resolve("skiko-mingwx64/0.156.0").apply { mkdirs() }
            val metadata = mapOf("component" to mapOf("group" to "test.native", "module" to "skiko", "version" to "0.156.0", "url" to "../../skiko/0.156.0/skiko-0.156.0.module"))
            val pom = directory.resolve("skiko-mingwx64-0.156.0.pom")
            pom.writeText("""<project><groupId>test.native</groupId><artifactId>skiko-mingwx64</artifactId><version>0.156.0</version></project>""")
            verifier.verifyPublication(metadata, directory, "test.native", "skiko-mingwx64", "0.156.0")
            pom.writeText("""<project><groupId>test.native</groupId><artifactId>skiko</artifactId><version>0.156.0</version></project>""")
            assertFailsWith<IllegalStateException> { verifier.verifyPublication(metadata, directory, "test.native", "skiko-mingwx64", "0.156.0") }
        } finally { assertTrue(work.deleteRecursively()) }
    }

    @Test
    fun missingSkippedAndDuplicateRegressionReportsCannotPass() {
        val work = Files.createTempDirectory("native-publication-reports").toFile()
        try {
            val verifier = fixture(work)
            val results = work.resolve("build/test-results/nativeTest").apply { mkdirs() }
            val report = results.resolve("TEST-fixture.xml")
            report.writeText("""<testsuite name="example.FixtureTest" tests="2" errors="0" failures="0" skipped="0"/>""")
            verifier.verifyReports(listOf(work.resolve("build")), mapOf("FixtureTest" to 2))
            report.writeText("""<testsuite name="example.FixtureTest" tests="2" errors="0" failures="0" skipped="1"/>""")
            assertFailsWith<IllegalStateException> { verifier.verifyReports(listOf(work.resolve("build")), mapOf("FixtureTest" to 2)) }
            report.writeText("""<testsuite name="example.FixtureTest" tests="2" errors="0" failures="0" skipped="0"/>""")
            report.copyTo(results.resolve("TEST-duplicate.xml"))
            assertFailsWith<IllegalStateException> { verifier.verifyReports(listOf(work.resolve("build")), mapOf("FixtureTest" to 2)) }
            results.listFiles().orEmpty().forEach { it.delete() }
            assertFailsWith<IllegalStateException> { verifier.verifyReports(listOf(work.resolve("build")), mapOf("FixtureTest" to 2)) }
        } finally { assertTrue(work.deleteRecursively()) }
    }

    @Test
    fun expectedReportsIncludeOnlyTheSelectedHostTests() {
        val work = Files.createTempDirectory("native-publication-host").toFile()
        try {
            val verifier = fixture(work)
            val module = work.resolve("native-compose/adaptation/test/src")
            for (sourceSet in listOf("nativeTest", "windowsTest", "macosTest")) {
                val file = module.resolve("$sourceSet/kotlin/${sourceSet}Fixture.kt").apply { parentFile.mkdirs() }
                file.writeText("class ${sourceSet}Fixture {\n    @Test\n    fun case() = Unit\n}")
            }
            assertEquals(setOf("nativeTestFixture", "windowsTestFixture"), verifier.expectedTests("mingw_x64", false).keys)
            assertEquals(setOf("nativeTestFixture", "macosTestFixture"), verifier.expectedTests("macos_arm64", false).keys)
        } finally { assertTrue(work.deleteRecursively()) }
    }

    @Test
    fun assemblyRejectsMissingHostsAndOverlappingPathsBeforeCreatingOutput() {
        val work = Files.createTempDirectory("native-publication-merge").toFile()
        try {
            val verifier = fixture(work)
            val output = work.resolve("output")
            assertFailsWith<IllegalStateException> { verifier.assemble(mapOf("mingw_x64" to work.resolve("windows")), output) }
            assertFalse(output.exists())
            val inputs = targetSuffixes.keys.associateWith { work.resolve("inputs/$it") }
            assertFailsWith<IllegalStateException> { verifier.assemble(inputs, inputs.getValue("mingw_x64").resolve("output")) }
            assertFalse(inputs.getValue("mingw_x64").resolve("output").exists())
        } finally { assertTrue(work.deleteRecursively()) }
    }
}
