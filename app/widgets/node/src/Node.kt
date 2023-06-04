package node

import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.input.ContextMenuEvent.CONTEXT_MENU_REQUESTED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmName
import mu.KLogging
import sp.it.pl.core.CoreMenus
import sp.it.pl.layout.NodeInput
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.WidgetNodeInstance
import sp.it.pl.layout.WidgetTag
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.AppEventLog
import sp.it.pl.main.IconUN
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
import sp.it.util.file.div
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
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.text.appendSent
import sp.it.util.text.nameUi
import sp.it.util.text.split2Partial
import sp.it.util.text.splitNoEmpty
import sp.it.util.type.superKClassesInc
import sp.it.util.ui.dsl
import sp.it.util.ui.prefSize
import sp.it.util.ui.show
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class Node(widget: Widget): SimpleController(widget) {

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
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()

      root.onEventDown(MOUSE_CLICKED, SECONDARY) { if (it.isPrimaryButtonDown && it.isStillSincePress) { buildContextMenu().show(root, it); it.consume() } }
      root.onEventDown(CONTEXT_MENU_REQUESTED) { if (it.isKeyboardTrigger) { buildContextMenu().show(root, it); it.consume() } }
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
        CoreMenus.menuItemBuilders[this@Node.widget]
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