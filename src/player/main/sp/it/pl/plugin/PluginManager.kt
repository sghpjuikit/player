package sp.it.pl.plugin

import sp.it.pl.main.APP

class PluginManager {

    /** Install the specified plugins. */
    fun installPlugins(vararg plugins: Plugin) = plugins.forEach { installPlugins(it) }

    /** Install the specified plugins. */
    private fun installPlugins(plugin: Plugin) = APP.configuration.installActions(plugin)

}