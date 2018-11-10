package sp.it.pl.plugin

import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.util.conf.cr
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.graphics.getScreenForMouse
import sp.it.pl.util.graphics.ordinal
import sp.it.pl.util.system.open
import java.io.IOException

class ScreenRotator: PluginBase("Screen Rotator", true) {

    private val programFile = getResource(PROGRAM_FILE_NAME)
    private val programHelpFile = getResource(PROGRAM_HELP_FILE_NAME)
    private val actions by lazy {
        setOf(
                Action("Rotate screen (active) cw", { rotateScreen(null, "cw") }, "Rotates screen (active) clockwise", configurableGroup, "CTRL+ALT+DOWN", true, false),
                Action("Rotate screen (active) cw", { rotateScreen(null, "cw") }, "Rotates screen (active) clockwise", configurableGroup, "CTRL+ALT+RIGHT", true, false),
                Action("Rotate screen (active) ccw", { rotateScreen(null, "ccw") }, "Rotates screen (active) counter-clockwise", configurableGroup, "CTRL+ALT+UP", true, false),
                Action("Rotate screen (active) ccw", { rotateScreen(null, "ccw") }, "Rotates screen (active) counter-clockwise", configurableGroup, "CTRL+ALT+LEFT", true, false),
                Action("Rotate screen 1 cw", { rotateScreen(1, "cw") }, "Rotates screen 1 clockwise", configurableGroup, "CTRL+ALT+1", true, false),
                Action("Rotate screen 2 cw", { rotateScreen(2, "cw") }, "Rotates screen 2 clockwise", configurableGroup, "CTRL+ALT+2", true, false),
                Action("Rotate screen 3 cw", { rotateScreen(3, "cw") }, "Rotates screen 3 clockwise", configurableGroup, "CTRL+ALT+3", true, false),
                Action("Rotate screen 4 cw", { rotateScreen(4, "cw") }, "Rotates screen 4 clockwise", configurableGroup, "CTRL+ALT+4", true, false),
                Action("Rotate screen 1 ccw", { rotateScreen(1, "ccw") }, "Rotates screen 1 counter-clockwise", configurableGroup, "CTRL+SHIFT+ALT+1", true, false),
                Action("Rotate screen 2 ccw", { rotateScreen(2, "ccw") }, "Rotates screen 2 counter-clockwise", configurableGroup, "CTRL+SHIFT+ALT+2", true, false),
                Action("Rotate screen 3 ccw", { rotateScreen(3, "ccw") }, "Rotates screen 3 counter-clockwise", configurableGroup, "CTRL+SHIFT+ALT+3", true, false),
                Action("Rotate screen 4 ccw", { rotateScreen(4, "ccw") }, "Rotates screen 4 counter-clockwise", configurableGroup, "CTRL+SHIFT+ALT+4", true, false)
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

    companion object: KLogging() {
        private const val PROGRAM_FILE_NAME = "display.exe"
        private const val PROGRAM_HELP_FILE_NAME = "display.htm"
    }

}