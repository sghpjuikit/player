package logger

import javafx.scene.control.TextArea
import kotlinx.coroutines.NonCancellable.children
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.TextDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cv
import sp.it.pl.util.graphics.Util.setAnchors
import sp.it.pl.util.graphics.setMinPrefMaxSize

@Widget.Info(
        author = "Martin Polakovic",
        name = Widgets.LOGGER,
        description = "Displays console output by listening to System.out, which contains application logging.",
        version = "1",
        year = "2015",
        group = Widget.Group.DEVELOPMENT
)
class Logger(widget: Widget<*>): SimpleController(widget), TextDisplayFeature {

    private val area = TextArea()

    @IsConfig(name = "Wrap text", info = "Wrap text at the end of the text area to the next line.")
    private val wrapText by cv(false)

    init {
        area.isEditable = false
        area.isWrapText = false
        area.wrapTextProperty().bind(wrapText)
        area.appendText("# This is redirected output (System.out) stream of this application.\n")
        area.setMinPrefMaxSize(USE_COMPUTED_SIZE)
        this.setMinPrefMaxSize(USE_COMPUTED_SIZE)
        children.add(area)
        setAnchors(area, 0.0)

        onClose += APP.systemout.addListener { area.appendText(it) }
        // [Stackoverflow source](https://stackoverflow.com/a/24140252/6723250)
        //                val stream = TextAreaStream(area)
        //                val con = PrintStream(stream)
        //                System.setOut(con)
        //                System.setErr(con)
    }

    override fun showText(text: String) = println(text)

}