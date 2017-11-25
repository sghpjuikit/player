package sp.it.pl.plugin

import sp.it.pl.util.access.v
import sp.it.pl.util.action.Action
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Configuration
import sp.it.pl.util.conf.IsConfig.EditMode
import sp.it.pl.util.file.Util.isValidatedDirectory
import sp.it.pl.util.functional.ifFalse
import sp.it.pl.util.functional.seqOf

class PluginManager(private val configuration: Configuration, private val userErrorLogger: (String) -> Unit) {

    /** Install the specified plugins. */
    fun installPlugins(vararg plugins: Plugin) {
        plugins.forEach { installPlugins(it) }
    }

    /** Install the specified plugins. */
    private fun installPlugins(plugin: Plugin) {
        val name = "Enable"
        val group = "${Plugin.CONFIG_GROUP}.${plugin.name}"
        val info = "Enable/disable this plugin"
        val starter = v(false) {
            val preInitOk = it && seqOf(plugin.location, plugin.userLocation)
                    .all { isValidatedDirectory(it) }
                    .ifFalse { userErrorLogger("Directory ${plugin.location} or ${plugin.userLocation} can not be used.") }
            plugin.activate(it && preInitOk)
        }
        configuration.collect(Config.PropertyConfig(Boolean::class.java, name, name, starter, group, info, EditMode.USER))
        configuration.collect(plugin)
        Action.installActions(plugin)   // TODO: move to configuration#collect
    }

}