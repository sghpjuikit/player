package sp.it.pl.plugin

import sp.it.pl.main.APP
import sp.it.util.Locatable
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.GlobalConfigDelegator
import sp.it.util.dev.Idempotent
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.pl.main.AppSettings.plugins as conf


/** Plugin is configurable start/stoppable component. */
interface Plugin: Configurable<Any>, GlobalConfigDelegator, Locatable {

   val name: String
   override val configurableGroupPrefix get() = "${conf.name}.$name"
   override val location get() = APP.location.plugins/name
   override val userLocation get() = APP.location.user.plugins/name

   override fun getConfigs(): Collection<Config<Any>> = fail { "Not standalone configurable" }

   override fun getConfig(name: String): Config<Any>? = fail { "Not standalone" }

   @Idempotent
   fun start()

   @Idempotent
   fun stop()

   fun isRunning(): Boolean

   fun activate(active: Boolean) = if (active) start() else stop()

   fun isSupported() = true

}