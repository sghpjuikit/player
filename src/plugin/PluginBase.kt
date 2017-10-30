package plugin

import util.dev.Idempotent

abstract class PluginBase(name: String): Plugin {
    private val name = name
    private var isActive = false

    override fun getName() = name

    override fun start() {
        if (!isActive()) onStart()
        isActive = true
    }

    @Idempotent
    internal abstract fun onStart()

    override fun stop() {
        if (isActive()) onStop()
        isActive = false
    }

    @Idempotent
    internal abstract fun onStop()

    override fun isActive() = isActive
}