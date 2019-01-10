package sp.it.pl.plugin.screenrotator

import javafx.stage.Screen
import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.plugin.PluginBase
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cr
import sp.it.pl.util.dev.fail
import sp.it.pl.util.graphics.getScreenForMouse
import sp.it.pl.util.system.Os
import sp.it.pl.util.system.open
import java.io.IOException

class ScreenRotator: PluginBase("Screen Rotator", true) {

    private val programFile = getResource(PROGRAM_FILE_NAME)
    private val programHelpFile = getResource(PROGRAM_HELP_FILE_NAME)
    private val actions by lazy {
        if (Os.WINDOWS.isCurrent)
            setOf(
                    action(null, Dir.CW, "CTRL+ALT+DOWN"),
                    action(null, Dir.CW, "CTRL+ALT+RIGHT"),
                    action(null, Dir.CCW, "CTRL+ALT+UP"),
                    action(null, Dir.CCW, "CTRL+ALT+LEFT"),
                    action(1, Dir.CW, "CTRL+ALT+1"),
                    action(2, Dir.CW, "CTRL+ALT+2"),
                    action(3, Dir.CW, "CTRL+ALT+3"),
                    action(4, Dir.CW, "CTRL+ALT+4"),
                    action(1, Dir.CCW, "CTRL+SHIFT+ALT+1"),
                    action(2, Dir.CCW, "CTRL+SHIFT+ALT+2"),
                    action(3, Dir.CCW, "CTRL+SHIFT+ALT+3"),
                    action(4, Dir.CCW, "CTRL+SHIFT+ALT+4"),
                    Action(
                            "Start screen saver",
                            { startScreenSaver() },
                            "Starts screen saver if enabled. It can be stopped by moving the mouse. On some system that can open logon screen.",
                            configurableGroup,
                            "CTRL+SHIFT+ALT+L",
                            true, false
                    ),
                    Action(
                            "Turn off screens",
                            { turnScreens(false) },
                            "Turns off all screens. They can be turned on again by moving the mouse.",
                            configurableGroup,
                            "CTRL+SHIFT+ALT+K",
                            true,
                            false
                    )
            )
        else
            setOf()
    }

    @IsConfig(name = "Open help", info = "Open technical usage help")
    private val openHelpDo by cr { openHelp() }

    override fun onStart() = APP.configuration.collect(actions)

    override fun onStop() = APP.configuration.drop(actions)

    fun openHelp() = programHelpFile.open()

    fun turnScreens(on: Boolean) {
        try {
            ProcessBuilder(programFile.path, if (on) "/power:on" else "/power:off").directory(userLocation).start()
        } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
        }
    }

    fun startScreenSaver() {
        try {
            ProcessBuilder(programFile.path, "/power:saver").directory(userLocation).start()
        } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
        }
    }

    fun rotateScreen(screen: Int? = null, direction: Dir) {
        val scr = screen ?: indexOfScreenOfMouse()
        try {
            ProcessBuilder(programFile.path, "/rotate:${direction.command}", "/device:$scr").directory(userLocation).start()
        } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
        }
    }

    private fun action(screen: Int?, direction: Dir, keys: String) = Action(
            "Rotate screen ${screen ?: "(active)"} ${direction.text}",
            { rotateScreen(screen, direction) },
            "Rotates screen ${screen ?: "(active)"} ${direction.text}",
            configurableGroup,
            keys,
            true,
            false
    )

    private fun indexOfScreenOfMouse(): Int {
        return if (Screen.getScreens().size==1) 1
        else try {
            val bounds = getScreenForMouse().bounds
            ProcessBuilder(programFile.path, "/listdevices", "/properties").directory(userLocation).start()
                    .inputStream.bufferedReader()
                    .useLines {
                        1+it.filter { it.contains("Display Area Size:") }
                                .indexOfFirst {
                                    val corners = it.substringAfter(": ").substringBefore(" (").split(" - ")
                                    val minX = corners[0].split(",")[0].toDouble()
                                    val minY = corners[0].split(",")[1].toDouble()
                                    val maxX = corners[1].split(",")[0].toDouble()
                                    val maxY = corners[1].split(",")[1].toDouble()
                                    bounds.minX==minX && bounds.minY==minY && bounds.maxX==maxX && bounds.maxY==maxY
                                }
                    }
        } catch (e: Throwable) {
            logger.error(e) { "Failed to list monitors" }
            fail { "Failed to list monitors" }
        }
    }

    enum class Dir(val command: String, val text: String) {
        CW("cw", "clockwise"),
        CCW("ccw", "counter-clockwise")
    }

    companion object: KLogging() {
        private const val PROGRAM_FILE_NAME = "display.exe"
        private const val PROGRAM_HELP_FILE_NAME = "display.htm"
    }

}