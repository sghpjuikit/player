package sp.it.util.conf

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.ObservableList
import sp.it.util.access.OrV
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfList.Companion.FailFactory
import sp.it.util.conf.ConfigImpl.ConfigBase
import sp.it.util.conf.Constraint.ReadOnlyIf
import sp.it.util.access.OrV.OrValue
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.file.properties.PropVal
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.file.properties.PropVal.PropValN
import sp.it.util.functional.asIs
import sp.it.util.functional.compose
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.parsing.Parsers
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.type.VType
import sp.it.util.type.type
import java.util.HashSet
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KTypeProjection.Companion.covariant
import kotlin.reflect.KTypeProjection.Companion.invariant
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.isAccessible
import sp.it.util.conf.Constraint.ValueUnsealedSet
import sp.it.util.functional.asIf
import sp.it.util.functional.ifNotNull
import sp.it.util.type.jvmErasure

interface ConfigImpl {

   abstract class ConfigBase<T> constructor(
      override val type: VType<T>,
      override val name: String,
      override val nameUi: String,
      override val defaultValue: T,
      override val group: String,
      override val info: String,
      override val isEditable: EditMode
   ): Config<T>() {

      private var constraintsImpl: HashSet<Constraint<T>>? = null

      override val constraints
         get() = constraintsImpl.orEmpty()

      constructor(type: VType<T>, name: String, c: ConfigDefinition, constraints: Set<Constraint<T>>, `val`: T, group: String): this(type, name, c.name.ifEmpty { name }, `val`, group, c.info, c.editable) {
         constraintsImpl = if (constraints.isEmpty()) null else HashSet(constraints)

         type.jvmErasure.companionObjectInstance.asIf<UnsealedEnumerator<T>>()?.ifNotNull {
            addConstraints(ValueUnsealedSet { it.enumerateUnsealed() })
         }
      }

      @SafeVarargs
      override fun addConstraints(vararg constraints: Constraint<T>): ConfigBase<T> {
         if (constraintsImpl==null) constraintsImpl = constraints.toHashSet()
         else constraintsImpl!!.addAll(constraints)
         return this
      }
   }

}

/** [Config] wrapping a value. Not observable. */
class ValueConfig<V>: ConfigBase<V> {
   private var value: V

   constructor(type: VType<V>, name: String, nameUi: String, value: V, group: String, info: String, editable: EditMode): super(type, name, nameUi, value, group, info, editable) {
      this.value = value
   }

   constructor(type: VType<V>, name: String, value: V, info: String): super(type, name, name, value, "", info, EditMode.USER) {
      this.value = value
   }

   @Suppress("UNCHECKED_CAST")
   override fun getValue(): V {
      return value
   }

   override fun setValue(value: V) {
      this.value = value
   }

}

/** [Config] that does not store value, instead uses the getter and setter which provide/accept it. Not observable. */
class AccessConfig<T>: ConfigBase<T>, WritableValue<T> {
   private val setter: (T) -> Unit
   private val getter: () -> T

   /**
    * @param setter defines [setValue]
    * @param getter defines [getValue]
    */
   constructor(
      type: VType<T>, name: String, gui_name: String, setter: (T) -> Unit, getter: () -> T, group: String, info: String, editable: EditMode
   ): super(type, name, gui_name, getter(), group, info, editable) {
      this.getter = getter
      this.setter = setter
   }

   /**
    * @param setter defines [setValue]
    * @param getter defines [getValue]
    */
   constructor(
      type: VType<T>, name: String, description: String, setter: (T) -> Unit, getter: () -> T
   ): super(type, name, name, getter(), "", description, EditMode.USER) {
      this.getter = getter
      this.setter = setter
   }

   override fun getValue() = getter()

   override fun setValue(value: T) {
      setter(value)
   }

}

/** [Config] backed by [KProperty] and an object instance. Can wrap both static or instance fields. */
open class FieldConfig<T>(
   type: VType<T>, name: String, def: ConfigDefinition, constraints: Set<Constraint<T>>, private val instance: Any?, group: String, private val property: KProperty<T>
): ConfigBase<T>(
   // type can not be obtained from property, as it could be platform type and have ambiguous nullability
   type, name, def, constraints, property.getter.call(instance), group
) {

   override fun getValue(): T {
      return runTry {
         property.isAccessible = true
         property.getter.isAccessible = true
         property.getter.call(instance)
      }.getOrSupply {
         fail(it) { "Error getting config $group.$name's property $property value" }
      }
   }

   override fun setValue(value: T) {
      if (property is KMutableProperty<T>) {
         runTry {
            property.isAccessible = true
            property.setter.isAccessible = true
            property.setter.call(instance, value)
         }.getOrSupply {
            fail(it) { "Error setting config $group.$name's property $property value" }
         }
      }
   }

}

