package plugin

import util.access.v
import util.action.Action
import util.conf.Config
import util.conf.Configuration
import util.conf.IsConfig.EditMode
import util.file.Util.isValidatedDirectory
import util.functional.ifFalse
import util.functional.invoke
import util.functional.seqOf
import java.util.function.Consumer

class PluginManager(private val configuration: Configuration, private val userErrorLogger: Consumer<String>) {

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