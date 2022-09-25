package sp.it.pl.main

import sp.it.pl.layout.WidgetUse
import sp.it.pl.layout.controller.Controller
import sp.it.pl.layout.feature.ConfiguringFeature
import sp.it.pl.ui.pane.action
import sp.it.util.functional.asIf
import sp.it.util.system.open

/** Denotes actions for [App] */
object AppActionsApp {

   val openLocation = action<App>("Open app directory", "Opens directory from which this application is running from", IconFA.GEARS) { APP.location.open() }

   val openHotkeysInfo = action<App>("Open hotkeys", "Display all available shortcuts", IconMD.KEYBOARD_VARIANT) { it.actions.showShortcuts() }

   val openSettings = action<App>("Open settings", "Opens application settings", IconFA.GEARS) { openSettings(null) }

   val openEventLog = action<App>("Open app event log", "Opens application settings", IconUN(0x1f4c1)) { APP.actionsLog.showDetailForLast() }

   fun openSettings(groupToSelect: String?) {
      APP.widgetManager.widgets.use<ConfiguringFeature>(WidgetUse.NEW) {
         it.asIf<Controller>()?.widget?.forbidUse?.value = true
         it.configure(APP.configuration, groupToSelect)
      }
   }

}