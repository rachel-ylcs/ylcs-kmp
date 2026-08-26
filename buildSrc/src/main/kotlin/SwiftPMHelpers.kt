import org.gradle.api.Project

fun Project.patchMMKVSwiftPackage() {
    tasks.named("fetchSyntheticImportProjectPackages") {
        doLast {
            val packageFile = layout.buildDirectory.file("kotlin/swiftPMCheckout/checkouts/MMKV/Package.swift").get().asFile
            if (!packageFile.isFile) return@doLast

            packageFile.setWritable(true, true)

            val originalBlock = """            linkerSettings: [
                .linkedLibrary("z"),
                .linkedLibrary("c++")
            ]"""
            val patchedBlock = """            linkerSettings: [
                .linkedLibrary("z"),
                .linkedLibrary("c++"),
                .linkedFramework("UIKit")
            ]"""

            val contents = packageFile.readText()
            if (contents.contains(originalBlock) && !contents.contains(".linkedFramework(\"UIKit\")")) {
                packageFile.writeText(contents.replace(originalBlock, patchedBlock))
            }
        }
    }
}