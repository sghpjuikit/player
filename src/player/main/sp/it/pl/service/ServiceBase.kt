package sp.it.pl.service

import mu.KLogging
import sp.it.util.access.initSync
import sp.it.util.access.v
import sp.it.util.conf.IsConfig
import sp.it.util.conf.cv

abstract class ServiceBase(override val name: String, isEnabledByDefault: Boolean): Service {

    @IsConfig(name = "Enabled", info = "Starts or stops the service")
    private val enabled by cv(isEnabledByDefault) { v(it).initSync { runWhenReady { enable(it) } } }

    private fun enable(isToBeRunning: Boolean) {
        val wasRunning = isRunning()
        if (wasRunning==isToBeRunning) return

        if (isToBeRunning && !isDependency() && isSupported()) {
            logger.info { "Service $name starting..." }
            start()
        }
        if (!isToBeRunning) {
            logger.info { "Service $name stopping..." }
            stop()
        }
    }

    companion object: KLogging()
}