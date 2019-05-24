package sp.it.pl.service

import sp.it.pl.main.Settings
import sp.it.util.conf.Configurable
import sp.it.util.conf.MultiConfigurable
import sp.it.util.dev.Idempotent

/**
 * Requirements:
 * * all public methods of the service must be thread-safe
 * * service should never make assumptions about other services
 * * [start] and [stop] must never assume any state of the service
 */
interface Service: Configurable<Any>, MultiConfigurable {

    val name: String

    override val configurableDiscriminant get() = "${Settings.Service.name}.$name"

    @Idempotent
    fun start()

    fun isRunning(): Boolean

    @Idempotent
    fun stop()

    fun isSupported() = true

    fun isDependency() = false

}