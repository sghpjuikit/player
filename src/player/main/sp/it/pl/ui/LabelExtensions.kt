package sp.it.pl.ui

import javafx.scene.Node
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.pl.ui.objects.icon.NullCheckIcon
import sp.it.util.access.toggle
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventDown
import sp.it.util.type.WriteOnlyPropertyOperator

/** Sets [Label.labelFor] with mouse click focusing the node */
val Label.labelForWithClick: WriteOnlyPropertyOperator<Node, Subscription>
   get() = WriteOnlyPropertyOperator {
      labelFor = it

      Subscription(
         onEventDown(MOUSE_CLICKED, PRIMARY) { _ ->
            it.requestFocus()

            if (it is CheckBox) it.selectedProperty().toggle()
            if (it is NullCheckIcon) it.selected.value = it.selected.value.let { when(it) { null -> true true -> false false -> null } }
            if (it is CheckIcon) it.selected.toggle()
         },
         Subscription { if (labelFor==it) labelFor = null }
      )
   }