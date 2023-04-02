package sp.it.pl.layout

import javafx.beans.value.WritableValue
import javafx.event.EventDispatcher
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Skin
import javafx.stage.Window
import kotlin.reflect.KClass
import mu.KLogging
import sp.it.util.conf.ConfigDef
import sp.it.util.conf.Configurable
import sp.it.util.conf.ListConfigurable
import sp.it.util.conf.PropertyConfig
import sp.it.util.file.json.JsConverter
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsValue
import sp.it.util.functional.asIs
import sp.it.util.type.VType
import sp.it.util.type.forEachJavaFXProperty
import sp.it.util.type.isSubclassOf
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.raw

data class WidgetNodeInstance(val node: Node?, val properties: List<NodeInput>, val configurable: Configurable<Any?>, val configurableJson: JsObject?): Configurable<Any?> by configurable {

   companion object: KLogging(), JsConverter<WidgetNodeInstance> {

      operator fun invoke(node: Node?): WidgetNodeInstance {
         val properties = node.javaFxProperties().toList()
         val configs = properties.map { PropertyConfig(it.type, it.name, ConfigDef(it.name, "", "instance"), setOf(), it.value.asIs(), it.value.value, "instance") }
         val configurable = ListConfigurable.homogeneous(configs)
         return WidgetNodeInstance(node, properties, configurable, null)
      }

      override fun fromJson(value: JsValue) =
         WidgetNodeInstance(null, listOf(), ListConfigurable.homogeneous(), value.asJsObject())

      override fun toJson(value: WidgetNodeInstance) =
         JsObject(value.configurable.getConfigs().associate { it.name to it.valueAsJson })

      @Suppress("SimplifyBooleanWithConstants")
      private fun Node?.javaFxProperties(): Sequence<NodeInput> = when(this) {
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
   }

}

data class NodeInput(val name: String, val declaringClass: KClass<*>, val valueSupplier: () -> WritableValue<*>, val type: VType<*>) {
   val value: WritableValue<Any?> by lazy { valueSupplier().asIs() }
}