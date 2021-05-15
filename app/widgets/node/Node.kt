package node

import java.util.Base64
import javafx.beans.value.WritableValue
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSuperclassOf
import mu.KLogging
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.APP
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.IconUN
import sp.it.pl.main.emScaled
import sp.it.pl.main.toS
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.vn
import sp.it.util.collections.setTo
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.util.functional.asIf
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.text.nameUi
import sp.it.util.text.split2Partial
import sp.it.util.text.splitTrimmed
import sp.it.util.type.VType
import sp.it.util.type.forEachJavaFXProperty
import sp.it.util.ui.dsl
import sp.it.util.ui.prefSize
import sp.it.util.ui.show
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class Node(widget: Widget): SimpleController(widget) {

   /** The fully qualified name of the class of the node or null if none */
   private val node by cvn<String>(null).def(name = "Component class", info = "Fully qualified name of the kotlin.reflect.KClass of the javafx.scene.Node component. Needs public no argument constructor.")
   /** The node instance or null if none */
   private val nodeInstance = vn<Node>(null).apply {
      node sync {
         val isDifferentClass = value?.net { it::class.java.name }!=it
         if (isDifferentClass) value = runTry { Class.forName(it)?.kotlin?.takeIf(Node::class::isSuperclassOf)?.createInstance() as Node? }.orNull()
      }
   }

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()

      root.onEventDown(MOUSE_CLICKED, SECONDARY) {
         if (it.isStillSincePress)
            ContextMenu().dsl {
               menu("Inputs") {
                  val propertiesWithInputs = io.i.getInputs().asSequence().map { it.name }.toSet()
                  val properties = nodeInstance.value.javaFxProperties()
                  properties.forEach { p ->
                     item {
                        SelectionMenuItem(p.name, p.name in propertiesWithInputs).apply {
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
            }.show(root, it)
      }

      nodeInstance sync { node ->
         io.i.removeAll()
         restoreInputs()
         storeInputs()

         root.children setTo listOfNotNull(node)
      }
   }

   fun storeInputs() {
      widget.properties["node-widget-inputs"] = io.i.getInputs().joinToString("-") {
         it.name.encodeBase64() + "|" + if (it.isBound()) "" else it.value.toS().quote().encodeBase64()
      }
   }

   fun restoreInputs() {
      widget.properties["node-widget-inputs"].asIf<String>().orEmpty().splitTrimmed("-").forEach {
         val (propertyNameBase64, propertyValueBase64) = it.split2Partial("|")
         val (propertyName, propertyValueS) = propertyNameBase64.decodeBase64() to propertyValueBase64.ifNotEmpty { it.decodeBase64().unquote() }
         val properties = nodeInstance.value.javaFxProperties()
         val property = properties.firstOrNull { p -> p.name==propertyName }
         if (property!=null && io.i.getInputs().none { it.name==property.name }) {
            val propertyValue = when (propertyValueS) {
               "" -> property.value.value
               else -> sp.it.pl.main.APP.converter.general.ofS(property.type, propertyValueS).getOrSupply { property.value.value }
            }

            property.value.value = propertyValue
            io.i.create(property.name, property.type, propertyValue) { property.value.value = it }
         }
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Node"
      override val description = "Displays component specified by class"
      override val descriptionLong = "$description. This avoids the need to create widget wrappers for ui components."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf(
         ShortcutPane.Entry("Node", "Edit component properties as widget inputs", SECONDARY.nameUi),
      )
      override val group = APP

      fun Node?.javaFxProperties(): List<NodeInput> {
         if (this==null) return listOf()

         val properties = mutableListOf<NodeInput>()

         forEachJavaFXProperty(this) { observable, name, type ->
            if (observable is WritableValue<*>)
               properties += NodeInput(name, observable, VType<Any?>(type))
         }

         return properties
      }

      data class NodeInput(val name: String, val value: WritableValue<*>, val type: VType<*>)

      fun String.ifNotEmpty(mapper: (String) -> String) = if (isEmpty()) "" else mapper(this)
      fun String.unquote() = if (startsWith("\"") && endsWith("\"")) drop(1).dropLast(1) else fail { "Must be quoted" }
      fun String.quote() = "\"$this\""
      fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(toByteArray())
      fun String.decodeBase64() = String(Base64.getDecoder().decode(toByteArray()))
   }
}