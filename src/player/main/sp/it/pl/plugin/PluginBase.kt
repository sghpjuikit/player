package sp.it.pl.plugin

import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.main.run1AppReady
import sp.it.util.conf.IsConfig
import sp.it.util.conf.cv
import sp.it.util.dev.Idempotent
import sp.it.util.file.Util.isValidatedDirectory
import sp.it.util.functional.ifFalse

abstract class PluginBase(override val name: String, isEnabledByDefault: Boolean): Plugin {

    @IsConfig(name = "Enable", info = "Enable/disable this plugin")
    private val enabled by cv(isEnabledByDefault) sync { APP.run1AppReady { enable(it) } }
    private var isRunning = false

    private fun enable(isToBeRunning: Boolean) {
        val preInitOk = isToBeRunning && sequenceOf(location, userLocation)
                .all { isValidatedDirectory(it) }
                .ifFalse { APP.ui.messagePane.orBuild.show("Directory $location or $userLocation can not be used.") }
        val v = isToBeRunning && preInitOk
        val action = if (v) "starting" else "stopping"

        logger.info { "Plugin $name $action..." }
        activate(v)
    }

    @Idempotent
    override fun start() {
        if (!isRunning()) onStart()
        isRunning = true
    }

    @Idempotent
    override fun stop() {
        val wasRunning = isRunning
        isRunning = false
        if (wasRunning) onStop()
    }

    internal abstract fun onStart()

    internal abstract fun onStop()

    override fun isRunning() = isRunning

    companion object: KLogging()
}