package plugin

import main.App
import util.conf.Configurable
import util.dev.Idempotent
import util.file.Util.childOf

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

    fun getLocation() = childOf(App.APP.DIR_APP, "plugins", getName())!!

    fun getResource(path: String) = childOf(getLocation(), path)!!

    fun getUserLocation() = childOf(App.APP.DIR_USERDATA, "plugins", getName())!!

    fun getUserResource(path: String) = childOf(getUserLocation(), path)!!

    companion object Factory {
        // TODO: avoid specifying plugin name in group
        const val CONFIG_GROUP = "Plugins"
    }
}