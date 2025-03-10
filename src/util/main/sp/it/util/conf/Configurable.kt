package sp.it.util.conf

import javafx.event.EventHandler
import kotlin.reflect.full.memberProperties
import sp.it.util.dev.fail
import sp.it.util.functional.asIs
import sp.it.util.type.VType
import sp.it.util.type.forEachJavaFXProperty
import sp.it.util.type.isSubtypeOf

/**
 * Defines object that can be configured.
 *
 * Configurable object exports its configurable state as collection of [Config].
 * This can be used to store, restore or manipulate this state.
 *
 * Object can implement [Configurable] or be converted to it (e.g. [toConfigurableByReflect]). There is also choice to
 * use delegated configurable properties (e.g. [c], [cv], [cvn], et.c).
 *
 * @param <T> parameter specifying generic parameter of the configs. Useful if all the configs have the same generic
 * type argument.
 */
interface Configurable<T> {

   /** @return all configs of this configurable */
   fun getConfigs(): Collection<Config<T>>

   /** @return config with given [Config.name] or null if it does not exist */
   fun getConfig(name: String): Config<T>?

   /** @return config with given [Config.name] or throw exception if it does not exist */
   fun getConfigOrThrow(name: String): Config<T> = getConfig(name) ?: fail { "Config field '$name' not found." }

   companion object {
      val EMPTY = EmptyConfigurable
   }
}

/** Configurable with no configs. */
object EmptyConfigurable: Configurable<Nothing> {
   override fun getConfigs(): Collection<Config<Nothing>> = listOf()
   override fun getConfig(name: String): Nothing? = null
}

/** Configurable using reflection using [IsConfig], where configs are recomputed on every access. */
interface ConfigurableByReflect: Configurable<Any?> {

   override fun getConfigs(): Collection<Config<Any?>> = toConfigurableByReflect().getConfigs().asIs()

   override fun getConfig(name: String): Config<Any?>? {
      val property = this::class.memberProperties.find { it.name==name } ?: return null
      return annotatedConfig(property, this).asIs()
   }
}

/** [Configurable] backed by [List]. Hence [getConfigs] retains the order. */
class ListConfigurable<T> private constructor(private val configs: List<Config<T>>): Configurable<T> {

   override fun getConfigs() = configs

   override fun getConfig(name: String) = configs.find { name==it.name }

   companion object {

      fun <T> heterogeneous(configs: Collection<Config<out T>>): ListConfigurable<*> = ListConfigurable(configs.toList().asIs<List<Config<T>>>())

      fun <T> heterogeneous(vararg configs: Config<out T>): ListConfigurable<*> = heterogeneous(configs.toList())

      fun <T> homogeneous(configs: Collection<Config<T>>): ListConfigurable<T> = ListConfigurable(configs.toList())

      fun <T> homogeneous(vararg configs: Config<T>): ListConfigurable<T> = homogeneous(configs.toList())

   }
}

/** @return configurable of configs representing all configs marked [IsConfig] */
fun Any.toConfigurableByReflect(): Configurable<*> = annotatedConfigs(this).toListConfigurable()

/** @return configurable of configs representing all configs marked [IsConfig] */
fun Any.toConfigurableByReflect(fieldNamePrefix: String, category: String): Configurable<*> = annotatedConfigs(fieldNamePrefix, category, this).toListConfigurable()

/** @return configurable of configs representing all javafx properties of this object */
fun Any.toConfigurableFx(): Configurable<*> = forEachJavaFXProperty(this)
   .filter { !it.type.isSubtypeOf<EventHandler<*>>() }
   .map { (p, name, readOnly, _, type, constraints) ->
      Config.forValue<Any?>(VType(type), name, p()).apply {
         if (readOnly) addConstraints(Constraint.ReadOnlyIf(true))
         addConstraints(constraints.asIs<Collection<Constraint<Any?>>>())
      }
   }
   .toList().toListConfigurable()

/** @return configurable wrapping this list */
fun <T> Collection<Config<out T>>.toListConfigurable() = ListConfigurable.heterogeneous(this)