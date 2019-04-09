package sp.it.pl.layout.widget.feature

import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.ListConfigurable

/** Editor for [Configurable]. */
@Feature(name = "Configurator", description = "Provides settings and configurations", type = ConfiguringFeature::class)
interface ConfiguringFeature {

    /** Display configs of the specified configurable object for user to edit. */
    fun configure(configurable: Configurable<*>?)

    /** Display specified configs for user to edit. */
    @Suppress("UNCHECKED_CAST")
    fun configure(configurable: Collection<Config<*>>) = configure(ListConfigurable(configurable as Collection<Config<Any>>))

}