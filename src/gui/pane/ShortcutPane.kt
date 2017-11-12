package gui.pane

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_CIRCLE_OUTLINE
import gui.objects.icon.CheckIcon
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.VPos
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import main.createInfoIcon
import util.access.V
import util.action.Action
import util.conf.IsConfig
import util.graphics.Util.layHeaderTop
import util.graphics.Util.layHorizontally
import util.graphics.Util.layStack
import util.graphics.Util.layVertically
import util.reactive.attach

class ShortcutPane: OverlayPane<Collection<Action>> {

    private val g = GridPane()
    private var value: Collection<Action> = emptySet()

    @IsConfig(name = HE_TITLE, info = HE_INFO)
    val hideEmptyShortcuts = V(true)

    constructor(): super() {
        styleClass += STYLECLASS

        val helpI = createInfoIcon("Shortcut viewer\n\nDisplays available shortcuts. Optionally also those that have not been assigned yet.")
        val hideI = CheckIcon(hideEmptyShortcuts)
                .icons(CHECKBOX_BLANK_CIRCLE_OUTLINE, CLOSE_CIRCLE_OUTLINE)
                .tooltip("$HE_TITLE\n\n$HE_INFO")

        val sp = ScrollPane().apply {
            onScroll = EventHandler { it.consume() }
            content = layStack(g, CENTER)
            isFitToWidth = true
            isFitToHeight = false
            hbarPolicy = ScrollBarPolicy.NEVER
            vbarPolicy = ScrollBarPolicy.AS_NEEDED
        }
        content = layHeaderTop(5.0, CENTER,
                layHorizontally(5.0, CENTER_RIGHT, hideI, helpI),
                layStack(sp, CENTER)
        ).apply {
            maxWidth = 500.0
            maxHeightProperty().bind(this@ShortcutPane.heightProperty().subtract(100))
        }

        hideEmptyShortcuts attach { if (isShown()) build(value) }
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

        g.columnConstraints += ColumnConstraints(150.0, 150.0, 150.0, NEVER, HPos.RIGHT, false)
        g.columnConstraints += ColumnConstraints(10.0)
        g.columnConstraints += ColumnConstraints(-1.0, -1.0, -1.0, ALWAYS, HPos.LEFT, false)

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
                    val group = Label(key)
                    group.styleClass += STYLECLASS_GROUP
                    g.add(layVertically(0.0, Pos.CENTER, Label(), group), 2, i)
                    GridPane.setValignment(group.parent, VPos.CENTER)
                    GridPane.setHalignment(group.parent, HPos.LEFT)

                    // shortcut rows
                    for (a in value) {
                        i++
                        g.add(Label(a.keys), 0, i)
                        g.add(Label(a.name), 2, i)
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