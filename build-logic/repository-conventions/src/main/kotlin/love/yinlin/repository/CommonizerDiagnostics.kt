package love.yinlin.repository

import org.gradle.api.IsolatedAction
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import java.io.File

@Suppress("UnstableApiUsage")
internal class ConfigureCommonizerDiagnostics : IsolatedAction<Project> {
    override fun execute(target: Project) {
        val host = System.getProperty("os.name")
        if (!host.startsWith("Windows", ignoreCase = true) && !host.equals("Linux", ignoreCase = true)) return
        val agentJar = File(CommonizerWarningFilter::class.java.protectionDomain.codeSource.location.toURI())
        if (!agentJar.isFile) return
        target.afterEvaluate {
            // KGP registers the task and sets its JVM arguments from an afterEvaluate listener.
            if ("commonizeCInterop" !in tasks.names) return@afterEvaluate
            tasks.named("commonizeCInterop").configure {
                val getter = javaClass.methods.firstOrNull { it.name == "getCustomJvmArgs\$kotlin_gradle_plugin_common" } ?: return@configure
                @Suppress("UNCHECKED_CAST")
                val args = getter.invoke(this) as ListProperty<String>
                args.add("-javaagent:${agentJar.absolutePath}")
            }
        }
    }
}
