package sp.it.pl.main

import com.sun.tools.attach.VirtualMachine
import sp.it.pl.layout.ComponentLoader.WINDOW
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.feature.TextDisplayFeature
import sp.it.pl.layout.orNone
import sp.it.pl.main.Widgets.ICON_BROWSER
import sp.it.pl.main.Widgets.INSPECTOR
import sp.it.pl.main.Widgets.TESTER
import sp.it.pl.ui.pane.action
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.launch
import sp.it.util.functional.toUnit
import sp.it.util.system.browse
import sp.it.util.units.uri

/** Denotes actions for 'App.Developer' */
object AppHelp

/** Denotes actions for [AppHelp] */
object AppActionsAppHelp {

   val init = action<App>("Help", "Set of actions for advanced users", IconOC.CIRCUIT_BOARD) { AppHelp }

   val openGithubPage = action<AppHelp>("Open Github page", "Opens Github page for this application.", IconFA.GITHUB) {
      APP.projectGithubUri.browse()
   }

   val openCssReferenceGuide = action<AppHelp>("Open css guide", "Opens skin css reference guide.", IconFA.CSS3) {
      uri("https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html").browse()
   }

   val openIconBrowser = action<AppHelp>("Open ${ICON_BROWSER.name}", "Browse available icons", IconFA.FONTICONS) {
      launch(FX) { WINDOW(APP.widgetManager.factories.getFactory(ICON_BROWSER.id).orNone().create()) }.toUnit()
   }

   val openUiInspector = action<AppHelp>("Open UI inspector", "Open widget for inspecting UI elements.", IconFA.EYEDROPPER) {
      launch(FX) { WINDOW(APP.widgetManager.factories.getFactory(INSPECTOR.id).orNone().create()) }.toUnit()
   }

   val openUiTester = action<AppHelp>("Open UI Tester", "Browse widget for testing UI functionality", IconFA.EYEDROPPER) {
      launch(FX) { WINDOW(APP.widgetManager.factories.getFactory(TESTER.id).orNone().create()) }.toUnit()
   }

   val openSystemProperties = action<AppHelp>("Show system properties", "Display system properties.", IconMD.INFORMATION_OUTLINE) {
      APP.ui.infoPane.orBuild.show(Unit)
   }

   val printJavaProcesses = action<AppHelp>("Print running java processes", "Print running java processes", IconMD.RESPONSIVE) {
      val text = VirtualMachine.list().joinToString("") { "\nVM:\n\tid: ${it.id()}\n\tdisplayName: ${it.displayName()}\n\tprovider: ${it.provider()}" }
      APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) }
   }

}