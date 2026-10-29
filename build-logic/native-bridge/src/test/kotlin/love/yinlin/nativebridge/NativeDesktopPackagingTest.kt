package love.yinlin.nativebridge

import org.gradle.api.tasks.bundling.Zip
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.zip.ZipFile
import kotlin.test.*

class NativeDesktopPackagingTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun archivesKeepExecutablesInFlatAndMacosBundleLayouts() {
        for (executablePath in listOf("ModManager.exe", "ModManager.kexe", "ModManager.app/Contents/MacOS/ModManager.kexe")) {
            val project = ProjectBuilder.builder().withProjectDir(temporary.newFolder()).build()
            val distribution = project.file("distribution").apply { mkdirs() }
            val executable = distribution.resolve(executablePath).apply {
                parentFile.mkdirs()
                writeBytes(byteArrayOf(0x4d, 0x5a, 1, 2, 3))
            }
            val runtime = executable.parentFile.resolve("icudtl.dat").apply { writeText("runtime fixture") }
            val archive = project.tasks.register("packageNative", Zip::class.java).get().apply {
                destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
                archiveFileName.set("application.zip")
                NativeDesktopPackaging.configureArchive(this, project.provider { distribution }, executablePath)
            }
            archive.destinationDirectory.get().asFile.mkdirs()
            archive.actions.forEach { it.execute(archive) }
            ZipFile(archive.archiveFile.get().asFile).use { zip ->
                assertContentEquals(executable.readBytes(), zip.getInputStream(zip.getEntry(executablePath)).readBytes())
                val runtimePath = runtime.relativeTo(distribution).invariantSeparatorsPath
                assertContentEquals(runtime.readBytes(), zip.getInputStream(zip.getEntry(runtimePath)).readBytes())
                assertEquals(2, zip.entries().asSequence().count { !it.isDirectory })
            }
            FileSystems.newFileSystem(archive.archiveFile.get().asFile.toPath(), emptyMap<String, Any>()).use { zip ->
                assertEquals(PosixFilePermissions.fromString("rwxr-xr-x"), Files.getAttribute(zip.getPath("/$executablePath"), "zip:permissions"))
                assertEquals(PosixFilePermissions.fromString("rw-r--r--"), Files.getAttribute(zip.getPath("/${runtime.relativeTo(distribution).invariantSeparatorsPath}"), "zip:permissions"))
            }
        }
    }

    @Test
    fun archiveRejectsAResourceOnlyDistribution() {
        val project = ProjectBuilder.builder().withProjectDir(temporary.newFolder()).build()
        val distribution = project.file("distribution").apply { mkdirs() }
        distribution.resolve("icudtl.dat").writeText("runtime fixture")
        val archive = project.tasks.register("packageNative", Zip::class.java).get().apply {
            destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
            archiveFileName.set("application.zip")
            NativeDesktopPackaging.configureArchive(this, project.provider { distribution }, "ModManager.app/Contents/MacOS/ModManager.kexe")
        }
        val error = assertFailsWith<IllegalArgumentException> { archive.actions.forEach { it.execute(archive) } }
        assertContains(error.message.orEmpty(), "Missing staged Native executable")
    }

    @Test
    fun macosBundleContainsExecutableMetadataAndEscapedAuthorFields() {
        val desktop = NativeDesktopDistribution().apply {
            enabled = true
            packageName = "NativeApp"
            displayName = "应用 & <Demo>"
            packageVersion = "1.2.3-beta01"
            author = "A & B"
            copyright = "Copyright <2026>"
        }
        val root = temporary.newFolder()
        val icon = temporary.newFile("app.icns").apply { writeBytes(byteArrayOf(0x69, 0x63, 0x6e, 0x73, 0, 0, 0, 8)) }
        desktop.macos.iconFile = icon
        val executable = root.resolve(NativeDesktopPackaging.executableDirectory(desktop, "macos_arm64")).resolve("app.kexe")
        executable.parentFile.mkdirs()
        executable.writeText("fixture")
        NativeDesktopPackaging.finish(root, executable, "macos_arm64", desktop)
        val plist = root.resolve("NativeApp.app/Contents/Info.plist").readText()
        assertContains(plist, "应用 &amp; &lt;Demo&gt;")
        assertContains(plist, "A &amp; B")
        assertContains(plist, "<key>CFBundleVersion</key><string>1.2.3</string>")
        assertContains(plist, "<key>NSHighResolutionCapable</key><true/>")
        assertContains(plist, "<key>CFBundleExecutable</key><string>app.kexe</string>")
        assertContains(plist, "<key>CFBundleIconFile</key><string>application-icon.icns</string>")
        assertContentEquals(icon.readBytes(), root.resolve("NativeApp.app/Contents/Resources/application-icon.icns").readBytes())
        assertFalse(root.resolve("NativeApp.app/Contents/Resources/application.json").exists())
    }

    @Test
    fun icoDirectoryRejectsImageRangesOutsideTheInput() {
        val bytes = ByteBuffer.allocate(23).order(ByteOrder.LITTLE_ENDIAN)
        bytes.putShort(0).putShort(1).putShort(1)
        bytes.put(byteArrayOf(1, 1, 0, 0)).putShort(1).putShort(32).putInt(1).putInt(22).put(42)
        val resources = NativeWindowsResources.iconResources(bytes.array())
        assertEquals(2, resources.size)
        assertContentEquals(byteArrayOf(42), resources.first().third)
        bytes.putInt(18, Int.MAX_VALUE)
        assertFailsWith<IllegalArgumentException> { NativeWindowsResources.iconResources(bytes.array()) }
    }

    @Test
    fun directoryNamesCannotEscapeTheDistributionAndVersionComponentsStayBounded() {
        for (name in listOf("../other", "C:\\other", "a/b", "a\u0000b", "a\tb", "é".repeat(126), "CON", "app.", "app ")) {
            assertFailsWith<IllegalArgumentException> { NativeDesktopDistribution().apply { packageName = name }.validate() }
        }
        assertFailsWith<IllegalArgumentException> { NativeWindowsResources.versionInfo(NativeDesktopDistribution().apply { packageVersion = "65536.0" }, "app.exe") }
    }
}
