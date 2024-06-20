package sp.it.pl.main

import javafx.scene.input.Clipboard
import sp.it.pl.layout.WidgetUse
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.feature.TextDisplayFeature
import sp.it.pl.ui.pane.ActionData
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.action
import sp.it.util.action.IsAction
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.ConstrainedRead
import sp.it.util.conf.Constraint.RepeatableAction
import sp.it.util.conf.butElement
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.nonEmpty

/** Denotes actions for [Unit] */
object AppActionsUnit {

   val detectContentFromClipBoard = action<Unit>("Detect clipboard content", "Identifies the type of the clipboard content and shows appropriate ui for it", IconMD.MAGNIFY) {
      Clipboard.getSystemClipboard().getAny().detectContent()
   }

   val runCommand = action<Unit>("Run cli command", "Runs command just like in a system's shell's command line in working directory of this process.", IconMD.CONSOLE) {
      object: ConfigurableBase<Any?>(), ConstrainedRead<Any?> {
         val command by cv("SpitPlayer --help").nonEmpty()
         val showOutput by cv(false)
         override val constraints = setOf(RepeatableAction)
      }.configure("Run cli command") { c ->
         runCommandWithOutput(c.command.value).ui { if (c.showOutput.value) apOrApp.show(it) }
      }
   }

   val runAppCommand = action<Unit>("Run arg command", "Equivalent of launching this application with the command as a parameter. See cli options for details.", IconMD.CONSOLE) {
      configureString("Run arg command", "command") {
         APP.parameterProcessor.process(listOf(it))
      }
   }

}