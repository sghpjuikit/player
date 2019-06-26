package sp.it.util.conf

import sp.it.util.conf.ConfigValueSource.Companion.SimpleConfigValueStore

/**
 * Configurable that holds its [Config]s, because it provides its own standalone [configurableValueStore] (which is why
 * [configurableDiscriminant] is null).
 *
 * Allows creating type-safe declarative [Configurable] objects, like:
 * @sample ConfigurableBase.sample
 */
@Suppress("LeakingThis")
open class ConfigurableBase<T>: SimpleConfigValueStore<T>(), MultiConfigurable, Configurable<T> {
   override val configurableDiscriminant: Nothing? = null
   override val configurableValueStore = this

   companion object {
      private fun sample() {
         object: ConfigurableBase<Boolean>() {
            val orderCount by cv(10)
            val orderId by cv("myOrder")
         }
      }
   }
}