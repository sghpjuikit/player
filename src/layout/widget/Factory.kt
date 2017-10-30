package layout.widget

import layout.Component
import layout.widget.controller.Controller
import main.App
import util.file.childOf
import util.file.nameOrRoot
import util.type.ClassName
import java.io.File

/** Component factory that creates component by deserializing it from file. */
interface ComponentFactory<out T: Component>: ComponentInfo {
    fun create(): T
}

/** Component factory that creates widgets. */
@Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
@Widget.Info // empty widget info with default values
class WidgetFactory<C: Controller<*>>: ComponentFactory<Widget<C>>, WidgetInfo {

    val controllerType: Class<C>
    val location: File?
    val locationUser: File?
    private val name: String
    private val nameGui: String
    private val description: String
    private val version: String
    private val author: String
    private val contributor: String
    private val howto: String
    private val year: String
    private val notes: String
    private val group: Widget.Group

    /** Whether this factory will be preferred over others of the same group. */
    var isPreferred = false

    /** Whether this factory will be ignored on widget requests. */
    var isIgnored = false

    /**
     * @param controllerType of the controller of the widget this factory will create
     * @param location parent directory of the widget
     */
    @JvmOverloads constructor(controllerType: Class<C>, location: File? = null) {
        val i: Widget.Info = controllerType.getAnnotation(Widget.Info::class.java) ?: WidgetFactory::class.java.getAnnotation(Widget.Info::class.java)!!
        this.name = ClassName.of(controllerType)
        this.controllerType = controllerType
        this.location = location
        this.locationUser = if (location==null) null else App.APP.DIR_USERDATA.childOf("widgets", location.nameOrRoot)
        this.nameGui = if (i.name.isEmpty()) name else i.name
        this.description = i.description
        this.version = i.version
        this.author = i.author
        this.contributor = i.contributor
        this.howto = i.howto
        this.year = i.year
        this.notes = i.notes
        this.group = i.group
    }

    override fun name() = name
    override fun nameGui() = nameGui
    override fun description() = description
    override fun version() = version
    override fun author() = author
    override fun contributor() = contributor
    override fun year() = year
    override fun howto() = howto
    override fun notes() = notes
    override fun group() = group
    override fun type() = controllerType

    override fun create(): Widget<C> = Widget(name, this)

    override fun toString() = "${javaClass.simpleName} $name $nameGui $controllerType"

}

/** Component factory that creates component by deserializing it from file. */
class DeserializingFactory: ComponentFactory<Component> {
    val launcher: File
    private val nameGui: String

    constructor(launcher: File) {
        this.launcher = launcher
        this.nameGui = launcher.nameWithoutExtension
    }

    override fun nameGui() = nameGui

    override fun create() = App.APP.windowManager.instantiateComponent(launcher)!!

    override fun toString() = "${javaClass.simpleName} $nameGui $launcher"

}