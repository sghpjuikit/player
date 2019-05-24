package sp.it.pl.service

import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.main.run1AppReady
import sp.it.util.conf.IsConfig
import sp.it.util.conf.cv

abstract class ServiceBase(override val name: String, isEnabledByDefault: Boolean): Service {

    @IsConfig(name = "Enabled", info = "Starts or stops the service")
    private val enabled by cv(isEnabledByDefault) sync { APP.run1AppReady { enable(it) } }

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