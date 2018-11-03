package sp.it.pl.plugin

import sp.it.pl.main.AppUtil.APP
import sp.it.pl.main.Settings
import sp.it.pl.util.Locatable
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.MultiConfigurable
import sp.it.pl.util.dev.Idempotent
import sp.it.pl.util.file.childOf

/** Plugin is like Service, but with no API to consume so developer never uses it directly. */
interface Plugin: Configurable<Any>, MultiConfigurable, Locatable {

    val name: String
    override val configurableDiscriminant get() = "${Settings.PLUGINS}.$name"
    override val location get() = APP.DIR_APP.childOf("plugins", name)
    override val userLocation get() = APP.DIR_USERDATA.childOf("plugins", name)

    @Idempotent
    fun start()

    @Idempotent
    fun stop()

    fun isRunning(): Boolean

    fun activate(active: Boolean) = if (active) start() else stop()

}