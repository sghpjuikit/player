package sp.it.util.conf

import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import kotlin.reflect.full.companionObjectInstance
import mu.KLogging
import sp.it.util.access.OrV
import sp.it.util.access.vAlways
import sp.it.util.access.vx
import sp.it.util.conf.Constraint.NoPersist
import sp.it.util.conf.Constraint.ReadOnlyIf
import sp.it.util.conf.Constraint.ValueSealedSet
import sp.it.util.conf.Constraint.ValueSealedSetIfNotIn
import sp.it.util.conf.Constraint.ValueSealedSetIfNotIn.Strategy.USE
import sp.it.util.conf.Constraint.ValueSealedSetIfNotIn.Strategy.USE_AND_ADD
import sp.it.util.conf.Constraint.ValueSealedSetIfNotIn.Strategy.USE_DEFAULT
import sp.it.util.dev.Experimental
import sp.it.util.dev.fail
import sp.it.util.file.properties.PropVal
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.Option
import sp.it.util.functional.Option.None
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.invoke
import sp.it.util.functional.net
import sp.it.util.parsing.Parsers
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.enumValues
import sp.it.util.type.isEnum
import sp.it.util.type.isObject
import sp.it.util.type.isSubclassOf
import sp.it.util.type.raw
import sp.it.util.type.sealedSubObjects
import sp.it.util.type.type
import sp.it.util.type.typeOrNothing

/**
 * Represents of a configurable value.
 *
 * Config abstracts away access and provides description of a value. Composes into [Configurable].
 *
 * @param <T> type of value of this config
 */
abstract class Config<T>: WritableValue<T>, Configurable<T>, Constrained<T, Config<T>> {

   abstract override fun getValue(): T

   abstract override fun setValue(value: T)

   /** Name of this config. */
   abstract val name: String

   /** Human-readable [name]. Short, single line. */
   abstract val nameUi: String

   /** Semantic category/group path using '.' separator. */
   abstract val group: String

   /** Human-readable [group]. Short, single line. */
   val groupUi: String get() = group.split(".").joinToString(" > ")

   /** Human-readable description. Potentially multiple lines. */
   abstract val info: String

   /** Editability. This can be further restricted with constraints. */
   abstract val isEditable: EditMode

   /** Name of this config. */
   abstract val type: VType<T>

   fun isNotEditableRightNow() = constraints.any { it is ReadOnlyIf && it.condition.value }

   fun isEditableByUserRightNow() = isEditable.isByUser && !isNotEditableRightNow()

   fun isEditableByUserRightNowProperty(): ObservableValue<Boolean> {
      return when {
         !isEditable.isByUser -> vAlways(false)
         else -> {
            val readOnlyIfs = findConstraints<ReadOnlyIf>().map { it.condition }.toList()
            when {
               readOnlyIfs.isEmpty() -> vAlways(true)
               else -> object: BooleanBinding() {
                  init { bind(*readOnlyIfs.toTypedArray()) }
                  override fun computeValue() = readOnlyIfs.none { it.value }
               }
            }
         }
      }
   }

   fun isPersistable() = isEditable.isByApp &&
      !hasConstraint<NoPersist>() &&
      !type.raw.net { it==Void::class || it==Nothing::class || (type.isNotNull && (it==Unit::class || it==Nothing::class || it.isObject)) }

   /** Limits put on this value or markers that signify certain treatment of it. */
   abstract val constraints: Set<Constraint<T>>

   @Experimental("Expert API, mutates state")
   abstract fun addConstraints(vararg constraints: Constraint<T>): Config<T>

   @Experimental("Expert API, mutates state")
   override fun addConstraint(constraint: Constraint<T>): Config<T> = addConstraints(listOf(constraint))

   @Experimental("Expert API, mutates state")
   fun addConstraints(constraints: Collection<Constraint<T>>): Config<T> = addConstraints(*constraints.toTypedArray())

