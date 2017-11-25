package sp.it.pl.plugin

import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.Locatable
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.dev.Idempotent
import sp.it.pl.util.file.childOf

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

    override fun getLocation() = APP.DIR_APP.childOf("plugins", name)

    override fun getUserLocation() = APP.DIR_USERDATA.childOf("plugins", name)

    companion object {
        const val CONFIG_GROUP = "Plugins"
    }

}