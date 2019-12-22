package sp.it.util.conf

/** [Configurable] backed by [List]. Hence [getFields] retains the order. */
class ListConfigurable<T> private constructor(val configs: List<Config<T>>): Configurable<T> {

   override fun getFields() = configs

   override fun getField(name: String) = configs.find { name==it.name }

   companion object {

      @Suppress("UNCHECKED_CAST")
      fun <T> heterogeneous(configs: Collection<Config<out T>>): ListConfigurable<*> = ListConfigurable(configs.toList() as List<Config<T>>)

      fun <T> heterogeneous(vararg configs: Config<out T>): ListConfigurable<*> = heterogeneous(configs.toList())

      fun <T> homogeneous(configs: Collection<Config<T>>): ListConfigurable<T> = ListConfigurable(configs.toList())

      fun <T> homogeneous(vararg configs: Config<T>): ListConfigurable<T> = homogeneous(configs.toList())

   }
}