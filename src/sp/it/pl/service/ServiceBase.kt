package sp.it.pl.service

import mu.KLogging
import sp.it.pl.util.conf.cv
import sp.it.pl.util.access.V
import sp.it.pl.util.access.initSync
import sp.it.pl.util.conf.IsConfig

abstract class ServiceBase(override val name: String, isEnabledByDefault: Boolean): Service {

    @IsConfig(name = "Enabled", info = "Starts or stops the service")
    private val enabled by cv(isEnabledByDefault) { V(it).apply { initSync { runWhenReady { enable(it) } } } }

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