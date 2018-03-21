package htmlViewer

import javafx.scene.web.HTMLEditor
import javafx.util.Duration.seconds
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.ClassController
import sp.it.pl.util.access.v
import sp.it.pl.util.async.runPeriodic
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfig.EditMode
import sp.it.pl.util.graphics.setAnchors
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
class HtmlViewer: ClassController() {

    val editor = HTMLEditor()

    @IsConfig(name = "Last visited address", info = "Last visited address", editable = EditMode.APP)
    val text = v("") { editor.htmlText = it }

    override fun init() {
        val input = getInputs().create<String>("Html") { text.setValue(it ?: "") }
        onClose += input.monitor { text.setValue(it ?: "") }

        val output = getOutputs().create(widget.id, "Html", "")
        onClose += text sync { output.setValue(it) }

        val t = runPeriodic(seconds(5.0)) { text.setValue(editor.htmlText) }
        onClose += t::stop

        children += editor
        editor.setAnchors(0.0)

    }

}