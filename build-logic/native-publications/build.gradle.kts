import groovy.json.JsonSlurper
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

group = "love.yinlin.native"
version = "${libs.versions.rachel.get()}-cmp-${libs.versions.cmp.get()}"

val repositoryDirectory = rootDir.parentFile
val nativeDirectory = repositoryDirectory.resolve("native-compose")
val repositoryPath = providers.gradleProperty("native.publishing.repository")
val inputRepository = repositoryPath.orNull?.let { repositoryDirectory.resolve(it).canonicalFile }
val compose = JsonSlurper().parse(nativeDirectory.resolve("upstream/compose.json")) as Map<*, *>
val skiko = JsonSlurper().parse(nativeDirectory.resolve("upstream/lock.json")) as Map<*, *>
val modules = compose["modules"] as Map<*, *>
val coordinates = listOf("skiko", "skiko-skottie").map { skiko["group"].toString() to it } + modules.map { (name, value) ->
    ((value as Map<*, *>)["group"] ?: compose["group"]).toString() to name.toString()
}
val emptyJavadoc = tasks.register<Jar>("nativeJavadocJar") {
    archiveClassifier.set("javadoc")
}

fun Element.appendTextElement(name: String, value: String): Element = ownerDocument.createElement(name).also {
    it.textContent = value
    appendChild(it)
}

if (inputRepository != null) {
    require(inputRepository.isDirectory) { "Missing assembled Native Maven repository: $inputRepository" }
    val factory = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        isXIncludeAware = false
    }
    for ((groupName, rootArtifact) in coordinates) {
        require(groupName.startsWith("love.yinlin.native."))
        for (suffix in listOf("", "-mingwx64", "-linuxx64", "-macosarm64")) {
            val artifact = rootArtifact + suffix
            val directory = inputRepository.resolve("${groupName.replace('.', '/')}/$artifact/$version")
            val prefix = "$artifact-$version"
            val pomFile = directory.resolve("$prefix.pom")
            require(pomFile.isFile) { "Missing Native publication: $pomFile" }
            val original = factory.newDocumentBuilder().parse(pomFile).documentElement
            for ((field, expected) in mapOf("groupId" to groupName, "artifactId" to artifact, "version" to version.toString())) {
                val actual = (0 ..< original.childNodes.length).map { original.childNodes.item(it) }.single { it.nodeName == field }.textContent.trim()
                require(actual == expected) { "Wrong $field in $pomFile: $actual" }
            }
            val metadata = directory.resolve("$prefix.module")
            require(metadata.isFile) { "Missing Native Gradle metadata: $metadata" }
            val files = directory.listFiles().orEmpty().filter {
                it.isFile && it.name.startsWith(prefix) && it.extension in setOf("jar", "klib", "module", "json", "dat")
            }
            require(files.any { it.name == "$prefix-sources.jar" }) { "Missing Native sources: $directory" }
            val publicationName = "native${coordinates.indexOf(groupName to rootArtifact)}${suffix.replace("-", "")}"
            extensions.getByType<PublishingExtension>().publications.create<MavenPublication>(publicationName) {
                groupId = groupName
                artifactId = artifact
                version = project.version.toString()
                for (file in files) artifact(file) {
                    extension = file.extension
                    classifier = file.name.removePrefix(prefix).removeSuffix(".${file.extension}").removePrefix("-").ifEmpty { null }
                }
                if (files.none { it.name == "$prefix-javadoc.jar" }) artifact(emptyJavadoc)
                pom.withXml {
                    val destination = asElement()
                    while (destination.hasChildNodes()) destination.removeChild(destination.firstChild)
                    for (index in 0 ..< original.childNodes.length) destination.appendChild(destination.ownerDocument.importNode(original.childNodes.item(index), true))
                    if (destination.getElementsByTagName("developers").length == 0) {
                        val developers = destination.ownerDocument.createElement("developers").also { destination.appendChild(it) }
                        val developer = destination.ownerDocument.createElement("developer").also { developers.appendChild(it) }
                        developer.appendTextElement("id", "rachel-ylcs")
                        developer.appendTextElement("name", "Rachel contributors")
                        developer.appendTextElement("url", "https://github.com/rachel-ylcs")
                    }
                    val scm = (destination.getElementsByTagName("scm").item(0) as? Element)
                        ?: destination.ownerDocument.createElement("scm").also { destination.appendChild(it) }
                    while (scm.hasChildNodes()) scm.removeChild(scm.firstChild)
                    scm.appendTextElement("url", "https://github.com/rachel-ylcs/ylcs-kmp")
                    scm.appendTextElement("connection", "scm:git:https://github.com/rachel-ylcs/ylcs-kmp.git")
                    scm.appendTextElement("developerConnection", "scm:git:ssh://git@github.com/rachel-ylcs/ylcs-kmp.git")
                }
            }
            tasks.named<GenerateMavenPom>("generatePomFileFor${publicationName.replaceFirstChar(Char::uppercase)}Publication") {
                inputs.file(pomFile)
            }
        }
    }
}

extensions.configure<PublishingExtension> {
    repositories.maven {
        name = "ReleaseVerification"
        url = uri(layout.buildDirectory.dir("repository"))
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    doFirst { require(inputRepository != null) { "Set -Pnative.publishing.repository to the assembled three-target Maven repository" } }
}

tasks.named("publishToMavenCentral") {
    doFirst { require(inputRepository != null) { "Native publishing requires all three host repositories" } }
}
