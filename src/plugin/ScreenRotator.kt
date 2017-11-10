package plugin

import mu.KotlinLogging
import util.action.Action
import util.conf.Config.RunnableConfig
import util.conf.IsConfig
import util.system.Environment
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
    private val openHelpDo = RunnableConfig("showhelp", "Open technical usage help", GROUP, "", { openHelp() })

    override fun onStart() {
        Action.getActions() += actions
        actions.forEach { it.register() }
    }

    override fun onStop() {
        actions.forEach { it.unregister() }
        Action.getActions() -= actions
    }

    fun rotateScreen(screen: Int, rotation: String) {
        try {
            ProcessBuilder(programFile.path, "/rotate:$rotation", "/device:$screen").directory(userLocation).start()
        } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
        }
    }

    fun openHelp() = Environment.open(programHelpFile)

}