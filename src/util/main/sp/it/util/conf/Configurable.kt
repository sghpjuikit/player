package sp.it.util.conf

import sp.it.util.conf.ConfigurationUtil.configsOf
import sp.it.util.dev.fail
import sp.it.util.functional.asIs
import sp.it.util.type.Util
import sp.it.util.type.Util.forEachJavaFXProperty
import java.util.ArrayList

/**
 * Defines object that can be configured.
 *
 * Configurable object exports its configurable state as collection of [Config].
 * This can be used to store, restore or manipulate this state.
 *
 * Object can implement [Configurable] or be converted to it (e.g. [toConfigurableByReflect]). There is also choice to
 * use delegated configurable properties (e.g. [c], [cv], [cvn], etc).
 *
 * @param <T> parameter specifying generic parameter of the configs. Useful if all of the configs have the same generic
 * type argument.
 */
interface Configurable<T> {

   /** @return all configs of this configurable */
   fun getConfigs(): Collection<Config<T>>

   /** @return config with given [Config.name] or null if does not exist */
   fun getConfig(name: String): Config<T>?

   /** @return config with given [Config.name] or throw exception if does not exist */
   @JvmDefault
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

   @JvmDefault
   override fun getConfigs(): Collection<Config<Any?>> = toConfigurableByReflect().getConfigs().asIs()

   @JvmDefault
   override fun getConfig(name: String): Config<Any?>? {
      return try {
         ConfigurationUtil.createConfig(Util.getField(javaClass, name), this).asIs()
      } catch (e: NoSuchFieldException) {
         null
      } catch (e: SecurityException) {
         null
      }
   }
}

/** [Configurable] backed by [List]. Hence [getConfigs] retains the order. */
class ListConfigurable<T> private constructor(private val configs: List<Config<T>>): Configurable<T> {

   override fun getConfigs() = configs

   override fun getConfig(name: String) = configs.find { name==it.name }

   companion object {

      @Suppress("UNCHECKED_CAST")
      fun <T> heterogeneous(configs: Collection<Config<out T>>): ListConfigurable<*> = ListConfigurable(configs.toList() as List<Config<T>>)

      fun <T> heterogeneous(vararg configs: Config<out T>): ListConfigurable<*> = heterogeneous(configs.toList())

      fun <T> homogeneous(configs: Collection<Config<T>>): ListConfigurable<T> = ListConfigurable(configs.toList())

      fun <T> homogeneous(vararg configs: Config<T>): ListConfigurable<T> = homogeneous(configs.toList())

   }
}

/** @return configurable of configs representing all configs marked [IsConfig] */
fun Any.toConfigurableByReflect(): Configurable<*> = configsOf(javaClass, this).toListConfigurable()

/** @return configurable of configs representing all configs marked [IsConfig] */
fun Any.toConfigurableByReflect(fieldNamePrefix: String, category: String): Configurable<*> =
   configsOf(javaClass, fieldNamePrefix, category, this).toListConfigurable()

/** @return configurable of configs representing all javafx properties of this object */
fun Any.toConfigurableFx(): Configurable<*> {
   val cs = ArrayList<Config<Any?>>()
   forEachJavaFXProperty(this) { p, name, type -> cs.add(Config.forValue(type, name, p)) }
   return cs.toListConfigurable()
}

/** @return configurable wrapping this list */
fun <T> Collection<Config<out T>>.toListConfigurable() = ListConfigurable.heterogeneous(this)