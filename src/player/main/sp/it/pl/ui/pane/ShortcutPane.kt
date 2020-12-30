package sp.it.pl.ui.pane

import sp.it.pl.main.AppSettings.ui.view.shortcutViewer.hideUnassignedShortcuts as hideUnassignedShortcuts1
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.VPos
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.KeyCode.ALT
import javafx.scene.input.KeyCode.COMMAND
import javafx.scene.input.KeyCode.CONTROL
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyCode.WINDOWS
import javafx.scene.input.MouseButton
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment.RIGHT
import javafx.scene.text.TextFlow
import sp.it.pl.main.IconMD
import sp.it.pl.main.Key
import sp.it.pl.main.emScaled
import sp.it.pl.main.infoIcon
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.util.access.v
import sp.it.util.action.Action
import sp.it.util.collections.setToOne
import sp.it.util.functional.supplyIf
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.system.Os
import sp.it.util.text.keysUi
import sp.it.util.text.nameUi
import sp.it.util.ui.Util.layVertically
import sp.it.util.ui.borderPane
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.scrollPane
import sp.it.util.ui.vBox

class ShortcutPane: OverlayPane<ShortcutPane.Info>() {

   val hideEmptyShortcuts = v(true)
   private val grid = GridPane()
   private val text = TextFlow()
   private var value: Info = Info("", listOf())

   init {
      styleClass += STYLECLASS
      hideEmptyShortcuts attach { if (isShown()) build(value) }

      content = vBox(5, CENTER) {
         maxWidth = 750.emScaled

         lay += hBox(5, CENTER_RIGHT) {
            lay += CheckIcon(hideEmptyShortcuts)
               .icons(IconMD.CHECKBOX_BLANK_CIRCLE_OUTLINE, IconMD.CLOSE_CIRCLE_OUTLINE)
               .tooltip("${hideUnassignedShortcuts1.name}\n\n${hideUnassignedShortcuts1.info}")
            lay += infoIcon("Shortcut viewer\n\nDisplays available shortcuts. Optionally also those that have not been assigned yet.")
         }
         lay(ALWAYS) += borderPane {
            padding = Insets(20.emScaled)

            left = vBox(0, BOTTOM_LEFT) {
               id = "legend"
               padding = Insets(0.0, 20.emScaled, 0.0, 0.0)

               lay += (MouseButton.values().toList() - MouseButton.NONE).map { legendRow(it) }
               lay += label()
               lay += legendRow(ALT)
               lay += legendRow(ALT)
               lay += legendRow(CONTROL)
               lay += legendRow(SHIFT)
               lay += legendRow(ESCAPE)
               lay += supplyIf(Os.WINDOWS.isCurrent) { legendRow(WINDOWS) }
               lay += supplyIf(Os.OSX.isCurrent) { legendRow(COMMAND) }
            }
            center = scrollPane {
               isFitToWidth = true
               isFitToHeight = false
               hbarPolicy = NEVER
               vbarPolicy = AS_NEEDED
               consumeScrolling()

               content = vBox {
                  lay += text
                  lay += grid
               }
            }
         }
      }
   }

   override fun show(data: Info) {
      super.show()
      value = data
      build(value)
   }

   private fun build(data: Info) {
      text.children setToOne Text(data.text)

      grid.children.clear()
      grid.rowConstraints.clear()
      grid.columnConstraints.clear()

      grid.columnConstraints += ColumnConstraints(180.emScaled, 180.emScaled, 180.emScaled, Priority.NEVER, HPos.RIGHT, false)
      grid.columnConstraints += ColumnConstraints(10.emScaled)
      grid.columnConstraints += ColumnConstraints(-1.0, -1.0, -1.0, ALWAYS, HPos.LEFT, false)

      var i = -1
      data.entries.asSequence()
         .filter { !hideEmptyShortcuts.value || it.keysUi!=null }
         .groupBy { it.groupUi }
         .entries.asSequence()
         .sortedBy { it.key }
         .onEach { it.value.sortedBy { it.nameUi } }
         .forEach { (key, value) ->
            // group title row
            i++
            val group = label(key) {
               styleClass += STYLECLASS_GROUP
            }
            grid.add(layVertically(0.0, CENTER, label(), group), 2, i)
            GridPane.setValignment(group.parent, VPos.CENTER)
            GridPane.setHalignment(group.parent, HPos.LEFT)

            // shortcut rows
            value.asSequence().sortedBy { it.nameUi }.forEach {
               i++
               grid.add(label(it.keysUi ?: ""), 0, i)
               grid.add(label(it.nameUi), 2, i)
            }
         }
   }

   private fun legendRow(button: MouseButton) = legendRow(button.nameUi, button.name.toLowerCase().capitalize())
   private fun legendRow(key: Key) = legendRow(key.nameUi, key.name.toLowerCase().capitalize())
   private fun legendRow(left: String, right: String) = hBox(0, CENTER_LEFT) {
      lay += label(left) {
         prefWidth = 10.emScaled
         textAlignment = RIGHT
         alignment = CENTER_RIGHT
      }
      lay += label(" → $right")
   }

   class Info(val text: String, val entries: List<Entry>)
   class Entry(val groupUi: String, val nameUi: String, val keysUi: String?) {
      constructor(a: Action): this(a.groupUi, a.nameUi, if (a.hasKeysAssigned()) a.keysUi() else null)
   }

   companion object {
      private const val STYLECLASS = "shortcut-pane"
      private const val STYLECLASS_GROUP = "shortcut-pane-group-label"
   }

}