package htmlViewer

import javafx.scene.web.HTMLEditor
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.util.access.initSync
import sp.it.pl.util.access.v
import sp.it.pl.util.async.runPeriodic
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.math.seconds
import sp.it.pl.util.reactive.on
import javafx.scene.layout.Priority
import javafx.scene.layout.GridPane
import javafx.scene.web.WebView

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
class HtmlViewer(widget: Widget): SimpleController(widget) {

    val editor = HTMLEditor()
    val text = v("").initSync { editor.htmlText = it }
    val input = inputs.create<String>("Html") { text.value = it ?: "" }
    val output = outputs.create(widget.id, "Html", "")

    init {
        editor.fixHardcodedSize()
        input.monitor { text.value = it ?: "" } on onClose
        runPeriodic(5.seconds) { output.value = editor.htmlText } on onClose

        root.lay += editor
    }

    companion object {
        fun HTMLEditor.fixHardcodedSize() {
            val webView = lookup("WebView") as WebView
            GridPane.setHgrow(webView, Priority.ALWAYS)
            GridPane.setVgrow(webView, Priority.ALWAYS)
        }
    }

}