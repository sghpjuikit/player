package htmlViewer

import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.web.HTMLEditor
import javafx.scene.web.WebView
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.scaleEM
import sp.it.pl.util.access.initSync
import sp.it.pl.util.access.v
import sp.it.pl.util.async.runPeriodic
import sp.it.pl.util.reactive.on
import sp.it.pl.util.ui.lay
import sp.it.pl.util.ui.prefSize
import sp.it.pl.util.ui.x
import sp.it.pl.util.units.seconds

@Widget.Info(
        name = "HtmlEditor",
        author = "Martin Polakovic",
        howto = "",
        description = "Very simple html text editor.",
        notes = "",
        version = "0.5.0",
        year = "2016",
        group = Widget.Group.OTHER
)
@ExperimentalController("Needs better skin and transparent background setting")
class HtmlViewer(widget: Widget): SimpleController(widget) {

    private val editor = HTMLEditor()
    private val text = v("").initSync { editor.htmlText = it }
    private val input = inputs.create<String>("Html") { text.value = it ?: "" }
    private val output = outputs.create(widget.id, "Html", "")

    init {
        root.prefSize = 600.scaleEM() x 500.scaleEM()
        root.lay += editor.apply {
            fixHardcodedSize()
        }

        input.sync { text.value = it ?: "" } on onClose
        runPeriodic(5.seconds) { output.value = editor.htmlText } on onClose
    }

    companion object {
        fun HTMLEditor.fixHardcodedSize() {
            val webView = lookup("WebView") as WebView
            GridPane.setHgrow(webView, Priority.ALWAYS)
            GridPane.setVgrow(webView, Priority.ALWAYS)
        }
    }

}