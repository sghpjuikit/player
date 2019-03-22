package sp.it.pl.gui.objects.window.stage

import javafx.stage.Window as WindowFX

fun WindowFX.asWindowOrNull() = properties["window"] as? Window