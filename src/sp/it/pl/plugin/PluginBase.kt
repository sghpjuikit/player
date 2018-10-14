package sp.it.pl.plugin

import mu.KLogging
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.conf.cv
import sp.it.pl.util.access.V
import sp.it.pl.util.access.initSync
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.dev.Idempotent
import sp.it.pl.util.file.Util.isValidatedDirectory
import sp.it.pl.util.functional.ifFalse
import sp.it.pl.util.functional.seqOf

abstract class PluginBase(override val name: String, isEnabledByDefault: Boolean): Plugin {

    @IsConfig(name = "Enable", info = "Enable/disable this plugin")
    private val enabled by cv(isEnabledByDefault) { V(it).apply { initSync { runWhenReady { enable(it) } } } }
    private var isRunning = false

    private fun enable(isToBeRunning: Boolean) {
        val preInitOk = isToBeRunning && seqOf(location, userLocation)
                .all { isValidatedDirectory(it) }
                .ifFalse { APP.messagePane.show("Directory $location or $userLocation can not be used.") }
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