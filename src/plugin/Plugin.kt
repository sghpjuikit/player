package plugin

import main.App
import util.Locatable
import util.conf.Configurable
import util.dev.Idempotent
import util.file.childOf

/** Plugin is like Service, but with no API to consume so developer never uses it directly. */
interface Plugin: Configurable<Any>, Locatable {

    val name: String

    @Idempotent
    fun start()

    @Idempotent
    fun stop()

    fun isActive(): Boolean

    fun activate(active: Boolean) {
        if (active) start()
        else stop()
    }

    override fun getLocation() = App.APP.DIR_APP.childOf("plugins", name)

    override fun getUserLocation() = App.APP.DIR_USERDATA.childOf("plugins", name)

    companion object {
        const val CONFIG_GROUP = "Plugins"
    }

}