package sp.it.pl.layout.widget

import javafx.scene.layout.Pane
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.main.APP
import sp.it.util.conf.Config
import sp.it.util.file.div

/** Empty widget. Useful for certain layout operations and as a fill in for null. */
@Widget.Info(
        author = "Martin Polakovic",
        name = "Empty",
        description = "Empty widget with no content or functionality.",
        version = "1.0",
        year = "2014",
        group = Widget.Group.OTHER
)
@ExperimentalController("Has no use for user")
class EmptyWidget(widget: Widget): Controller(widget) {

    private val root = Pane()

    override fun loadFirstTime() = root
    override fun focus() {}
    override fun close() {}
    override fun getField(n: String) = null
    override fun getFields() = emptyList<Config<Any>>()
    override fun getFieldsMap() = emptyMap<String, Config<Any>>()

}

val emptyWidgetFactory = WidgetFactory(EmptyWidget::class, APP.DIR_WIDGETS/"Empty", null)