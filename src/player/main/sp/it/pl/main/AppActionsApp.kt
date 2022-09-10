package sp.it.pl.main

import sp.it.pl.ui.pane.action
import sp.it.util.action.ActionRegistrar

/** Denotes actions for [App] */
object AppActionsApp {

   val openLocation = action<App>(IconFA.FOLDER, ActionRegistrar["Open app directory"])

   val openHotkeysInfo = action<App>("Open hotkeys", "Display all available shortcuts", IconMD.KEYBOARD_VARIANT) { it.actions.showShortcuts() }

}