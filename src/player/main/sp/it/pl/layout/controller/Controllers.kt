package sp.it.pl.layout.controller

import javafx.geometry.Pos.CENTER
import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.initialTemplateFactory
import sp.it.pl.layout.isCompiling
import sp.it.pl.main.APP
import sp.it.pl.main.appProgressIndicator
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Interpolators.Companion.geomElastic
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.hBox
import sp.it.util.ui.hyperlink
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.vBox
import sp.it.util.units.millis
import javafx.scene.robot.Robot
import mu.KLogging
import sp.it.pl.layout.WidgetTag
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.IconUN
import sp.it.pl.ui.objects.picker.ContainerPicker
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.util.async.runFX
import sp.it.util.conf.Config
import sp.it.util.ui.centre
import sp.it.util.ui.stackPane
import sp.it.util.ui.toPoint2D
import sp.it.util.units.version
import sp.it.util.units.year
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javafx.scene.control.ContextMenu
import javafx.scene.input.MouseButton.SECONDARY
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmName
import sp.it.pl.core.CoreMenus
import sp.it.pl.layout.NodeInput
import sp.it.pl.layout.WidgetNodeInstance
import sp.it.pl.main.AppError
import sp.it.pl.main.AppEventLog
import sp.it.pl.main.emScaled
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.contextmenu.MenuItemBoolean
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.collections.setTo
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.fail
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.json.JsNull
import sp.it.util.file.json.JsString
import sp.it.util.functional.asIf
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.consumeScrolling
import sp.it.util.text.appendSent
import sp.it.util.text.nameUi
import sp.it.util.text.split2Partial
import sp.it.util.text.splitNoEmpty
import sp.it.util.type.superKClassesInc
import sp.it.util.ui.dsl
import sp.it.util.ui.prefSize
import sp.it.util.ui.x

/** Controller for [Widget] with no [sp.it.pl.layout.WidgetFactory]. */
class ControllerNoFactory(widget: Widget): SimpleController(widget) {
   init {
      root.lay += vBox(5, CENTER) {
         lay += label("Widget ${widget.name} is not recognized")
         lay += compileInfoUi()
      }
   }
}

/** Controller for [Widget] that fails to instantiate its controller. */
class ControllerLoadError(widget: Widget): SimpleController(widget) {
   init {
      root.lay += vBox(5, CENTER) {
         lay += label("Widget ${widget.name} failed to load properly")
         lay += compileInfoUi()
         lay += hyperlink("Reload") {
            onEventDown(MOUSE_CLICKED, PRIMARY) { APP.widgetManager.factories.recompile(widget.factory) }
         }
      }
   }
}

private fun SimpleController.compileInfoUi(): Node {
   val isCompiling = widget.factory.isCompiling(onClose)
   return hBox(10, CENTER) {
      lay += label("Compiling...").apply {
         val a = anim { setScaleXY(it*it) }.delay(500.millis).dur(500.millis).intpl(geomElastic()).applyNow()
         isCompiling sync { if (it) a.playOpen() else a.playClose() } on onClose
      }
      lay += appProgressIndicator().apply {
         isCompiling sync { progress = if (it) -1.0 else 1.0 } on onClose
      }
   }
}

/** Controller for empty [Widget]. Useful for certain layout operations and as a fill in for null. */
class ControllerEmpty(widget: Widget): Controller(widget) {

   private val root = Pane()

   override fun uiRoot() = root
   override fun focus() {}
   override fun close() {}
   override fun getConfigs() = emptyList<Config<Any?>>()
   override fun getConfig(name: String) = null

   companion object: WidgetCompanion, KLogging() {
      override val name = "Empty"
      override val description = "Empty widget with no content or functionality"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2014)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf<WidgetTag>()
      override val summaryActions = listOf<Entry>()
   }
}

/** Controller for intro [Widget]. Useful as initial content for new user. */
class ControllerIntro(widget: Widget): Controller(widget) {

   private val root = stackPane()

   override fun uiRoot() = root.apply {
      val p = Placeholder(IconUN(0x1f4c1), "Start with a simple click\n\nIf you are 1st timer, choose ${ContainerPicker.choiceForTemplate} > ${initialTemplateFactory.name}") {
         runFX(300.millis) {
            val c = widget.parent
            val i = c?.indexOf(widget)
            if (c!=null && i!=null) {
               val clickAt = root.localToScreen(root.layoutBounds).centre.toPoint2D()
               AppAnimator.closeAndDo(root) {
                  runFX(500.millis) {
                     c.removeChild(widget)
                     with(Robot()) {
                        mouseMove(clickAt)
                        mouseClick(PRIMARY)
                     }
                  }
               }
            }
         }
      }
      AppAnimator.applyAt(p, 0.0)
      p.showFor(root)
      AppAnimator.openAndDo(p) {}
   }

   override fun focus() {}
   override fun close() {}
   override fun getConfigs() = emptyList<Config<Any?>>()
   override fun getConfig(name: String) = null

   companion object: WidgetCompanion, KLogging() {
      override val name = "Intro"
      override val description = "Introductory help widget guiding user through initial steps"
      override val descriptionLong = "$description."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2020)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf<WidgetTag>()
      override val summaryActions = listOf<Entry>()
   }
}

/** Controller for [Node]-based [Widget]. Useful to wrap ordinary content into a widget. */
class ControllerNode(widget: Widget): SimpleController(widget) {

