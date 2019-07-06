package terminal

import com.kodedu.terminalfx.TerminalBuilder
import com.kodedu.terminalfx.TerminalTab
import com.kodedu.terminalfx.config.TerminalConfig
import javafx.scene.control.TabPane
import javafx.scene.control.TabPane.TabClosingPolicy
import javafx.scene.input.KeyCode.T
import javafx.scene.input.KeyCode.W
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.paint.Color.rgb
import sp.it.pl.gui.objects.placeholder.Placeholder
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.scaleEM
import sp.it.util.conf.IsConfig
import sp.it.util.conf.cvn
import sp.it.util.conf.only
import sp.it.util.file.div
import sp.it.util.reactive.SHORTCUT
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.syncSize
import sp.it.util.system.Os
import sp.it.util.text.keys
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.validation.Constraint.FileActor.FILE
import java.io.File

@Widget.Info(
   author = "Martin Polakovic",
   name = "Terminal",
   description = "Terminal for working with shell.",
   version = "1.0.0",
   year = "2015",
   group = Widget.Group.DEVELOPMENT
)
class Terminal(widget: Widget): SimpleController(widget) {

   private val tConfig = TerminalConfig()
   private val tBuilder = TerminalBuilder(tConfig)
   private val tabPane = TabPane()
   private val placeholder = Placeholder(IconFA.TERMINAL, "New terminal (${keys("CTRL+T")})") { openNewTab() }

   @IsConfig(name = "Shell path", info = "Path to the shell or none for default")
   val shellPath by cvn(null as File?).only(FILE) sync {
      closeAllTabs()
      when (Os.current) {
         Os.WINDOWS -> tConfig.windowsTerminalStarter = it?.absolutePath ?: "cmd.exe"
         Os.UNIX -> tConfig.unixTerminalStarter = it?.absolutePath ?: "/bin/bash -i"
         else -> Unit
      }
   }

   init {
      root.prefSize = 600.scaleEM() x 500.scaleEM()
      root.lay += tabPane

      com.kodedu.terminalfx.helper.ThreadHelper.daemonThread = true
      tConfig.setBackgroundColor(rgb(16, 16, 16))
      tConfig.setForegroundColor(rgb(240, 240, 240))
      tConfig.setCursorColor(rgb(255, 0, 0, 0.1))
      tConfig.isScrollbarVisible = false
      tConfig.webViewUserDataDirectory = APP.locationTmp/".terminalFx"/"webView"
      tabPane.tabClosingPolicy = TabClosingPolicy.ALL_TABS

      root.onEventUp(KEY_PRESSED, SHORTCUT, W) { closeActiveTab() }
      root.onEventUp(KEY_PRESSED, SHORTCUT, T) { openNewTab() }

      tabPane.tabs syncSize { tabPane.isVisible = it!=0 } on onClose
      tabPane.tabs syncSize { placeholder.show(root, it==0) } on onClose

      onClose += { closeAllTabs() }
   }

   override fun focus() {
      if (placeholder.isVisible) placeholder.requestFocus()
      else super.focus()
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

}