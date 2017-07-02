package plugin

import main.App
import util.Locatable
import util.conf.Configurable
import util.dev.Idempotent
import util.file.Util.childOf

interface Plugin: Configurable<Any>, Locatable {

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

    override fun getLocation() = childOf(App.APP.DIR_APP, "plugins", getName())!!

    override fun getUserLocation() = childOf(App.APP.DIR_USERDATA, "plugins", getName())!!

    companion object Factory {
        // TODO: avoid specifying plugin name in group
        const val CONFIG_GROUP = "Plugins"
    }
}