   val node by cvn<String>(null)
      .valuesUnsealed { APP.instances.recommendedNodeClassesAsWidgets.map { it.type.jvmName } }
      .def(name = "Component class", info = buildString {
         appendSent("Fully qualified name of the kotlin.reflect.KClass of the javafx.scene.Node ui element or null if none")
         appendSent("The class needs public no argument constructor unless appropriate NodeFactory has been provided to an application.")
      })

   val nodeInstance by cv(WidgetNodeInstance(null))
      .def(name = "Node", info = "The javafx.scene.Node ui element or null if none")

   init {
      node sync { c ->
         val isDifferentClass = nodeInstance.value.node?.net { it::class.java.name }!=c
         if (isDifferentClass)
            nodeInstance.value = runTry {
               val type = Class.forName(c).kotlin
               val typeBuilder = APP.instances.recommendedNodeClassesAsWidgets.find { b -> b.type == type }
               val instance = typeBuilder?.constructor?.invoke() ?: type.takeIf(Node::class::isSuperclassOf)?.createInstance() as Node?
               widget.fieldsRaw["node"] = JsString(type.jvmName)

               val nInstance = WidgetNodeInstance(instance)

               nodeInstance.value.configurableJson?.value.ifNotNull { configs ->
                  nInstance.getConfigs().forEach { config ->
                     if (config.isPersistable() && config.name in configs)
                        config.valueAsJson = configs[config.name] ?: JsNull
                  }
               }

               nInstance
            }
            .ifError {
               if (it !is ClassNotFoundException && it !is ClassNotFoundException)
                  AppEventLog.push(AppError("Failed to instantiate $c", "Reason: ${it.stacktraceAsString}"))
            }
            .getOrSupply {
               WidgetNodeInstance(null)
            }
      }
   }

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.consumeScrolling()
      root.onEventDown(MOUSE_CLICKED, PRIMARY) { if (nodeInstance.value.node==null) APP.windowManager.showSettings(widget, root) }

      nodeInstance sync { node ->
         io.i.removeAll()
         restoreInputs()
         storeInputs()

         root.children setTo listOfNotNull(node.node)
      }
   }

  fun buildContextMenu() = ContextMenu().dsl {
     menu("Inputs") {
        val node = nodeInstance.value.node
        val propertiesWithInputs = io.i.getInputs().asSequence().map { it.name }.toSet()
        val properties = nodeInstance.value.properties
        val propertiesByClass =  node.javaFXSuperClasses().associateWith { listOf<NodeInput>() } + properties.groupBy { it.declaringClass }
        val i = AtomicInteger(-1)
        propertiesByClass.forEach { (declaringClass, properties) ->
           val namePrefix = if (i.incrementAndGet()==0) "" else "".padStart(i.get(), ' ') + "⌎ "
           menu(namePrefix + declaringClass.toUi()) {
              properties.forEach { p ->
                 item {
                    MenuItemBoolean(p.name + ": " + p.type.toUi(), p.name in propertiesWithInputs).apply {
                       selected attachFalse {
                          io.i.getInputs().find { it.name==p.name }.ifNotNull {
                             io.i.remove(it)
                             storeInputs()
                          }
                       }
                       selected attachTrue {
                          io.i.create(p.name, p.type, p.value.value) {
                             p.value.value = it
                             storeInputs()
                          }
                          storeInputs()
                       }
                    }
                 }
              }
           }
        }
     }
     items {
        CoreMenus.menuItemBuilders[this@ControllerNode.widget]
     }
  }

   fun storeInputs() {
      widget.properties["node-widget-inputs"] = io.i.getInputs().joinToString("-") {
         it.name.encodeBase64() + "|" + if (it.isBoundUnless()) "" else it.value.toS().quote().encodeBase64()
      }
   }

   fun restoreInputs() {
      widget.properties["node-widget-inputs"].asIf<String>().orEmpty().splitNoEmpty("-").forEach {
         val (propertyNameBase64, propertyValueBase64) = it.split2Partial("|")
         val (propertyName, propertyValueS) = propertyNameBase64.decodeBase64() to propertyValueBase64.ifNotEmpty { it.decodeBase64().unquote() }
         val properties = nodeInstance.value.properties
         val property = properties.firstOrNull { p -> p.name==propertyName }
         if (property!=null && io.i.getInputs().none { it.name==property.name }) {
            val propertyValue = when (propertyValueS) {
               "" -> property.value.value
               else -> APP.converter.general.ofS(property.type, propertyValueS).getOrSupply { property.value.value }
            }

            property.value.value = propertyValue
            io.i.create(property.name, property.type, propertyValue) { property.value.value = it }
         }
      }
   }

   override fun focus() {
      nodeInstance.value.node
         .ifNotNull { it.requestFocus() }
         .ifNull { root.requestFocus() }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Custom"
      override val description = "Displays custom component specified by ${Node::class.jvmName} class"
      override val descriptionLong = "$description. This avoids the need to create widget wrappers for ui components."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf<WidgetTag>()
      override val summaryActions = listOf(
         Entry("Node", "Edit component properties as widget inputs", SECONDARY.nameUi),
      )

      fun Node?.javaFXSuperClasses(): Sequence<KClass<*>> = when(this) {
         null -> sequenceOf()
         else -> this::class.superKClassesInc().filter { !it.java.isInterface }
      }

      fun String.ifNotEmpty(mapper: (String) -> String) = if (isEmpty()) "" else mapper(this)
      fun String.unquote() = if (startsWith("\"") && endsWith("\"")) drop(1).dropLast(1) else fail { "Must be quoted" }
      fun String.quote() = "\"$this\""
      fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(toByteArray())
      fun String.decodeBase64() = String(Base64.getDecoder().decode(toByteArray()))
   }

}