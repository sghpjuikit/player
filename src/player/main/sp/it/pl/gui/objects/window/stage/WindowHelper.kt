package sp.it.pl.gui.objects.window.stage

import sp.it.pl.layout.container.Layout
import javafx.stage.Window as WindowFX

fun WindowFX.asAppWindow() = properties[Window.keyWindowAppWindow] as? Window

fun WindowFX.asLayout() = properties[Window.keyWindowLayout] as? Layout