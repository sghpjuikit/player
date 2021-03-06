package sp.it.util.conf

import sp.it.util.conf.ConfigValueSource.Companion.SimpleConfigValueStore

/**
 * Configurable that holds its [Config]s, because it provides its own standalone [configurableValueSource] (which is why
 * [configurableGroupPrefix] is null).
 *
 * Allows creating type-safe declarative [Configurable] objects, like:
 * @sample ConfigurableBase.sample
 */
@Suppress("LeakingThis")
open class ConfigurableBase<T>: SimpleConfigValueStore<T>(), ConfigDelegator, Configurable<T> {
   override val configurableGroupPrefix: Nothing? = null
   override val configurableValueSource = this

   companion object {
      private fun sample() {
         object: ConfigurableBase<Any?>() {
            val orderCount by cv(10)
            val orderId by cv("myOrder")
         }
      }
   }
}