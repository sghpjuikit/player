package sp.it.pl.gui.pane

import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.VPos
import javafx.scene.Cursor
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import sp.it.pl.main.AppUtil
import sp.it.pl.util.conf.MultiConfigurable
import sp.it.pl.main.createInfoIcon
import sp.it.pl.util.graphics.Util.layHeaderTop
import sp.it.pl.util.graphics.Util.layHorizontally
import sp.it.pl.util.graphics.Util.layStack
import sp.it.pl.util.graphics.Util.layVertically
import sp.it.pl.util.system.copyToSysClipboard
import sp.it.pl.util.toLocalDateTime
import sp.it.pl.util.units.Dur

class InfoPane(override val configurableDiscriminant: String): OverlayPane<Void>(), MultiConfigurable {

    private val g = GridPane()

    init {
        styleClass += STYLECLASS

        val helpI = createInfoIcon("System information viewer\n\nDisplays available system properties. Click on the property to copy the value.")
        val sp = ScrollPane().apply {
            onScroll = EventHandler { it.consume() }
            content = layStack(g, CENTER)
            isFitToWidth = true
            isFitToHeight = false
            hbarPolicy = ScrollBarPolicy.NEVER
            vbarPolicy = ScrollBarPolicy.AS_NEEDED
        }
        content = layHeaderTop(5.0, CENTER,
                layHorizontally(5.0, CENTER_RIGHT, helpI),
                layStack(sp, CENTER)
        ).also {
            it.maxWidth = 800.0
            it.maxHeightProperty().bind(heightProperty().subtract(100))
        }
    }

    override fun show(data: Void?) {
        super.show()

        g.children.clear()
        g.rowConstraints.clear()
        g.columnConstraints.clear()

        g.columnConstraints += ColumnConstraints(550.0, 550.0, 550.0, NEVER, HPos.RIGHT, false)
        g.columnConstraints += ColumnConstraints(10.0)
        g.columnConstraints += ColumnConstraints(-1.0, -1.0, -1.0, ALWAYS, HPos.LEFT, false)

        var i = -1
        computeProperties().asSequence()
                .sortedBy { it.key }
                .onEach { it.value.sortBy { it.name } }
                .forEach {
                    // group title row
                    i++
                    val group = Label(it.key.toLowerCase().capitalize())
                    group.styleClass += STYLECLASS_GROUP
                    g.add(layVertically(0.0, Pos.CENTER, Label(), group), 2, i)
                    GridPane.setValignment(group.parent, VPos.CENTER)
                    GridPane.setHalignment(group.parent, HPos.LEFT)

                    // property rows
                    for (n in it.value) {
                        i++

                        val hasGroupAsPrefix = n.name.length > it.key.length && n.name.startsWith(it.key)
                        val name = if (hasGroupAsPrefix) n.name.substring(it.key.length+1) else n.name
                        val value = n.value.fixASCII()

                        val nameL = Label(name)
                        val valL = Label(value).apply {
                            cursor = Cursor.HAND
                            setOnMouseClicked { copyToSysClipboard(value) }
                        }
                        g.add(valL, 0, i)
                        g.add(nameL, 2, i)
                    }
                }
    }

    private fun computeProperties(): Map<String, MutableList<Named>> {
        fun <K,V> MutableMap<K,MutableList<V>>.group(key: K) = getOrPut(key) { ArrayList() }
        val ps = System.getProperties()
                .entries.asSequence()
                .filter { it.key is String && it.value is String }
                .map { it.key as String named it.value as String }
                .groupByTo(HashMap()) { it.name.getPropertyGroup() }

        val p = ProcessHandle.current()
        val pInfo = p.info()
        ps.group("process") += listOf(
                "pid" named p.pid().toString(),
                "arguments" named pInfo.arguments().map { it.joinToString(", ") }.orElse(""),
                "command" named pInfo.command().orElse(""),
                "commandline" named pInfo.commandLine().orElse(""),
                "start time" named pInfo.startInstant().map { it.toLocalDateTime().toString() }.orElse(""),
                "running time" named pInfo.totalCpuDuration().map { Dur(it.toMillis().toDouble()).toString() }.orElse(""),
                "user" named pInfo.user().orElse("")
        )
        ps.group("java") += "vm.arguments" named AppUtil.APP.fetchVMArguments().joinToString(" ")

        return ps
    }

    companion object {
        private const val STYLECLASS = "info-pane"
        private const val STYLECLASS_GROUP = "info-pane-group-label"
    }

}

private data class Named(val name: String, val value: String)

private infix fun String.named(value: String) = Named(this, value)

private fun String.getPropertyGroup() = substringBefore('.')

private fun String.fixASCII(): String {
    when (length) {
        1 -> {
            if (this[0]=='\n') return "\n"
            if (this[0]=='\r') return "\r"
        }
        2 -> {
            if (this[0]=='\n' && this[1]=='\r') return "\\n\\r"
            if (this[0]=='\r' && this[1]=='\n') return "\\r\\n"
        }
    }
    return this
}