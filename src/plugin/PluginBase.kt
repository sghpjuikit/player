package plugin

import util.dev.Idempotent

abstract class PluginBase(name: String): Plugin {
    private val name = name
    private var isActive = false

    override fun getName() = name

    @Idempotent
    override fun start() {
        if (!isActive()) onStart()
        isActive = true
    }

    internal abstract fun onStart()

    @Idempotent
    override fun stop() {
        if (isActive()) onStop()
        isActive = false
    }

    internal abstract fun onStop()

    override fun isActive() = isActive
}