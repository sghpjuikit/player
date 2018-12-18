package sp.it.pl.gui.pane

import javafx.event.EventHandler
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
import sp.it.pl.main.createInfoIcon
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.MultiConfigurable
import sp.it.pl.util.conf.cv
import sp.it.pl.util.graphics.Util.layVertically
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.label
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.scrollPane
import sp.it.pl.util.graphics.stackPane
import sp.it.pl.util.graphics.vBox
import sp.it.pl.util.reactive.attach

class ShortcutPane(override val configurableDiscriminant: String): OverlayPane<Collection<Action>>(), MultiConfigurable {

    private val g = GridPane()
    private var value: Collection<Action> = emptySet()

    @IsConfig(name = HE_TITLE, info = HE_INFO)
    val hideEmptyShortcuts by cv(true)

    init {
        styleClass += STYLECLASS
        hideEmptyShortcuts attach { if (isShown()) build(value) }

        content = vBox(5, CENTER) {
            maxWidth = 500.0

            lay += hBox(5, CENTER_RIGHT) {
                lay += CheckIcon(hideEmptyShortcuts)
                        .icons(IconMD.CHECKBOX_BLANK_CIRCLE_OUTLINE, IconMD.CLOSE_CIRCLE_OUTLINE)
                        .tooltip("$HE_TITLE\n\n$HE_INFO")
                lay += createInfoIcon("Shortcut viewer\n\nDisplays available shortcuts. Optionally also those that have not been assigned yet.")
            }
            lay(ALWAYS) += stackPane {
                lay += scrollPane {
                    onScroll = EventHandler { it.consume() }
                    content = stackPane(g)
                    isFitToWidth = true
                    isFitToHeight = false
                    hbarPolicy = NEVER
                    vbarPolicy = AS_NEEDED
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
        private const val HE_TITLE = "Hide unassigned shortcuts"
        private const val HE_INFO = "Displays only shortcuts that have keys assigned"
        private const val STYLECLASS = "shortcut-pane"
        private const val STYLECLASS_GROUP = "shortcut-pane-group-label"
    }

}