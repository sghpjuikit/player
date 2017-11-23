package sp.it.pl.plugin

import mu.KotlinLogging
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.Config.RunnableConfig
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.graphics.getScreenForMouse
import sp.it.pl.util.graphics.ordinal
import sp.it.pl.util.system.open
import java.io.IOException

private const val NAME = "Screen Rotator"
private const val GROUP = "${Plugin.CONFIG_GROUP}.$NAME"
private const val PROGRAM_FILE_NAME = "display.exe"
private const val PROGRAM_HELP_FILE_NAME = "display.htm"
private val logger = KotlinLogging.logger {}

class ScreenRotator: PluginBase(NAME) {

    private val programFile = getResource(PROGRAM_FILE_NAME)
    private val programHelpFile = getResource(PROGRAM_HELP_FILE_NAME)
    private val actions by lazy {
        setOf(
                Action("Rotate screen (active) cw", { rotateScreen(null, "cw") }, "Rotates screen (active) clockwise", "", "CTRL+ALT+DOWN", true, false),
                Action("Rotate screen (active) cw", { rotateScreen(null, "cw") }, "Rotates screen (active) clockwise", "", "CTRL+ALT+RIGHT", true, false),
                Action("Rotate screen (active) ccw", { rotateScreen(null, "ccw") }, "Rotates screen (active) counter-clockwise", "", "CTRL+ALT+UP", true, false),
                Action("Rotate screen (active) ccw", { rotateScreen(null, "ccw") }, "Rotates screen (active) counter-clockwise", "", "CTRL+ALT+LEFT", true, false),
                Action("Rotate screen 1 cw", { rotateScreen(1, "cw") }, "Rotates screen 1 clockwise", "", "CTRL+ALT+1", true, false),
                Action("Rotate screen 2 cw", { rotateScreen(2, "cw") }, "Rotates screen 2 clockwise", "", "CTRL+ALT+2", true, false),
                Action("Rotate screen 3 cw", { rotateScreen(3, "cw") }, "Rotates screen 3 clockwise", "", "CTRL+ALT+3", true, false),
                Action("Rotate screen 4 cw", { rotateScreen(4, "cw") }, "Rotates screen 4 clockwise", "", "CTRL+ALT+4", true, false),
                Action("Rotate screen 1 ccw", { rotateScreen(1, "ccw") }, "Rotates screen 1 counter-clockwise", "", "CTRL+SHIFT+ALT+1", true, false),
                Action("Rotate screen 2 ccw", { rotateScreen(2, "ccw") }, "Rotates screen 2 counter-clockwise", "", "CTRL+SHIFT+ALT+2", true, false),
                Action("Rotate screen 3 ccw", { rotateScreen(3, "ccw") }, "Rotates screen 3 counter-clockwise", "", "CTRL+SHIFT+ALT+3", true, false),
                Action("Rotate screen 4 ccw", { rotateScreen(4, "ccw") }, "Rotates screen 4 counter-clockwise", "", "CTRL+SHIFT+ALT+4", true, false)
        )
    }

    @Suppress("unused")
    @IsConfig(name = "Open help", group = GROUP)
    private val openHelpDo = RunnableConfig("show_help", "Open technical usage help", GROUP, "", { openHelp() })

    override fun onStart() {
        Action.getActions() += actions
        actions.forEach { it.register() }
    }

    override fun onStop() {
        actions.forEach { it.unregister() }
        Action.getActions() -= actions
    }

    fun rotateScreen(screen: Int? = null, rotation: String) {
        val scr = screen ?: getScreenForMouse().ordinal
        try {
            ProcessBuilder(programFile.path, "/rotate:$rotation", "/device:$scr").directory(userLocation).start()
        } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
        }
    }

    fun openHelp() = programHelpFile.open()

}