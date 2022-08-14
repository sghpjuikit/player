package sp.it.util.ui

import javafx.beans.value.ObservableValue
import javafx.scene.Scene
import javafx.stage.Window

/** @return property that is true iff this scene is attached to a window that is [Window.displayed] */
val Scene.displayed: ObservableValue<Boolean> get() = windowProperty().flatMap { it.showingProperty() }.orElse(false)