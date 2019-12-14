package sp.it.pl.layout.widget

import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.controller.NoFactoryController
import sp.it.pl.main.APP
import sp.it.util.file.div
import sp.it.util.file.nameOrRoot
import sp.it.util.text.nullIfBlank
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmName

/** Component factory that creates component by deserializing it from file. */
interface ComponentFactory<out T: Component>: ComponentInfo {
   fun create(): T
}

/** Component factory that creates widgets. */
@Widget.Info
open class WidgetFactory<C: Controller>: ComponentFactory<Widget>, WidgetInfo {

   val controllerType: Class<C>
   val location: File
   val locationUser: File
   private val idImpl: String
   private val nameImpl: String
   private val description: String
   private val version: String
   private val author: String
   private val contributor: String
   private val howto: String
   private val year: String
   private val notes: String
   private val group: Widget.Group

   /** Whether this factory will be preferred on widget `find and create` requests. */
   var isPreferred = false

   /** Whether this factory will be ignored on widget `find and create` requests. */
   var isIgnored = false

   /**
    * @param controllerType of the controller of the widget this factory will create
    * @param location parent directory of the widget
    */
   constructor(controllerType: KClass<C>, location: File) {
      val i: Widget.Info = controllerType.findAnnotation() ?: WidgetFactory::class.findAnnotation()!!
      this.controllerType = controllerType.java
      this.location = location
      this.locationUser = APP.location.user.widgets/location.nameOrRoot
      this.idImpl = controllerType.simpleName ?: controllerType.jvmName
      this.nameImpl = i.name.nullIfBlank() ?: idImpl
      this.description = i.description
      this.version = i.version
      this.author = i.author
      this.contributor = i.contributor
      this.howto = i.howto
      this.year = i.year
      this.notes = i.notes
      this.group = i.group
   }

   override val id get() = idImpl
   override val name get() = nameImpl
   override fun description() = description
   override fun version() = version
   override fun author() = author
   override fun contributor() = contributor
   override fun year() = year
   override fun howto() = howto
   override fun notes() = notes
   override fun group() = group
   override fun type() = controllerType

   override fun create(): Widget = Widget(this)

   override fun toString() = "${javaClass.simpleName} $id $name $controllerType"

}

/** Component factory that creates component programmatically using a supplier. */
class TemplateFactory<C: Component>(override val name: String, private val supplier: () -> C): ComponentFactory<C> {
   override fun create() = supplier()
   override fun toString() = "${javaClass.simpleName} $name"
}

/** Component factory that creates component by deserializing it from file. */
class DeserializingFactory(val launcher: File): ComponentFactory<Component> {
   override val name = launcher.nameWithoutExtension

   override fun create() = APP.windowManager.instantiateComponent(launcher)!!
   override fun toString() = "${javaClass.simpleName} $name $launcher"
}

class NoFactoryFactory(val factoryId: String): WidgetFactory<NoFactoryController>(NoFactoryController::class, APP.location.widgets/factoryId.decapitalize()) {
   override val id = factoryId
   override val name = factoryId

   override fun create(): Widget = Widget(this)
   override fun toString() = "${javaClass.simpleName} $factoryId"
   fun createController(widget: Widget) = NoFactoryController(widget)
}

/** Marks [Controller]/[Widget] as unfit for production use. */
annotation class ExperimentalController(val reason: String)

/** @see ExperimentalController */
fun ComponentFactory<*>.isExperimental() = this is WidgetFactory<*> && controllerType.isAnnotationPresent(ExperimentalController::class.java)