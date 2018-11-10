package sp.it.pl.layout.widget

import javafx.scene.Node
import javafx.scene.layout.Region
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.controller.io.Inputs
import sp.it.pl.layout.widget.controller.io.Outputs
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.conf.Config
import sp.it.pl.util.file.childOf
import java.io.ObjectStreamException

/** Empty widget. Useful for certain layout operations and as a fill in for null. */
@Widget.Info(
        author = "Martin Polakovic",
        name = "Empty",
        description = "Empty widget with no content or functionality.",
        version = "1.0",
        year = "2014",
        group = Widget.Group.OTHER
)
class EmptyWidget: Widget<EmptyWidget>("Empty", emptyWidgetFactory), Controller {

    private val o = Outputs()
    private val i = Inputs()
    override val location = super<Widget>.location
    override val userLocation = super<Widget>.userLocation

    init {
        controller = this
    }

    override fun close() = super<Widget>.close()

    override fun load(): Node {
        if (root==null) root = loadFirstTime()
        return root
    }

    override fun loadFirstTime() = Region()

    override fun refresh() {}

    override fun getOwnerWidget() = this

    override fun getOwnedOutputs() = o

    override fun getOwnedInputs() = i

    override fun getField(n: String) = null

    // can not use default impl. - it calls getFields on the controller since this=this.controller -> StackOverflow
    override fun getFields() = emptyList<Config<Any>>()

    override fun getFieldsMap() = emptyMap<String, Config<Any>>()

    @Throws(ObjectStreamException::class)
    override fun readResolve(): Any {
        root = Region()
        controller = this
        return super.readResolve()
    }

}

val emptyWidgetFactory: WidgetFactory<*> = WidgetFactory(EmptyWidget::class, APP.DIR_WIDGETS.childOf("Empty"))