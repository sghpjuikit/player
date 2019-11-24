package sp.it.pl.gui.objects.window.pane

import javafx.event.Event
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Cursor
import javafx.scene.Cursor.E_RESIZE
import javafx.scene.Cursor.NE_RESIZE
import javafx.scene.Cursor.NW_RESIZE
import javafx.scene.Cursor.N_RESIZE
import javafx.scene.Cursor.SE_RESIZE
import javafx.scene.Cursor.SW_RESIZE
import javafx.scene.Cursor.S_RESIZE
import javafx.scene.Cursor.W_RESIZE
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Region
import sp.it.util.dev.fail
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.anchorPane
import sp.it.util.ui.hBox
import sp.it.util.ui.initClip
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x

@Suppress("UNUSED_PARAMETER", "UNCHECKED_CAST")
@SuppressWarnings
fun <T: Node> Node.lookupId(id: String, type: Class<T>): T = lookup("#$id") as T? ?: fail { "No match for id=$id" }

fun buildWindowLayout(onDragStart: (MouseEvent) -> Unit, onDragged: (MouseEvent) -> Unit, onDragEnd: (MouseEvent) -> Unit) = stackPane {
   prefSize = 200 x 200

   lay += anchorPane {
      id = "borders"

      layFullArea += vBox {
         id = "contentRoot"

         lay += stackPane {
            id = "headerContainer"

            lay += stackPane {
               id = "header"
               styleClass += "header"
               padding = Insets(8.0, 5.0, 0.0, 5.0)

               lay(CENTER_LEFT) += hBox(4.0, CENTER_LEFT) {
                  id = "leftHeaderBox"
                  isFillHeight = false
                  isPickOnBounds = false

                  lay += label {
                     padding = Insets(0.0, 0.0, 0.0, 5.0)
                     id = "titleL"
                  }
               }

               lay(CENTER_RIGHT) += hBox(4.0, CENTER_RIGHT) {
                  id = "rightHeaderBox"
                  isFillHeight = false
                  isPickOnBounds = false

                  onEventDown(MOUSE_DRAGGED, Event::consume)
               }
            }
         }
         lay(ALWAYS) += anchorPane {
            id = "content"
            styleClass += "content"
            minSize = 0 x 0
            initClip()
         }
      }

      fun borderRegion(w: Number, h: Number, mc: Cursor) = Region().apply {
         onEventDown(MOUSE_PRESSED, onDragStart)
         onEventDown(MOUSE_DRAGGED, onDragged)
         onEventDown(MOUSE_RELEASED, onDragEnd)
         cursor = mc
         prefSize = w x h
      }

      lay(null, 25, 0, 25) += borderRegion(-1, 4, S_RESIZE)
      lay(0, 25, null, 25) += borderRegion(-1, 4, N_RESIZE)
      lay(25, 0, 25, null) += borderRegion(4, -1, E_RESIZE)
      lay(25, null, 25, 0) += borderRegion(4, -1, W_RESIZE)
      lay(null, 0, 0, null) += borderRegion(4, 25, SE_RESIZE)
      lay(null, 0, 0, null) += borderRegion(25, 4, SE_RESIZE)
      lay(0, null, null, 0) += borderRegion(4, 25, NW_RESIZE)
      lay(0, null, null, 0) += borderRegion(25, 4, NW_RESIZE)
      lay(null, null, 0, 0) += borderRegion(25, 4, SW_RESIZE)
      lay(null, null, 0, 0) += borderRegion(4, 25, SW_RESIZE)
      lay(0, 0, null, null) += borderRegion(4, 25, NE_RESIZE)
      lay(0, 0, null, null) += borderRegion(25, 4, NE_RESIZE)
   }
}