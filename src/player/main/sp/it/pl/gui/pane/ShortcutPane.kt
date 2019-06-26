package sp.it.pl.gui.pane

import javafx.geometry.HPos
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.VPos
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.gui.objects.icon.CheckIcon
import sp.it.pl.main.IconMD
import sp.it.pl.main.infoIcon
import sp.it.util.access.v
import sp.it.util.action.Action
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.ui.Util.layVertically
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.scrollPane
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox

class ShortcutPane: OverlayPane<Collection<Action>>() {

    val hideEmptyShortcuts = v(true)
    private val g = GridPane()
    private var value: Collection<Action> = emptySet()

    init {
        styleClass += STYLECLASS
        hideEmptyShortcuts attach { if (isShown()) build(value) }

        content = vBox(5, CENTER) {
            maxWidth = 500.0

            lay += hBox(5, CENTER_RIGHT) {
                lay += CheckIcon(hideEmptyShortcuts)
                    .icons(IconMD.CHECKBOX_BLANK_CIRCLE_OUTLINE, IconMD.CLOSE_CIRCLE_OUTLINE)
                    .tooltip("$HIDE_EMPTY_NAME\n\n$HIDE_EMPTY_INFO")
                lay += infoIcon("Shortcut viewer\n\nDisplays available shortcuts. Optionally also those that have not been assigned yet.")
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

    override fun show(data: Collection<Action>?) {
        super.show()
        value = data.orEmpty()
        build(value)
    }

    private fun build(actions: Collection<Action>) {
        g.children.clear()
        g.rowConstraints.clear()
        g.columnConstraints.clear()

        g.columnConstraints += ColumnConstraints(150.0, 150.0, 150.0, Priority.NEVER, HPos.RIGHT, false)
        g.columnConstraints += ColumnConstraints(10.0)
        g.columnConstraints += ColumnConstraints(-1.0, -1.0, -1.0, Priority.ALWAYS, HPos.LEFT, false)

        var i = -1
        actions.asSequence()
            .filter { !hideEmptyShortcuts.value || it.hasKeysAssigned() }
            .groupBy { it.group }
            .entries.asSequence()
            .sortedBy { it.key }
            .onEach { it.value.sortedBy { it.name } }
            .forEach { (key, value) ->
                // group title row
                i++
                val group = label(key) {
                    styleClass += STYLECLASS_GROUP
                }
                g.add(layVertically(0.0, CENTER, label(), group), 2, i)
                GridPane.setValignment(group.parent, VPos.CENTER)
                GridPane.setHalignment(group.parent, HPos.LEFT)

                // shortcut rows
                for (a in value) {
                    i++
                    g.add(label(a.keys), 0, i)
                    g.add(label(a.name), 2, i)
                }
            }
    }

    companion object {
        const val HIDE_EMPTY_NAME = "Hide unassigned shortcuts"
        const val HIDE_EMPTY_INFO = "Displays only shortcuts that have keys assigned"
        private const val STYLECLASS = "shortcut-pane"
        private const val STYLECLASS_GROUP = "shortcut-pane-group-label"
    }

}