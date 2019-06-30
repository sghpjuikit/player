package sp.it.util.conf

import sp.it.util.conf.ConfigurationUtil.configsOf
import sp.it.util.conf.ConfigurationUtil.createConfig
import sp.it.util.dev.fail
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
 * This interface provides default implementation using reflection using [IsConfig], where configs are recomputed on
 * access as needed.
 *
 * @param <T> parameter specifying generic parameter of the configs. Useful if all of the configs have the same generic
 * type argument.
 */
interface Configurable<T> {

   /** @return all configs of this configurable */
   @Suppress("UNCHECKED_CAST")
   @JvmDefault
   fun getFields(): Collection<Config<T>> = configsOf(javaClass, this, false, true) as Collection<Config<T>>

   /** @return config with given [Config.getName] or null if does not exist */
   @Suppress("UNCHECKED_CAST")
   @JvmDefault
   fun getField(name: String): Config<T>? {
      return try {
         val c = this.javaClass
         val f = Util.getField(c, name)
         createConfig(c, f, this, false, true) as Config<T>?
      } catch (e: NoSuchFieldException) {
         null
      } catch (e: SecurityException) {
         null
      }
   }

   /** @return config with given [Config.getName] or throw exception if does not exist */
   @JvmDefault
   fun getFieldOrThrow(name: String): Config<T> = getField(name) ?: fail { "Config field '$name' not found." }

   companion object {

      /** Configurable with no fields. */
      val EMPTY: Configurable<Nothing> = object: Configurable<Nothing> {
         override fun getFields(): Collection<Config<Nothing>> = listOf()
         override fun getField(name: String): Nothing? = null
      }

   }
}

/** @return configurable of configs representing all fields marked [IsConfig] */
fun Any.toConfigurableByReflect(): Configurable<*> = configsOf(javaClass, this, false, true).toListConfigurable()

/** @return configurable of configs representing all fields marked [IsConfig] */
fun Any.toConfigurableByReflect(fieldNamePrefix: String, category: String): Configurable<*> =
   configsOf(javaClass, fieldNamePrefix, category, this, false, true).toListConfigurable()

/** @return configurable of configs representing all javafx properties of this object */
fun Any.toConfigurableFx(): Configurable<*> {
   val cs = ArrayList<Config<Any?>>()
   forEachJavaFXProperty(this) { p, name, type -> cs.add(Config.forValue<Any?>(type, name, p)) }
   return ListConfigurable(cs)
}

/** @return configurable wrapping this list */
@Suppress("UNCHECKED_CAST")
fun <T> Collection<Config<out T>>.toListConfigurable(): ListConfigurable<*> = ListConfigurable(this as Collection<Config<T>>)