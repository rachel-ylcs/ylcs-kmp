import java.net.URI
import java.util.Properties

val wrapper = checkNotNull(buildscript.sourceFile).resolveSibling("gradle-wrapper.properties")
val properties = Properties().apply { wrapper.inputStream().use(::load) }
val distributionUrl = checkNotNull(properties.getProperty("distributionUrl")) { "Missing distributionUrl in $wrapper." }
val archive = URI(distributionUrl).path.orEmpty().substringAfterLast('/')
val expectedGradle = checkNotNull(Regex("""gradle-(.+)-(?:bin|all)\.zip""").matchEntire(archive)?.groupValues?.get(1)) {
    "Cannot read the Gradle version from distributionUrl in $wrapper."
}
check(gradle.gradleVersion == expectedGradle) {
    "Use the repository Gradle Wrapper. The Wrapper requires Gradle $expectedGradle, running ${gradle.gradleVersion}."
}
