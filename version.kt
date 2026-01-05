
import com.github.ajalt.clikt.core.CliktCommand

object Version {
    const val VALUE = "__VERSION__"
}

class VersionCommand: CliktCommand(name = "version") {
    override fun run() {
        echo("${Version.VALUE}")
    }
}
