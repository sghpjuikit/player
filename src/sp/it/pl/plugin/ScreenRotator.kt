package sp.it.pl.plugin

import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cr
import sp.it.pl.util.graphics.getScreenForMouse
import sp.it.pl.util.graphics.ordinal
import sp.it.pl.util.system.open
import java.io.IOException

class ScreenRotator: PluginBase("Screen Rotator", true) {

    private val programFile = getResource(PROGRAM_FILE_NAME)
    private val programHelpFile = getResource(PROGRAM_HELP_FILE_NAME)
    private val actions by lazy {
        setOf(
                action(null, "CTRL+ALT+DOWN"),
                action(null, "CTRL+ALT+RIGHT"),
                action(null, "CTRL+ALT+UP"),
                action(null, "CTRL+ALT+LEFT"),
                action(1, "CTRL+ALT+1"),
                action(2, "CTRL+ALT+2"),
                action(3, "CTRL+ALT+3"),
                action(4, "CTRL+ALT+4"),
                action(1, "CTRL+SHIFT+ALT+1"),
                action(2, "CTRL+SHIFT+ALT+2"),
                action(3, "CTRL+SHIFT+ALT+3"),
                action(4, "CTRL+SHIFT+ALT+4")
        )
    }

    @IsConfig(name = "Open help", info = "Open technical usage help")
    private val openHelpDo by cr { openHelp() }

    override fun onStart() = APP.configuration.collect(actions)

    override fun onStop() = APP.configuration.drop(actions)

    fun rotateScreen(screen: Int? = null, rotation: String) {
        val scr = screen ?: getScreenForMouse().ordinal
        try {
            ProcessBuilder(programFile.path, "/rotate:$rotation", "/device:$scr").directory(userLocation).start()
        } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
        }
    }

    fun openHelp() = programHelpFile.open()

    private fun action(screen: Int?, keys: String) = Action(
            "Rotate screen ${screen ?: "(active)"} cw",
            { rotateScreen(screen, "cw") },
            "Rotates screen ${screen ?: "(active)"} clockwise",
            configurableGroup,
            keys,
            true,
            false
    )

    companion object: KLogging() {
        private const val PROGRAM_FILE_NAME = "display.exe"
        private const val PROGRAM_HELP_FILE_NAME = "display.htm"
    }

}