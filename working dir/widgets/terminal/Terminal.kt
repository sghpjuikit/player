package terminal

import com.terminalfx.TerminalBuilder
import com.terminalfx.TerminalTab
import com.terminalfx.config.TerminalConfig
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.scene.control.TabPane
import javafx.scene.control.TabPane.TabClosingPolicy
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.paint.Color
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.ClassController
import sp.it.pl.util.access.v
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.graphics.Util.setAnchors
import sp.it.pl.util.graphics.drag.Placeholder
import sp.it.pl.util.graphics.setAnchors
import sp.it.pl.util.reactive.syncSize
import sp.it.pl.util.system.Os
import sp.it.pl.util.text.keys
import sp.it.pl.util.validation.Constraint.FileActor.FILE
import sp.it.pl.util.validation.Constraint.FileType
import java.io.File

@Widget.Info(
        author = "Martin Polakovic",
        name = "Terminal",
        description = "Terminal for working with shell.",
        version = "0.5",
        year = "2015",
        group = Widget.Group.DEVELOPMENT
)
class Terminal: ClassController() {

    private val tConfig = TerminalConfig()
    private val tBuilder = TerminalBuilder(tConfig)
    private val tabPane = TabPane()
    private val placeholder = Placeholder(FontAwesomeIcon.TERMINAL, "New terminal (${keys("CTRL+T")})", { openNewTab() })

    @FileType(FILE)
    @field: IsConfig(name = "Shell path", info = "Path to the shell or none for default")
    val shellPath = v<File?>(null) {
        closeAllTabs()
        when (Os.current) {
            Os.WINDOWS -> tConfig.windowsTerminalStarter = it?.absolutePath ?: "cmd.exe"
            Os.UNIX -> tConfig.unixTerminalStarter = it?.absolutePath ?: "/bin/bash -i"
            else -> {}
        }
    }

    init {
        tConfig.setBackgroundColor(Color.rgb(16, 16, 16))
        tConfig.setForegroundColor(Color.rgb(240, 240, 240))
        tConfig.setCursorColor(Color.rgb(255, 0, 0, 0.1))
        tConfig.isScrollbarVisible = false
        tabPane.tabClosingPolicy = TabClosingPolicy.ALL_TABS
        addEventFilter(KeyEvent.KEY_PRESSED) { handleKey(it) }
        addEventFilter(KeyEvent.KEY_RELEASED) { handleKey(it) }
        addEventFilter(KeyEvent.KEY_TYPED) { handleKey(it) }

        children += tabPane
        tabPane.setAnchors(0.0)
        setAnchors(tabPane, 0.0)

        onClose += tabPane.tabs syncSize {
            tabPane.isVisible = it!=0
            placeholder.show(this, it==0)
        }
        onClose += { closeAllTabs() }
    }

    override fun refresh() {
        shellPath.applyValue()
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
        if (e.eventType==KeyEvent.KEY_PRESSED && e.isShortcutDown) {
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