@Suppress("UNCHECKED_CAST")
open class PropertyConfig<T>(
   valueType: VType<T>, name: String, def: ConfigDefinition, constraints: Set<Constraint<T>>, val property: WritableValue<T>, defaultValue: T = property.value, group: String
): ConfigBase<T>(valueType, name, def, constraints, defaultValue, group) {

   override fun getValue(): T = property.value

   override fun setValue(value: T) {
      property.value = value
   }

}

@Suppress("UNCHECKED_CAST")
open class PropertyConfigRO<T>(
   valueType: VType<T>, name: String, c: ConfigDefinition, constraints: Set<Constraint<T>>, val property: ObservableValue<T>, group: String
): ConfigBase<T>(valueType, name, c, constraints, property.value, group) {

   override fun getValue(): T = property.value

   override fun setValue(value: T) {}

}

/** [Config] backed by [OrV] and of [OrValue] type. Observable. */
open class OrPropertyConfig<T>: ConfigBase<OrValue<T>> {
   val property: OrV<T>
   val valueType: VType<T>
   val elementConstraints: MutableSet<Constraint<T>>

   constructor(
      valueType: VType<T>, name: String, c: ConfigDefinition, constraints: Set<Constraint<OrValue<T>>>, elementConstraints: Set<Constraint<T>>, property: OrV<T>, group: String
   ): super(
      VType(OrValue::class.createType(listOf(invariant(valueType.type)))),
      name, c, constraints, OrValue(property.override.value, property.real.value), group
   ) {
      this.property = property
      this.valueType = valueType
      this.elementConstraints = elementConstraints.toMutableSet()
   }

   override fun getValue() = OrValue(property.override.value, property.real.value)

   override fun setValue(value: OrValue<T>) {
      property.real.value = value.value
      property.override.value = value.override
   }

   fun constrainOverridden(block: ConstrainedDsl<T>.() -> Unit): ConfigBase<OrValue<T>> {
      ConstrainedDsl(elementConstraints::add).block()
      return this
   }

   override var valueAsProperty: PropVal
      get() = PropValN(
         listOf(
            Parsers.DEFAULT.toS(property.override.value),
            Parsers.DEFAULT.toS(property.real.value)
         )
      )
      set(v) {
         val s = v.valN
         if (s.size==2) {
            Parsers.DEFAULT.ofS<Boolean>(s[0])
               .ifOk { property.override.value = it }
               .ifError { logger.warn(it) { "Unable to set config=$name override value (Boolean.class) from text='${s[0]}'" } }
            Parsers.DEFAULT.ofS(valueType, s[1])
               .ifOk { property.real.value = it }
               .ifError { logger.warn(it) { "Unable to set config=$name real value ($valueType) from text='${s[0]}'" } }
         } else {
            logger.warn { "Unable to set config=$name value from property='$s', must have 2 values" }
         }
      }
}

