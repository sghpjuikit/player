package sp.it.util.ui

import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.stage.Window
import sp.it.util.functional.traverse

/** @return property that is true iff this node is attached to a scene in a window that is [Window.displayed] */
val Node.displayed: ObservableValue<Boolean> get() = sceneProperty().flatMap { it.windowProperty() }.flatMap { it.showingProperty() }.orElse(false)

/** @return path of this node to scene root */
val Node.scenePath: String get() = traverseParents().map { "${it.id.orEmpty()}:${it::class.simpleName}" }.toList().asReversed().joinToString(" ")

/** @return sequence of this and all parents in bottom to top order */
fun Node.traverseParents(): Sequence<Node> = traverse { it.parent }