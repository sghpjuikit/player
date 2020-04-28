package sp.it.pl.ui.pane

import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos.BOTTOM_CENTER
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.VPos
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.*
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.main.IconMD
import sp.it.pl.main.Key
import sp.it.pl.main.emScaled
import sp.it.pl.main.infoIcon
import sp.it.pl.ui.objects.icon.CheckIcon
import sp.it.util.access.v
import sp.it.util.action.Action
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.system.Os
import sp.it.util.text.keys
import sp.it.util.text.keysUi
import sp.it.util.text.nameUi
import sp.it.util.ui.Util.layVertically
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.scrollPane
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import sp.it.pl.main.AppSettings.ui.view.shortcutViewer.hideUnassignedShortcuts as hideUnassignedShortcuts1

class ShortcutPane: OverlayPane<Collection<ShortcutPane.Entry>>() {

   val hideEmptyShortcuts = v(true)
   private val grid = GridPane()
   private var value: Collection<Entry> = emptySet()

   init {
      styleClass += STYLECLASS
      hideEmptyShortcuts attach { if (isShown()) build(value) }

      content = vBox(5, CENTER) {
         maxWidth = 700.emScaled

         lay += hBox(5, CENTER_RIGHT) {
            lay += CheckIcon(hideEmptyShortcuts)
               .icons(IconMD.CHECKBOX_BLANK_CIRCLE_OUTLINE, IconMD.CLOSE_CIRCLE_OUTLINE)
               .tooltip("${hideUnassignedShortcuts1.name}\n\n${hideUnassignedShortcuts1.info}")
            lay += infoIcon("Shortcut viewer\n\nDisplays available shortcuts. Optionally also those that have not been assigned yet.")
         }
         lay(ALWAYS) += hBox(20.emScaled, BOTTOM_CENTER) {
            padding = Insets(20.emScaled)

            lay += text(
               buildString {
                  appendln("${ALT.nameUi} → Alt")
                  appendln("${CONTROL.nameUi} → Ctrl")
                  appendln("${SHIFT.nameUi} → Shift")
                  appendln("${ESCAPE.nameUi} → Escape")
                  if (Os.WINDOWS.isCurrent) appendln("${WINDOWS.nameUi} → Win")
                  if (Os.OSX.isCurrent) appendln("${COMMAND.nameUi} → Command")
               }
            )
            lay(ALWAYS) += scrollPane {
               isFitToWidth = true
               isFitToHeight = false
               hbarPolicy = NEVER
               vbarPolicy = AS_NEEDED
               consumeScrolling()

               content = stackPane {
                  lay += grid
               }
            }
         }
      }
   }

   override fun show(data: Collection<Entry>) {
      super.show()
      value = data
      build(value)
   }

   private fun build(actions: Collection<Entry>) {
      grid.children.clear()
      grid.rowConstraints.clear()
      grid.columnConstraints.clear()

      grid.columnConstraints += ColumnConstraints(180.emScaled, 180.emScaled, 180.emScaled, Priority.NEVER, HPos.RIGHT, false)
      grid.columnConstraints += ColumnConstraints(10.emScaled)
      grid.columnConstraints += ColumnConstraints(-1.0, -1.0, -1.0, ALWAYS, HPos.LEFT, false)

      var i = -1
      actions.asSequence()
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

   class Entry(val groupUi: String, val nameUi: String, val keysUi: String?) {
      constructor(a: Action): this(a.groupUi, a.nameUi, if (a.hasKeysAssigned()) a.keysUi() else null)
   }

   companion object {
      private const val STYLECLASS = "shortcut-pane"
      private const val STYLECLASS_GROUP = "shortcut-pane-group-label"
   }

}