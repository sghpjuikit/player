package layout.widget

import javafx.scene.layout.Region
import layout.widget.controller.Controller
import layout.widget.controller.io.Inputs
import layout.widget.controller.io.Outputs
import main.App
import util.conf.Config
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
internal class EmptyWidget: Widget<EmptyWidget>("Empty", App.APP.widgetManager.widgetFactoryEmpty), Controller<EmptyWidget> {

    private val o = Outputs()
    private val i = Inputs()

    init {
        controller = this
    }

    override fun close() = super<Widget>.close()

    override fun loadFirstTime() = Region()

    override fun refresh() {}

    override fun getWidget() = this

    override fun getOutputs() = o

    override fun getInputs() = i

    override fun getField(n: String) = null

    // can not use default implementation. it calls getFields on the controller
    // since this=this.controller -> StackOverflow
    override fun getFields() = emptyList<Config<Any>>()

    override fun getFieldsMap() = emptyMap<String, Config<Any>>()

    override fun getLocation() = super<Widget>.getLocation()

    override fun getUserLocation() = super<Widget>.getUserLocation()

    @Throws(ObjectStreamException::class)
    override fun readResolve(): Any {
        root = Region()
        controller = this
        return super.readResolve()
    }

}