   inline fun <reified T> hasConstraint(): Boolean = findConstraints<T>().firstOrNull()!=null

   inline fun <reified T> findConstraint(): T? = findConstraints<T>().firstOrNull()

   inline fun <reified T> findConstraints(): Sequence<T> = constraints.asSequence().filterIsInstance<T>()

   /** Initial value. Sensible value and potential fallback when user provided value is invalid. */
   abstract val defaultValue: T

   open fun setValueToDefault() {
      value = defaultValue
   }

   open var valueAsProperty: PropVal
      get() {
         return PropVal1(Parsers.DEFAULT.toS(value))
      }
      set(property) {
         val s = property.val1
         if (s!=null)
            convertValueFromString(this, s)
               .ifOk { value = it }
               .ifError { logger.warn { "Unable to set config=$name value from text=$s because=$it" } }
      }

   protected var valueEnumerator2nd: MutableList<T>? = null

   @Suppress("UNCHECKED_CAST")
   protected val valueEnumerator: Enumerator<T>? by lazy {
      null
         ?: findConstraint<ValueSealedSet<T>>()?.let { values ->
            Enumerator { values.enumerateSealed() + valueEnumerator2nd.orEmpty() }
         }
         ?: if (!type.raw.isEnum) null else {
            val values = type.raw.enumValues.toList()
            if (type.isNullable) Enumerator { values + valueEnumerator2nd.orEmpty() + (null as T) }
            else Enumerator { values + valueEnumerator2nd.orEmpty() }
         }
         ?: if (!type.raw.isSealed || !type.raw.sealedSubclasses.all { it.isObject }) null else {
            val values = type.raw.sealedSubObjects.asIs<List<T>>()
            if (type.isNullable) Enumerator { values + valueEnumerator2nd.orEmpty() + (null as T) }
            else Enumerator { values + valueEnumerator2nd.orEmpty() }
         }
         ?: (type.raw.companionObjectInstance as? SealedEnumerator<T>)?.net { e ->
            if (type.isNullable) Enumerator { e.enumerateSealed() + valueEnumerator2nd.orEmpty() + (null as T) }
            else Enumerator { e.enumerateSealed() + valueEnumerator2nd.orEmpty() }
         }
         ?: type.raw.objectInstance?.net { instance ->
            val values = if (type.isNullable) listOf(instance, null as T) else listOf(instance)
            Enumerator { values }.asIs()
         }
   }

   /** True iff [enumerateValues] returns a value. */
   @Suppress("UNCHECKED_CAST")
   val isEnumerable: Boolean
      get() =
         findConstraint<ValueSealedSet<T>>()!=null ||
         type.raw.isEnum ||
         (type.raw.isSealed && type.raw.sealedSubclasses.all { it.isObject }) ||
         (type.raw.companionObjectInstance as? SealedEnumerator<T>)!=null

   /** @return collection of values this config's value is usually within, see [ValueSealedSetIfNotIn] for exceptions */
   fun enumerateValues(): Collection<T> = valueEnumerator?.net { it() } ?: fail {
      "Config $name is not enumerable, because $type not enumerable or no value set was provided."
   }

   override fun getConfig(name: String) = takeIf { it.name==name }

   override fun getConfigs() = listOf(this)

