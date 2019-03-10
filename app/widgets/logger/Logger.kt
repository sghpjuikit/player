package logger

import javafx.event.EventHandler
import javafx.scene.control.TextArea
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.TextDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets
import sp.it.pl.main.scaleEM
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cv
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.prefSize
import sp.it.pl.util.graphics.x
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
        root.prefSize = 500.scaleEM() x 500.scaleEM()
        root.lay += area.apply {
            isEditable = false
            isWrapText = false
            wrapTextProperty() syncFrom wrapText on onClose

            text = "# This is redirected output (System.out) stream of this application.\n"
        }

        root.onScroll = EventHandler { it.consume() }

        APP.systemout.addListener { area.appendText(it) } on onClose
    }

    override fun showText(text: String) = println(text)

}