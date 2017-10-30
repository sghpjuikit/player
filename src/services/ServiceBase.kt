package services

import util.access.v
import util.conf.CachedConfigurable
import util.conf.Config
import util.conf.IsConfig
import java.util.*

abstract class ServiceBase(isEnabled: Boolean) : Service, CachedConfigurable<Any> {

    @Suppress("unused")
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