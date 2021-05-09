package sp.it.util.access

import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.TextInputControl
import javafx.scene.control.TreeItem
import javafx.scene.text.TextAlignment
import javafx.stage.Window

/** [Window.focusedProperty] */
val Window.focused: ReadOnlyBooleanProperty get() = focusedProperty()
/** [Window.showingProperty] */
val Window.showing: ReadOnlyBooleanProperty get() = showingProperty()
/** [Node.focusedProperty] */
val Node.focused: ReadOnlyBooleanProperty get() = focusedProperty()
/** [TextInputControl.editableProperty] */
val TextInputControl.editable: BooleanProperty get() = editableProperty()
/** [ComboBox.editableProperty] */
val ComboBox<*>.editable: BooleanProperty get() = editableProperty()
/** [TreeItem.expandedProperty] */
val TreeItem<*>.expanded: BooleanProperty get() = expandedProperty()
/** [Node.visibleProperty] */
val Node.visible: BooleanProperty get() = visibleProperty()
/** [Node.visibleProperty] */
val javafx.scene.text.Text.textAlign: ObjectProperty<TextAlignment> get() = textAlignmentProperty()