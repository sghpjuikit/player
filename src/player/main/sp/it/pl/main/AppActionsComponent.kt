package sp.it.pl.main

import javafx.stage.FileChooser.ExtensionFilter
import sp.it.pl.core.CoreMenuNoInspect
import sp.it.pl.layout.Component
import sp.it.pl.layout.Widget
import sp.it.pl.layout.exportFxwl
import sp.it.pl.layout.exportFxwlDefault
import sp.it.pl.layout.openInConfigured
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.action
import sp.it.util.async.runFX
import sp.it.util.functional.asIs
import sp.it.util.functional.orNull
import sp.it.util.system.saveFile

data class WidgetDefaultMenu(val widget: Widget): CoreMenuNoInspect

/** Denotes actions for [Component] */
object AppActionsComponent {

   val componentFocus = action<Component>("Focus", "Focus this component.", IconFA.CLONE) { it.focus() }

   val componentClone = action<Component>("Clone", "Creates new component with the same content and state as this one.", IconFA.CLONE) { it.openInConfigured() }

   val componentExport = action<Component>(
      "Export",
      "Creates a launcher for this component with its current settings.\nOpening the launcher with this application will open this component with current settings",
      IconMD.EXPORT,
      BLOCK
   ) { w ->
      val f = runFX { saveFile("Export to...", APP.location.user.layouts, w.name, window, ExtensionFilter("Component", "*.fxwl")).orNull() }.blockAndGetOrThrow() ?: return@action Unit
      w.exportFxwl(f).blockAndGetOrThrow() ?: Unit
   }

   val componentExportedSave = action<Component>(
      "Save",
      "Exports this component with its current settings to the launcher file it was loaded from.",
      IconMD.EXPORT,
      constriction = { w -> w.factoryDeserializing!=null }
   ) { w ->
      w.exportFxwl(w.factoryDeserializing!!.launcher).blockAndGetOrThrow() ?: Unit
   }

   val widgetExportDefault = action<Component>(
      "Export default",
      buildString {
         append("Creates a launcher for this component with no settings.\n")
         append("Opening the launcher with this application will open this component with no settings as if it were a standalone application.")
      },
      IconMD.EXPORT,
      BLOCK,
      constriction = { it is Widget },
   ) {w ->
      val f = runFX { saveFile("Export to...", APP.location.user.layouts, w.name, window, ExtensionFilter("Component", "*.fxwl")) }.blockAndGetOrThrow().orNull() ?: return@action Unit
      w.asIs<Widget>().exportFxwlDefault(f).blockAndGetOrThrow()
   }

   val widgetUseAsDefault = action<WidgetDefaultMenu>(
      "Use as default",
      buildString {
         append("Uses settings of this widget as default settings when creating widgets of this type. This ")
         append("overrides the default settings of the widget set by the developer. For using multiple widget ")
         append("configurations at once, use 'Export' instead.")
      },
      IconMD.SETTINGS_BOX,
      BLOCK
   ) {
      it.widget.storeDefaultConfigs()
   }

   val widgetClearDefault = action<WidgetDefaultMenu>(
      "Clear default",
      "Removes any overridden default settings for this widget type. New widgets will start with no settings.",
      IconMD.SETTINGS_BOX,
      BLOCK
   ) {
      it.widget.clearDefaultConfigs()
   }

}