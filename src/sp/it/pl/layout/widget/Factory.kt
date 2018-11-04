package sp.it.pl.layout.widget

import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.main.APP
import sp.it.pl.util.file.childOf
import sp.it.pl.util.file.nameOrRoot
import sp.it.pl.util.type.ClassName
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/** Component factory that creates component by deserializing it from file. */
interface ComponentFactory<out T: Component>: ComponentInfo {
    fun create(): T
}

/** Component factory that creates widgets. */
@Widget.Info
class WidgetFactory<C: Controller>: ComponentFactory<Widget<C>>, WidgetInfo {

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
    constructor(controllerType: KClass<C>, location: File?) {
        val i: Widget.Info = controllerType.findAnnotation() ?: WidgetFactory::class.findAnnotation()!!
        this.name = ClassName.of(controllerType.java)
        this.controllerType = controllerType.java
        this.location = location
        this.locationUser = location?.let { APP.DIR_USERDATA.childOf("widgets", it.nameOrRoot) }
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

    override fun create() = APP.windowManager.instantiateComponent(launcher)!!

    override fun toString() = "${javaClass.simpleName} $nameGui $launcher"

}