   companion object: KLogging() {

      /** Helper method. Expert API. */
      @Suppress("MoveVariableDeclarationIntoWhen")
      fun <T> convertValueFromString(config: Config<T>, s: String): Try<T, String> {
         return if (config.isEnumerable) {
            // Instead of parsing the value, iterate through possible values and find the one with the same toS
            // Expensive, but always preserves object identity
            for (v in config.enumerateValues())
               if (Parsers.DEFAULT.toS(v).equals(s, true)) return Try.ok(v)

            val strategy = config.findConstraint<ValueSealedSetIfNotIn>()?.strategy ?: USE_DEFAULT
            when (strategy) {
               USE_AND_ADD -> Parsers.DEFAULT.ofS(config.type, s).ifOk {
                  if (config.valueEnumerator2nd == null) config.valueEnumerator2nd = mutableListOf(it)
                  else config.valueEnumerator2nd!! += it
               }
               USE -> Parsers.DEFAULT.ofS(config.type, s)
               USE_DEFAULT -> Try.ok(config.defaultValue)
            }
         } else {
            Parsers.DEFAULT.ofS(config.type, s)
         }
      }

      /**
       * Creates config for value. Attempts in order:
       *  *  [forProperty]
       *  *  create [ListConfig] if the type is subtype of [javafx.collections.ObservableList]
       *  *  wraps the value in [sp.it.util.access.V] and calls [forProperty]
       */
      fun <T> forValue(type: VType<T>, name: String, value: Any?): Config<T> = null
         ?: forPropertyImpl(type, name, value)
         ?: run {
            when {
               type.isSubclassOf<ObservableList<*>>() -> {
                  val valueTyped = value as ObservableList<*>
                  // TODO: use variance to get readOnly
                  val isReadOnly = if (ListConfig.isReadOnly(type.raw, valueTyped)) EditMode.NONE else EditMode.USER
                  val def = ConfigDef(name, "", "", editable = isReadOnly)
                  val itemType: VType<*> = VType<Any?>(type.type.argOf(ObservableList::class, 0).typeOrNothing)
                  ListConfig(name, def, ConfList(itemType, valueTyped.asIs()), "", setOf(), setOf()).asIs()
               }
               else -> forProperty(type, name, vx(value))
            }
         }

      /**
       * Creates config for property. Te property will become the underlying data
       * of the config and thus reflect any value changes and vice versa. If
       * the property is read only, config will also be read only (its set()
       * methods will not do anything). If the property already is config, it is
       * returned.
       *
       * @param name of the config, will be used as gui name
       * @param property underlying property for the config. The property must be an instance of any:
       *  *  [Config]
       *  *  [ConfList]
       *  *  [OrV]
       *  *  [WritableValue]
       *  *  [ObservableValue]
       *
       * so standard javafx properties will all work. If not instance of the above, runtime exception will be thrown.
       */
      fun <T> forProperty(type: VType<T>, name: String, property: Any?, initialValue: Option<T> = None): Config<T> = null
         ?: forPropertyImpl(type, name, property, initialValue)
         ?: fail { "Property $name must be WritableValue or ReadOnlyValue, but is ${property?.javaClass}" }

      inline fun <reified T> forProperty(name: String, property: Any?, initialValue: Option<T> = None): Config<T> = forProperty(type(), name, property, initialValue)

      @JvmStatic
      @JvmOverloads
      fun <T> forProperty(type: Class<T & Any>, name: String, property: Any?, isNullable: Boolean = false): Config<T> = forProperty(VType(type, isNullable), name, property)

      @Suppress("UNCHECKED_CAST")
      private fun <T> forPropertyImpl(type: VType<T>, name: String, property: Any?, initialValue: Option<T> = None): Config<T>? {
         val def = ConfigDef(name, "", group = "", editable = EditMode.USER)
         return when (property) {
            is Config<*> -> property.asIs()
            is ConfList<*> -> {
               val isReadOnly = if (ListConfig.isReadOnly(type.raw, property.list)) EditMode.NONE else EditMode.USER
               ListConfig(name, def.copy(editable = isReadOnly), property, "", setOf(), setOf()).asIs()
            }
            is OrV<*> -> OrPropertyConfig(type, name, def, setOf(), setOf(), property.asIs(), group = "").asIs()
            is WritableValue<*> -> PropertyConfig(type, name, def, setOf(), property.asIs(), group = "", defaultValue = initialValue.getOrSupply { property.value as T })
            is ObservableValue<*> -> PropertyConfigRO(type, name, def, setOf(), property.asIs(), group = "")
            else -> null
         }
      }
   }
}