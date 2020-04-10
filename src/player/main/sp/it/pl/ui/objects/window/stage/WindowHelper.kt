package sp.it.pl.ui.objects.window.stage

import sp.it.pl.layout.container.Layout
import sp.it.util.functional.asIf
import javafx.stage.Window as WindowFX

fun WindowFX.asAppWindow() = properties[Window.keyWindowAppWindow] as? Window

fun WindowFX.asLayout() = null
   ?: properties[Window.keyWindowLayout] as? Layout
   ?: scene?.root?.properties?.get(Window.keyWindowLayout) as? Layout

var WindowFX.popWindowOwner: javafx.stage.Window?
   get() = properties["popWindowOwner"].asIf()
   set(value) {
      properties["popWindowOwner"] = value
   }