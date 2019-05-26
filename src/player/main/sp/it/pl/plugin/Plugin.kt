package sp.it.pl.plugin

import sp.it.pl.main.APP
import sp.it.pl.main.Settings
import sp.it.util.Locatable
import sp.it.util.conf.Configurable
import sp.it.util.conf.MultiConfigurable
import sp.it.util.dev.Idempotent
import sp.it.util.file.div

/** Plugin is configurable start/stoppable component. */
interface Plugin: Configurable<Any>, MultiConfigurable, Locatable {

    val name: String
    override val configurableDiscriminant get() = "${Settings.Plugin.name}.$name"
    override val location get() = APP.DIR_APP/"plugins"/name
    override val userLocation get() = APP.DIR_USERDATA/"plugins"/name

    @Idempotent
    fun start()

    @Idempotent
    fun stop()

    fun isRunning(): Boolean

    fun activate(active: Boolean) = if (active) start() else stop()

    fun isSupported() = true

}