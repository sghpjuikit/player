package node

import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javafx.beans.value.WritableValue
import javafx.event.EventDispatcher
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Skin
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.stage.Window
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmName
import mu.KLogging
import sp.it.pl.core.CoreMenus
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.WidgetTag
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.AppEventLog
import sp.it.pl.main.IconUN
import sp.it.pl.main.emScaled
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.collections.filterNotNullValues
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfigDef
import sp.it.util.conf.Configurable
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.PropertyConfig
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.valuesUnsealed
import sp.it.util.dev.fail
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.div
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.toCompactS
import sp.it.util.file.properties.PropVal
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.text.appendSent
import sp.it.util.text.nameUi
import sp.it.util.text.split2Partial
import sp.it.util.text.splitNoEmpty
import sp.it.util.type.VType
import sp.it.util.type.forEachJavaFXProperty
import sp.it.util.type.isSubclassOf
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.raw
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

   val nodeInstance by cv(NodeInstance(null))
      .def(name = "Node", info = "The javafx.scene.Node ui element or null if none")

   init {
      node sync { c ->
         val isDifferentClass = nodeInstance.value.node?.net { it::class.java.name }!=c
         if (isDifferentClass)
            nodeInstance.value = runTry {
               val type = Class.forName(c).kotlin
               val typeBuilder = APP.instances.recommendedNodeClassesAsWidgets.find { b -> b.type == type }
               val instance = typeBuilder?.constructor?.invoke() ?: type.takeIf(Node::class::isSuperclassOf)?.createInstance() as Node?
               widget.fieldsRaw["node"] = PropVal.PropVal1(type.jvmName)

               val nInstance = NodeInstance(instance)

               nodeInstance.value.configurableJson?.value.ifNotNull { configs ->
                  nInstance.getConfigs().forEach { config ->
                     if (config.isPersistable()) {
                        if (config.name in configs) {
                           APP.serializerJson.json.fromJsonValue(config.type, configs[config.name]!!)
                              .ifOk { config.value = it }
                              .ifError { logger.warn(it) { "Failed to deserialize config ${config.name}:${config.type}" } }
                        }
                     }
                  }
               }

               nInstance
            }
            .ifError {
               if (it !is ClassNotFoundException && it !is ClassNotFoundException)
                  AppEventLog.push(AppError("Failed to instantiate $c", "Reason: ${it.stacktraceAsString}"))
            }
            .getOrSupply {
               NodeInstance(null)
            }
      }
   }

   init {
      root.prefSize = 500.emScaled x 500.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()

      root.onEventDown(MOUSE_CLICKED, PRIMARY) {
         if (nodeInstance.value.node==null)
            APP.windowManager.showSettings(widget, root)
      }
      root.onEventDown(MOUSE_CLICKED, SECONDARY) {
         if (it.isStillSincePress)
            ContextMenu().dsl {
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
                              SelectionMenuItem(p.name + ": " + p.type.toUi(), p.name in propertiesWithInputs).apply {
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
            }.show(root, it)
      }

      nodeInstance sync { node ->
         io.i.removeAll()
         restoreInputs()
         storeInputs()

         root.children setTo listOfNotNull(node.node)
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

      override fun init() {
         APP.converter.general.addParser(NodeInstance::class, NodeInstance)
      }

      override fun dispose() {
         APP.converter.general.parsersFromS -= NodeInstance::class
         APP.converter.general.parsersToS -= NodeInstance::class
      }

      fun Node?.javaFXSuperClasses(): Sequence<KClass<*>> = when(this) {
         null -> sequenceOf()
         else -> this::class.superKClassesInc().filter { !it.java.isInterface }
      }

      @Suppress("SimplifyBooleanWithConstants")
      fun Node?.javaFxProperties(): Sequence<NodeInput> = when(this) {
         null -> sequenceOf()
         else -> forEachJavaFXProperty(this)
            .filter {
               true
                  && !it.isReadOnly
                  && !it.type.isSubtypeOf<EventHandler<*>>()
                  && !it.type.isSubtypeOf<EventDispatcher>()
                  && !it.type.isSubtypeOf<Collection<*>>()
                  && !it.type.isSubtypeOf<Map<*, *>>()
                  && !it.type.raw.isSubclassOf<Skin<*>>()                           // not json persistable
                  && !it.type.isSubtypeOf<Node>()                                   // not json persistable
                  && !it.type.isSubtypeOf<Window>()                                 // not json persistable
                  && !it.type.isSubtypeOf<ContextMenu>()                            // not json persistable
                  && !(it.type.isSubtypeOf<Boolean>() && it.name=="needsLayout")    // internals
                  && !(it.type.isSubtypeOf<Boolean>() && it.name=="managed")        // internals
            }
            .map { NodeInput(it.name, it.declaringClass, { it.observable().asIs() }, VType<Any?>(it.type)) }
      }

      data class NodeInstance(val node: Node?, val properties: List<NodeInput>, val configurable: Configurable<Any?>, val configurableJson: JsObject?): Configurable<Any?> by configurable {
         companion object: KLogging(), ConverterString<NodeInstance> {
            operator fun invoke(node: Node?): NodeInstance {
               val properties: List<NodeInput> = node.javaFxProperties().toList()
               val configurable = ListConfigurable.homogeneous(
                  properties.map { PropertyConfig(it.type, it.name, ConfigDef(it.name, "", "instance"), setOf(), it.value.asIs(), it.value.value, "instance") }
               )
               return NodeInstance(node, properties, configurable, null)
            }
            override fun ofS(s: String) =
               APP.serializerJson.json.fromJson<JsObject>(s).mapError { it.message ?: "" }.map {
                  NodeInstance(null, listOf(), ListConfigurable.homogeneous(), it)
               }
            override fun toS(o: NodeInstance) =
               JsObject(
                  o.configurable.getConfigs().associate {
                     it.name to runTry { APP.serializerJson.json.toJsonValue(it.type, it.value) }
                        .ifError { e -> logger.warn(e) { "Failed to persist ${o.node?.javaClass}.${it.name}" } }
                        .orNull()
                  }.filterNotNullValues()
               ).toCompactS()
         }
      }

      data class NodeInput(val name: String, val declaringClass: KClass<*>, val valueSupplier: () -> WritableValue<*>, val type: VType<*>) {
         val value: WritableValue<Any?> by lazy { valueSupplier().asIs() }
      }

      fun String.ifNotEmpty(mapper: (String) -> String) = if (isEmpty()) "" else mapper(this)
      fun String.unquote() = if (startsWith("\"") && endsWith("\"")) drop(1).dropLast(1) else fail { "Must be quoted" }
      fun String.quote() = "\"$this\""
      fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(toByteArray())
      fun String.decodeBase64() = String(Base64.getDecoder().decode(toByteArray()))
   }

}