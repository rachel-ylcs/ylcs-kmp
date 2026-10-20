package love.yinlin.repository.tip

import love.yinlin.repository.C
import org.gradle.api.Project

fun buildLyrics(): List<String> {
    val lyrics = library.random().split("\n")
    return lyrics.map { str ->
        var newStr = if (str.length % 2 != 0) "$str\uFF5E" else str
        newStr = newStr.padStart(newStr.length + (20 - newStr.length) / 2, '\u3000').padEnd(20, '\u3000')
        "║$newStr║".padStart(43)
    }
}

@Suppress("unused")
fun Project.showtime() {
    val currentTaskName = this.gradle.startParameter.taskNames.firstOrNull() ?: "sync"
    val s0 = System.getProperty("user.dir")
    val s1 = currentTaskName.split(":").last().padStart(22).take(22)
    val s2 = C.platform.toString().padStart(22).take(22)
    val s3 = System.getProperty("os.version").padStart(22).take(22)
    val s4 = C.architecture.toString().padStart(22).take(22)
    val s5 = C.namespace.padStart(22).take(22)
    val s6 = gradle.gradleVersion.padStart(22).take(22)
    val s7 = System.getProperty("java.version").padStart(22).take(22)
    val runtime = Runtime.getRuntime()
    val s8 = "${"%.1f".format(runtime.freeMemory() / 1073741824.0)}G/${"%.1f".format(runtime.totalMemory() / 1073741824.0)}G/${"%.1f".format(runtime.maxMemory() / 1073741824.0)}G".padStart(22).take(22)

    val (l1, l2, l3, l4) = buildLyrics()

    val appV = C.versionName

    val art = """
                 ╔════════════════════════════════════════════╗
                 ║                银临茶舍$appV                ║
╔════════════════╩════════════════════════════════════════════╩════════════════╗
║         ________  ________  ________  ___  ___  _______   ___                ║
║        |\   __  \|\   __  \|\   ____\|\  \|\  \|\  ___ \ |\  \               ║
║        \ \  \|\  \ \  \|\  \ \  \___|\ \  \\\  \ \   __/|\ \  \              ║
║         \ \   _  _\ \   __  \ \  \    \ \   __  \ \  \_|/_\ \  \             ║
║          \ \  \\  \\ \  \ \  \ \  \____\ \  \ \  \ \  \_|\ \ \  \____        ║
║           \ \__\\ _\\ \__\ \__\ \_______\ \__\ \__\ \_______\ \_______\      ║
║            \|__|\|__|\|__|\|__|\|_______|\|__|\|__|\|_______|\|_______|      ║
╚══════════════════════════════════════════════════════════════════════════════╝
$l1
$l2
$l3
$l4
╔════════════════╦════════════════════════════╦════════════════════════════════╗
║      Task      ║   $s1   ║ @@@@@@@@@@%%%%%%%%+*@@@@@@@@@@ ║
╠════════════════╬════════════════════════════╣ @@@@@@@.=%*****%***%#@@@@@@@@@ ║
║    Platform    ║   $s2   ║ @@@@@#=#+#********##.==@@@@@@@ ║
╠════════════════╬════════════════════════════╣ @@@@+-.#==##########==..@@@@@@ ║
║     Version    ║   $s3   ║ @@@++.#######%%######*+@@@@@@@ ║
╠════════════════╬════════════════════════════╣ @@@@@.+=%+.+:...%=...@:#@@@@@@ ║
║  Architecture  ║   $s4   ║ @@@@@::..+.=:-..=..%=#*:@@@@@@ ║
╠════════════════╬════════════════════════════╣ @@@@@:@%=%==++*..+%===++=@@@@@ ║
║    Namespace   ║   $s5   ║ @@@#::@#####=..++.-=*==::@@+@@ ║
╠════════════════╬════════════════════════════║ @@@#::@#####......====:-:@@@@@ ║
║     Gradle     ║   $s6   ║ @@@@:=@#####.....=%-==**:@@@@@ ║
╠════════════════╬════════════════════════════║ @@@@@::##@##....+--%..*+:@@@@@ ║
║      Java      ║   $s7   ║ @@@@@::@@@@%..:=----:.*::@@@@@ ║
╠════════════════╬════════════════════════════║ @@@@@@@@@@@-:---------%#@@@@@@ ║
║     Memory     ║   $s8   ║ @@@@@@@@@@@***+-*****@@@@@@@@@ ║
╠════════════════╬════════════════════════════╩════════════════════════════════╣
║      Path      ║   $s0
╚════════════════╩═════════════════════════════════════════════════════════════╝
""".trimIndent()

    println(art)
}
