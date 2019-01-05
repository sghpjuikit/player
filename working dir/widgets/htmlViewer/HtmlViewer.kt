package htmlViewer

import javafx.scene.web.HTMLEditor
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.util.access.initAttach
import sp.it.pl.util.access.v
import sp.it.pl.util.async.runPeriodic
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.math.seconds
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.sync

@Widget.Info(
        name = "HtmlEditor",
        author = "Martin Polakovic",
        howto = "",
        description = "Very simple html text editor.",
        notes = "", version = "0.5",
        year = "2016",
        group = Widget.Group.OTHER
)
@ExperimentalController
class HtmlViewer(widget: Widget<*>): SimpleController(widget) {

    val editor = HTMLEditor()
    val text = v("").initAttach { editor.htmlText = it }
    val input = inputs.create<String>("Html") { text.value = it ?: "" }
    val output = outputs.create(widget.id, "Html", "")

    init {
        input.monitor { text.value = it ?: "" } on onClose
        text sync { output.setValue(it) } on onClose
        runPeriodic(5.seconds) { text.value = editor.htmlText } on onClose

        layFullArea += editor
    }

}