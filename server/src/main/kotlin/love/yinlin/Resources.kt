package love.yinlin

import java.io.File
import java.net.JarURLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption

open class Res : File {
	constructor(path: String) : super(path)
	constructor(parent: File, name: String) : super(parent, name)
	fun readResolve(): Any = this
}

object Resources {
	private const val PUBLIC_DIR_NAME = "public"

	object Public : Res(PUBLIC_DIR_NAME) {
		object Activity : Res(this, "activity") {
			fun activity(uniqueId: String) = Res(this, "${uniqueId}.webp")
		}
		object Assets : Res(this, "assets") {
			val DefaultAvatar = Res(this, "default_avatar.webp")
			val DefaultWall = Res(this, "default_wall.webp")
		}
		object Users : Res(this, "users") {
			class User(uid: Int) : Res(this, "$uid") {
				val avatar = Res(this, "avatar.webp")
				val wall = Res(this, "wall.webp")
				inner class Pics : Res(this, "pics") {
					fun pic(uniqueId: String) = Res(this, "${uniqueId}.webp")
				}
			}
		}
		val Photo = Res(this, "photo.json")
		val Server = Res(this, "server.json")
	}

	private fun copyResourcesFromFile(source: File, target: File) {
		if (!target.exists()) target.mkdirs()
		source.listFiles()?.forEach { file ->
			val destFile = File(target, file.name)
			if (file.isDirectory) copyResourcesFromFile(file, destFile)
			else Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
		}
	}

	fun copyResources() {
		if (Public.exists()) return
		val classLoader = Resources::class.java.classLoader
		val publicResourceUrl = classLoader.getResource(PUBLIC_DIR_NAME)!!
		if (publicResourceUrl.protocol == "jar") {
			Public.mkdirs()
			val jarFile = (publicResourceUrl.openConnection() as JarURLConnection).jarFile
			val entries = jarFile.entries()
			while (entries.hasMoreElements()) {
				val entry = entries.nextElement()
				if (entry.name.startsWith(PUBLIC_DIR_NAME) && !entry.isDirectory) {
					val targetFile = File(Public, entry.name.substring(PUBLIC_DIR_NAME.length + 1))
					targetFile.parentFile?.mkdirs()
					jarFile.getInputStream(entry).use { input ->
						Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
					}
				}
			}
		}
		else copyResourcesFromFile(File(publicResourceUrl.toURI()), Public)
	}
}