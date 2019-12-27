package sp.it.pl.plugin

import sp.it.pl.main.APP
import sp.it.util.collections.map.KClassMap
import sp.it.util.dev.failIf
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.toUnit
import kotlin.reflect.KClass

class PluginManager {

   private val plugins = KClassMap<Plugin>()

   /** Install the specified plugins. */
   fun installPlugins(vararg plugins: Plugin) = plugins.forEach { installPlugins(it) }

   /** Install the specified plugins. */
   private fun installPlugins(plugin: Plugin) {
      val type = plugin::class
      failIf(type in plugins) { "There already is plugin instance of type=$type" }

      plugins[type] = plugin
      APP.configuration.installActions(plugin)
   }

   /** @return all plugins. */
   fun getAll(): Sequence<Plugin> = plugins.values.asSequence()

   /** @return running plugin of the type specified by the argument or null if no such instance */
   fun <P: Plugin> get(type: KClass<P>): P? = getRaw(type)?.takeIf { it.isRunning() }

   /** @return running plugin of the type specified by the generic type argument if no such instance */
   inline fun <reified P: Plugin> get() = get(P::class)

   /** @return plugin of the type specified by the argument or null if no such instance */
   @Suppress("UNCHECKED_CAST")
   fun <P: Plugin> getRaw(type: KClass<P>): P? = plugins[type] as P?

   /** @return plugin of the type specified by the generic type argument if no such instance */
   inline fun <reified P: Plugin> getRaw() = getRaw(P::class)

   /** Invokes the action with the running plugin of the type specified by the argument or does nothing if no such instance */
   inline fun <P: Plugin> use(type: KClass<P>, action: (P) -> Unit): Unit = get(type).ifNotNull(action).toUnit()

   /** Invokes the action with the running plugin of the type specified by the generic type argument or does nothing if no such instance. */
   inline fun <reified P: Plugin> use(noinline action: (P) -> Unit) = use(P::class, action)

}