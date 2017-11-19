package plugin

import util.dev.Idempotent

abstract class PluginBase(name: String): Plugin {

    override val name = name
    private var isActive = false

    @Idempotent
    override fun start() {
        if (!isActive()) onStart()
        isActive = true
    }

    @Idempotent
    override fun stop() {
        if (isActive()) onStop()
        isActive = false
    }

    internal abstract fun onStart()

    internal abstract fun onStop()

    override fun isActive() = isActive

}