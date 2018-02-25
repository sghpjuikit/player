package sp.it.pl.service

import sp.it.pl.util.conf.Configurable


/**
 * <ul>
 * <li> all public methods of the service must be thread-safe
 * <li> starting and stopping the service must not depend on any other service. Service should in fact never make
 *     assumptions about other services
 * </ul>
 */
interface Service: Configurable<Any> {

    val name: String

    /**
     * @implSpec starting the service must not depend on any other service
     */
    fun start()

    fun isRunning(): Boolean

    /**
     * @implSpec stopping the service must not depend on any other service
     */
    fun stop()

    fun isSupported() = true

    fun isDependency() = false
}