package sp.it.pl.layout.widget.feature

import sp.it.util.conf.Configurable

/** Editor for [Configurable]. */
@Feature(name = "Configurator", description = "Provides settings and configurations", type = ConfiguringFeature::class)
interface ConfiguringFeature {

   /** Display configs of the specified configurable object for user to edit. */
   fun configure(configurable: Configurable<*>?)

}