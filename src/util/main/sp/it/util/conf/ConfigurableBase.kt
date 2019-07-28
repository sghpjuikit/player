package sp.it.util.conf

import sp.it.util.conf.ConfigValueSource.Companion.SimpleConfigValueStore

/**
 * Configurable that holds its [Config]s, because it provides its own standalone [configurableValueSource] (which is why
 * [configurableDiscriminant] is null).
 *
 * Allows creating type-safe declarative [Configurable] objects, like:
 * @sample ConfigurableBase.sample
 */
@Suppress("LeakingThis")
open class ConfigurableBase<T>: SimpleConfigValueStore<T>(), ConfigDelegator, Configurable<T> {
   override val configurableDiscriminant: Nothing? = null
   override val configurableValueSource = this

   companion object {
      private fun sample() {
         object: ConfigurableBase<Boolean>() {
            val orderCount by cv(10)
            val orderId by cv("myOrder")
         }
      }
   }
}