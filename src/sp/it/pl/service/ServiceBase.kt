package sp.it.pl.service

import sp.it.pl.util.access.v
import sp.it.pl.util.conf.CachedConfigurable
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.IsConfig
import java.util.*

abstract class ServiceBase(isEnabled: Boolean) : Service, CachedConfigurable<Any> {

    @Suppress("sp/it/pl/unused")
    @IsConfig(name = "Enabled", info = "Starts or stops the service")
    private val enabled = v(isEnabled, { enable(it) })
    private val configs = HashMap<String, Config<Any>>()

    private fun enable(isToBeRunning: Boolean) {
        val wasRunning = isRunning()
        if (wasRunning == isToBeRunning) return

        if (isToBeRunning && !isDependency() && isSupported()) start()
        if (!isToBeRunning) stop()
    }

    override fun getFieldsMap(): Map<String, Config<Any>> = configs

}