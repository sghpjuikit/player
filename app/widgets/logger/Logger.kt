package logger

import javafx.event.EventHandler
import javafx.scene.control.TextArea
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.TextDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cv
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.syncFrom

@Widget.Info(
        author = "Martin Polakovic",
        name = Widgets.LOGGER,
        description = "Displays console output by listening to System.out, which contains application logging.",
        version = "1",
        year = "2015",
        group = Widget.Group.DEVELOPMENT
)
class Logger(widget: Widget): SimpleController(widget), TextDisplayFeature {

    @IsConfig(name = "Wrap text", info = "Wrap text at the end of the text area to the next line.")
    private val wrapText by cv(false)
    private val area = TextArea()

    init {
        onScroll = EventHandler { it.consume() }

        layFullArea += area.apply {
            isEditable = false
            isWrapText = false
            wrapTextProperty() syncFrom wrapText on onClose
        }


        onClose += APP.systemout.addListener { area.appendText(it) }
        // [Stackoverflow source](https://stackoverflow.com/a/24140252/6723250)
        //                val stream = TextAreaStream(area)
        //                val con = PrintStream(stream)
        //                System.setOut(con)
        //                System.setErr(con)

        refresh()
    }

    override fun refresh() {
        area.text = "# This is redirected output (System.out) stream of this application.\n"
    }

    override fun showText(text: String) = println(text)

}