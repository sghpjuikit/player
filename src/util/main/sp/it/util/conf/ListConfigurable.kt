package sp.it.util.conf

/** [Configurable] backed by [List]. Hence [getFields] retains the order. */
class ListConfigurable<T>: Configurable<T> {

   private val configs: List<Config<T>>

   constructor(configs: Collection<Config<T>>) {
      this.configs = configs.toList()
   }

   constructor(vararg configs: Config<T>): this(configs.toList())

   /** @return config at specified index */
   fun getField(at: Int) = configs[at]

   override fun getFields() = configs

   override fun getField(name: String) = configs.find { name==it.name }

}