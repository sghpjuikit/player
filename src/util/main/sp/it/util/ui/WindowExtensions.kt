package sp.it.util.ui

import javafx.beans.value.ObservableValue
import javafx.stage.Window
import sp.it.util.access.showing

/** @return property that is true iff this window is showing */
val Window.displayed: ObservableValue<Boolean> get() = showing