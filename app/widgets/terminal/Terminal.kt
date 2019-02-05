package terminal

import com.terminalfx.TerminalBuilder
import com.terminalfx.TerminalTab
import com.terminalfx.config.TerminalConfig
import javafx.scene.control.TabPane
import javafx.scene.control.TabPane.TabClosingPolicy
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.KeyEvent.KEY_TYPED
import javafx.scene.paint.Color.rgb
import sp.it.pl.gui.objects.placeholder.Placeholder
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.IconFA
import sp.it.pl.util.access.initSync
import sp.it.pl.util.access.vn
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cvn
import sp.it.pl.util.conf.only
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.onEventUp
import sp.it.pl.util.reactive.syncSize
import sp.it.pl.util.system.Os
import sp.it.pl.util.text.keys
import sp.it.pl.util.validation.Constraint.FileActor.FILE
import java.io.File

@Widget.Info(
        author = "Martin Polakovic",
        name = "Terminal",
        description = "Terminal for working with shell.",
        version = "0.5",
        year = "2015",
        group = Widget.Group.DEVELOPMENT
)
class Terminal(widget: Widget<*>): SimpleController(widget) {

    private val tConfig = TerminalConfig()
    private val tBuilder = TerminalBuilder(tConfig)
    private val tabPane = TabPane()
    private val placeholder = Placeholder(IconFA.TERMINAL, "New terminal (${keys("CTRL+T")})", { openNewTab() })

    @IsConfig(name = "Shell path", info = "Path to the shell or none for default")
    val shellPath by cvn(null as File?) {
        vn(it).initSync {
            closeAllTabs()
            when (Os.current) {
                Os.WINDOWS -> tConfig.windowsTerminalStarter = it?.absolutePath ?: "cmd.exe"
                Os.UNIX -> tConfig.unixTerminalStarter = it?.absolutePath ?: "/bin/bash -i"
                else -> {}
            }
        }
    }.only(FILE)

    init {
        tConfig.setBackgroundColor(rgb(16, 16, 16))
        tConfig.setForegroundColor(rgb(240, 240, 240))
        tConfig.setCursorColor(rgb(255, 0, 0, 0.1))
        tConfig.isScrollbarVisible = false
        tabPane.tabClosingPolicy = TabClosingPolicy.ALL_TABS
        onEventUp(KEY_PRESSED) { handleKey(it) }
        onEventUp(KEY_RELEASED) { handleKey(it) }
        onEventUp(KEY_TYPED) { handleKey(it) }

        layFullArea += tabPane

        tabPane.tabs syncSize { tabPane.isVisible = it!=0 } on onClose
        tabPane.tabs syncSize { placeholder.show(this, it==0) } on onClose

        onClose += { closeAllTabs() }
    }

    fun openNewTab() {
        tabPane.tabs += TerminalTab(tConfig, tBuilder.nameGenerator, tBuilder.terminalPath)
        tabPane.selectionModel.selectLast()
        focusActiveTab()
    }

    fun closeActiveTab() {
        tabPane.tabs.asSequence().filterIsInstance<TerminalTab>().find { it.isSelected }?.apply {
            closeTerminal()
            focusActiveTab()
        }
    }

    fun closeAllTabs() {
        tabPane.tabs.asSequence().filterIsInstance<TerminalTab>().firstOrNull()?.closeAllTerminal()
        tabPane.tabs.clear()
    }

    private fun focusActiveTab() = tabPane.selectionModel.selectedItem?.content?.requestFocus()

    private fun handleKey(e: KeyEvent) {
        if (e.eventType==KEY_PRESSED && e.isShortcutDown) {
            if (e.code==KeyCode.T) {
                openNewTab()
                e.consume()
            }
            if (e.code==KeyCode.W) {
                closeActiveTab()
                e.consume()
            }
        }
    }

}