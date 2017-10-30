package plugin

import main.App
import util.conf.Configurable
import util.dev.Idempotent
import util.file.childOf

interface Plugin: Configurable<Any> {

    fun getName(): String

    @Idempotent
    fun start()

    @Idempotent
    fun stop()

    fun isActive(): Boolean

    fun activate(active: Boolean) {
        if (active) start()
        else stop()
    }

    fun getLocation() = App.APP.DIR_APP.childOf("plugins", getName())

    fun getResource(path: String) = getLocation().childOf(path)

    fun getUserLocation() = App.APP.DIR_USERDATA.childOf("plugins", getName())

    fun getUserResource(path: String) = getUserLocation().childOf(path)

    companion object Factory {
        // TODO: avoid specifying plugin name in group
        const val CONFIG_GROUP = "Plugins"
    }
}