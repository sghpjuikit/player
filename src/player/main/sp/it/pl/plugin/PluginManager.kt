package sp.it.pl.plugin

import sp.it.pl.main.APP
import sp.it.util.collections.map.ClassMap
import sp.it.util.dev.failIf
import java.util.Optional
import java.util.function.Consumer
import kotlin.reflect.KClass

class PluginManager {

    private val plugins = ClassMap<Plugin>()

    /** Install the specified plugins. */
    fun installPlugins(vararg plugins: Plugin) = plugins.forEach { installPlugins(it) }

    /** Install the specified plugins. */
    private fun installPlugins(plugin: Plugin) {
        val type = plugin.javaClass
        failIf(plugins.containsKey(type)) { "There already is a service of this type" }

        plugins[type] = plugin
        APP.configuration.installActions(plugin)
    }

    /** @return all plugins. */
    fun getAll(): Sequence<Plugin> = plugins.values.asSequence()

    /** @return plugin of the type specified by the argument. */
    @Suppress("UNCHECKED_CAST")
    fun <P: Plugin> get(type: Class<P>): Optional<P> = Optional.ofNullable(plugins[type] as P)

    /** Invokes the action with the plugin of the type specified by the argument. */
    fun <P: Plugin> use(type: KClass<P>, action: (P) -> Unit) = use(type.java, Consumer(action))

    /** Invokes the action with the plugin of the type specified by the argument. */
    fun <P: Plugin> use(type: Class<P>, action: Consumer<in P>) = get(type).filter { it.isRunning() }.ifPresent(action)

    /** Invokes the action with the plugin of the type specified by the generic type argument. */
    inline fun <reified P: Plugin> use(noinline action: (P) -> Unit) = use(P::class.java, Consumer(action))

}