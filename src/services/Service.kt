package services

import util.conf.Configurable

interface Service : Configurable<Any> {
    fun start()

    fun isRunning(): Boolean

    fun stop()

    fun isSupported() = true

    fun isDependency() = false
}