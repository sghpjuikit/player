package sp.it.pl.layout.widget.feature

import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.Configurable

/** Editor for [Configurable]. */
@Feature(name = "Configurator", description = "Provides settings and configurations", type = ConfiguringFeature::class)
interface ConfiguringFeature<T> {

    /** Display configs of the specified configurable object for user to edit. */
    fun configure(configurable: Configurable<out T>?) = configure(configurable?.fields ?: listOf())

    /** Display specified configs for user to edit. */
    fun configure(configurable: Collection<Config<out T>>)

}