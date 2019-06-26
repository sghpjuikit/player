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
        val wasRunning = isRunning()
        if (wasRunning==isToBeRunning) return

        if (isToBeRunning && isSupported()) {
            logger.info { "Plugin $name starting..." }

            val canBeRunning = isSupported() && sequenceOf(location, userLocation).all { isValidatedDirectory(it) }
                .ifFalse { APP.ui.messagePane.orBuild.show("Directory $location or $userLocation can not be used.") }

            if (canBeRunning) start()
            else logger.error { "Plugin $name could not start..." }
        }
        if (!isToBeRunning) {
            logger.info { "Plugin $name stopping..." }
            stop()
        }
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

    open fun onStart() = Unit

    open fun onStop() = Unit

    override fun isRunning() = isRunning

    companion object: KLogging()
}