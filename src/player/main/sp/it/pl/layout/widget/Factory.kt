package sp.it.pl.layout.widget

import de.jensd.fx.glyphs.GlyphIcons
import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.controller.NoFactoryController
import sp.it.pl.main.APP
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.file.div
import sp.it.util.file.nameOrRoot
import sp.it.util.functional.asIf
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.text.nullIfBlank
import sp.it.util.text.split3
import java.io.File
import java.time.Year
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.jvmName
import sp.it.util.Locatable

/** Component factory that creates component by deserializing it from file. */
interface ComponentFactory<out T: Component>: ComponentInfo {
   fun create(): T
}

/** Component factory that creates widgets. */
@Widget.Info
open class WidgetFactory<C: Controller>: ComponentFactory<Widget>, WidgetInfo, Locatable {

   override val id: String
   override val name: String
   override val icon: GlyphIcons?
   override val description: String
   override val descriptionLong: String
   override val version: KotlinVersion
   override val isSupported: Boolean
   override val author: String
   override val contributor: String
   override val year: Year
   override val group: Widget.Group
   override val type: KClass<*>
   override val summaryActions: List<ShortcutPane.Entry>
   override val location: File
   override val userLocation: File
   /** [KClass] of the controller created by [create]. */
   val controllerType: KClass<C>
   /** Whether this factory will be preferred on widget `find and create` requests. */
   var isPreferred = false
   /** Whether this factory will be ignored on widget `find and create` requests. */
   var isIgnored = false

   /**
    * @param controllerType of the controller of the widget this factory will create
    * @param location parent directory of the widget
    */
   constructor(controllerType: KClass<C>, location: File) {
      val info = controllerType.companionObjectInstance?.asIf<WidgetInfo>()
      val i: Widget.Info = null
         ?: controllerType.findAnnotation()
         ?: WidgetFactory::class.findAnnotation()!!
      this.controllerType = controllerType
      this.location = location
      this.userLocation = APP.location.user.widgets/location.nameOrRoot
      this.id = controllerType.simpleName ?: controllerType.jvmName
      this.name = info?.name ?: i.name.nullIfBlank() ?: id
      this.icon = info?.icon
      this.description = info?.description ?: i.description
      this.descriptionLong = (info?.descriptionLong ?: i.howto) + "\n" + i.notes
      this.version = info?.version ?: runTry { i.version.split3(".").let { (a,b,c) -> KotlinVersion(a.toInt(), b.toInt(), c.toInt()) } }.orNull() ?: KotlinVersion(0, 0, 0)
      this.author = info?.author ?: i.author
      this.contributor = info?.contributor ?: i.contributor
      this.year = info?.year ?: i.year.toIntOrNull()?.let { Year.of(it) } ?: Year.now()
      this.isSupported = info?.isSupported ?: true
      this.group = info?.group ?: i.group
      this.type = this.controllerType
      this.summaryActions = info?.summaryActions.orEmpty()
   }

   override fun create(): Widget = Widget(UUID.randomUUID(), this, false)

   fun createRecompiled(id: UUID): Widget = Widget(id, this, true)

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
   override val summaryActions = listOf<ShortcutPane.Entry>()

   override fun toString() = "${javaClass.simpleName} $factoryId"

   fun createController(widget: Widget) = NoFactoryController(widget)
}

/** Marks [Controller]/[Widget] as unfit for production use. */
annotation class ExperimentalController(val reason: String)

/** @see ExperimentalController */
fun ComponentFactory<*>.isExperimental() = this is WidgetFactory<*> && controllerType.hasAnnotation<ExperimentalController>()