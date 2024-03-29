package sp.it.pl.ui.pane

import javafx.geometry.HPos.LEFT
import javafx.geometry.HPos.RIGHT
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.VPos
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.main.APP
import sp.it.pl.main.contextMenuFor
import sp.it.pl.main.emScaled
import sp.it.pl.main.infoIcon
import sp.it.pl.main.toUi
import sp.it.util.Named
import sp.it.util.named
import sp.it.util.namedOrNaIfEmpty
import sp.it.util.namedOrNaIfNull
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.onEventDown
import sp.it.util.text.capitalLower
import sp.it.util.toLocalDateTime
import sp.it.util.ui.Util.layVertically
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.scrollPane
import sp.it.util.ui.show
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.units.javafx
import sp.it.util.units.toHMSMs

class InfoPane: OverlayPane<Unit>() {

   private val g = GridPane()

   init {
      styleClass += STYLECLASS

      content = vBox(5, CENTER) {
         maxWidth = 900.emScaled

         lay += hBox(5, CENTER_RIGHT) {
            lay += infoIcon("System information viewer\n\nDisplays available system properties. Click on the property to copy the value.")
         }
         lay(ALWAYS) += stackPane {
            lay += scrollPane {
               content = stackPane(g)
               isFitToWidth = true
               isFitToHeight = false
               hbarPolicy = NEVER
               vbarPolicy = AS_NEEDED
               consumeScrolling()
            }
         }
      }
   }

   override fun show(data: Unit) {
      super.show()

      g.children.clear()
      g.rowConstraints.clear()
      g.columnConstraints.clear()

      g.columnConstraints += ColumnConstraints(550.emScaled, 550.emScaled, 550.emScaled, Priority.NEVER, RIGHT, false)
      g.columnConstraints += ColumnConstraints(10.0)
      g.columnConstraints += ColumnConstraints(-1.0, -1.0, -1.0, ALWAYS, LEFT, false)

      var i = -1
      computeProperties().asSequence()
         .sortedBy { it.key }
         .onEach { it.value.sortBy { it.name } }
         .forEach {
            // group title row
            i++
            val group = label(it.key.capitalLower()) {
               styleClass += STYLECLASS_GROUP
            }
            g.add(layVertically(0.0, CENTER, label(), group), 2, i)
            GridPane.setValignment(group.parent, VPos.CENTER)
            GridPane.setHalignment(group.parent, LEFT)

            // property rows
            for (n in it.value) {
               i++

               val hasGroupAsPrefix = n.name.length>it.key.length && n.name.startsWith(it.key)
               val name = if (hasGroupAsPrefix) n.name.substring(it.key.length + 1) else n.name
               val value = n.value.fixASCII()

               val nameL = label(name)
               val valL = label(value.toUi()) {
                  isWrapText = true
                  onEventDown(MOUSE_CLICKED, SECONDARY) { contextMenuFor(value).show(this, it) }
               }
               g.add(valL, 0, i)
               g.add(nameL, 2, i)
            }
         }
   }

   private fun computeProperties(): Map<String, MutableList<Named>> {
      val ps = System.getProperties()
         .entries.asSequence()
         .map { it.key.toString() named it.value }
         .groupByTo(HashMap()) { it.name.getPropertyGroup() }

      val p = ProcessHandle.current()
      val pInfo = p.info()
      ps.group("process") += listOf(
         "pid" named p.pid().toString(),
         "arguments" namedOrNaIfEmpty pInfo.arguments().map { it.joinToString(", ") },
         "command" namedOrNaIfEmpty pInfo.command(),
         "commandline" namedOrNaIfEmpty pInfo.commandLine(),
         "start time" namedOrNaIfEmpty pInfo.startInstant().map { it.toLocalDateTime() },
         "running time" namedOrNaIfEmpty pInfo.totalCpuDuration().map { it.javafx.toHMSMs() },
         "user" namedOrNaIfEmpty pInfo.user(),
         "elevated" namedOrNaIfNull APP.processElevated
      )
      ps.group("java") += listOf(
         "vm.arguments" named APP.fetchVMArguments().joinToString(" ")
      )
      ps.group("kotlin") += listOf(
         "version" named KotlinVersion.CURRENT
      )
      ps.group("app") += listOf(
         "version" named APP.version,
         "location" named APP.location
      )

      return ps
   }

   companion object {
      private const val STYLECLASS = "info-pane"
      private const val STYLECLASS_GROUP = "info-pane-group-label"

      private fun <K, V> MutableMap<K, MutableList<V>>.group(key: K) = getOrPut(key) { ArrayList() }

      private fun String.getPropertyGroup() = substringBefore('.')

      private fun Any?.fixASCII(): Any? = when (this) {
         is String -> when (length) {
            1 -> {
               if (this[0]=='\n') "\n"
               else if (this[0]=='\r') "\r"
               else this
            }
            2 -> {
               if (this[0]=='\n' && this[1]=='\r') "\\n\\r"
               else if (this[0]=='\r' && this[1]=='\n') "\\r\\n"
               else this
            }
            else -> this
         }
         else -> this
      }
   }

}