@Suppress("UNCHECKED_CAST")
open class ListConfig<T>(
   name: String, def: ConfigDefinition, val a: ConfList<T>, group: String, constraints: Set<Constraint<ObservableList<T>>>, elementConstraints: Set<Constraint<T>>
): ConfigBase<ObservableList<T>>(
   VType(ObservableList::class.createType(nullable = false, arguments = listOf(invariant(a.itemType.type)))),
   name, def, constraints, observableArrayList(a.list.materialize()), group
) {

   val toConfigurable: (T?) -> Configurable<*>

   // TODO: support multi-value
   override var valueAsProperty: PropVal
      get() = PropValN(
         value.map {
            a.itemToConfigurable(it).getConfigs().joinToString(";") {
               it.valueAsProperty.val1 ?: fail { "Config $name supports only single-value within multi value" }
            }
         }
      )
      set(property) {
         val isFixedSizeAndHasConfigurableItems = isFixedSizeAndHasConfigurableItems
         a.list setTo property.valN.asSequence()
            .mapIndexed { i, s ->
               val item = if (isFixedSizeAndHasConfigurableItems) a.list[i] else a.itemFactory?.invoke()
               val configs = a.itemToConfigurable(item).getConfigs().toList().materialize()
               val values = s.split(";")
               if (configs.size==values.size)
                  (configs zip values).forEach { (c, v) -> c.valueAsProperty = PropVal1(v) }

               if (a.isSimpleItemType) (configs[0].value as T)
               else item
            }
            .filter(if (a.itemType.isNullable) { _ -> true } else { it -> it!=null })
      }

   private val isFixedSizeAndHasConfigurableItems: Boolean = a.itemFactory===FailFactory

   init {
      failIf(isReadOnly(null, a.list)!=def.editable.isByNone)

      toConfigurable = a.itemToConfigurable.compose { configurable ->
         if (configurable is Config<*>) {
            val config = configurable.asIs<Config<T>>()
            config.addConstraints(elementConstraints)
            if (!isEditable.isByUser) config.addConstraints(ReadOnlyIf(true))
         }
         configurable
      }
   }

   override fun getValue() = a.list

   fun setValue(value: List<T>) {
      if (value!==a.list)
         a.list setTo value
   }

   override fun setValue(value: ObservableList<T>) = setValue(value as List<T>)

   @Suppress("SimplifyBooleanWithConstants")
   companion object {
      internal fun isReadOnly(type: KClass<*>?, list: ObservableList<*>?): Boolean = false
         || type?.java?.simpleName?.contains("unmodifiable", true)==true
         || list?.javaClass?.simpleName?.contains("unmodifiable", true)==true
   }

}

class CheckListConfig<T, S: Boolean?>(
   name: String,
   def: ConfigDefinition,
   defaultValue: CheckList<T, S>,
   group: String,
   constraints: Set<Constraint<CheckList<T, S>>>
): ConfigBase<CheckList<T, S>>(
   VType(
      CheckList::class.createType(arguments = listOf(
         covariant(defaultValue.elementType.type),
         invariant(defaultValue.checkType.type))
      )
   ),
   name,
   def,
   constraints,
   defaultValue,
   group
), ReadOnlyProperty<ConfigDelegator, CheckList<T, S>> {
   override fun getValue(thisRef: ConfigDelegator, property: KProperty<*>): CheckList<T, S> = value
   override fun getValue(): CheckList<T, S> = defaultValue
   override fun setValue(value: CheckList<T, S>) {}
   override fun setValueToDefault() = value.selections setTo value.selectionsInitial

   @Suppress("UNCHECKED_CAST")
   override var valueAsProperty: PropVal
      get() = PropValN(
         value.selections.map { Parsers.DEFAULT.toS(it) }
      )
      set(v) {
         if (value.all.size==v.size()) {
            value.selections setTo v.valN.mapIndexed { i, text ->
               Parsers.DEFAULT.ofS(value.checkType, text)
                  .ifError { logger.warn(it) { "Unable to set config=$name element=$i from=$text" } }
                  .orNull() ?: value.selections[i]
            }
         } else {
            logger.warn { "Unable to set config=$name from=$v: element count mismatch" }
         }
      }
}

class CheckList<out T, S: Boolean?> private constructor(
   val checkType: VType<S>,
   val elementType: VType<T>,
   val all: List<T>,
   val selectionsInitial: List<S>
): Observable {
   val selections = observableArrayList(selectionsInitial)!!.apply {
      onChangeAndNow {
         failIf(all.size!=size) { "Selections and elements counts must always be the same " }
      }
   }

   fun forEach(block: (Pair<T, S>) -> Unit): Unit = (all zip selections).forEach(block)
   fun forEachIndexed(block: (Triple<Int, T, S>) -> Unit): Unit = all.indices.forEach { block(Triple(it, all[it], selections[it])) }
   fun selected(s: S): List<T> = all.filterIndexed { i, _ -> selections[i]==s }
   override fun addListener(listener: InvalidationListener?) = selections.addListener(listener)
   override fun removeListener(listener: InvalidationListener?) = selections.removeListener(listener)

   companion object {
      fun <T> nonNull(elementType: VType<T>, elements: List<T>, selections: List<Boolean> = elements.map { true }): CheckList<T, Boolean> = CheckList(type(), elementType, elements, selections)
      fun <T> nullable(elementType: VType<T>, elements: List<T>, selections: List<Boolean?> = elements.map { true }): CheckList<T, Boolean?> = CheckList(type(), elementType, elements, selections)
   }
}