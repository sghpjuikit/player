package sp.it.pl.layout.widget

import javafx.scene.layout.Region
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.controller.io.Inputs
import sp.it.pl.layout.widget.controller.io.Outputs
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.conf.Config
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
internal class EmptyWidget: Widget<EmptyWidget>("Empty", APP.widgetManager.widgetFactoryEmpty), Controller<EmptyWidget> {

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