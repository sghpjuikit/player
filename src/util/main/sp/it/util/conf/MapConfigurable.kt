package sp.it.util.conf

/** [Configurable] implemented as [Map], where key is config name. */
class MapConfigurable<T>: Configurable<T> {

    private var configs: Map<String, Config<T>>

    constructor(configs: List<Config<T>>) {
        this.configs = configs.associateBy { it.name }
    }

    @SafeVarargs
    constructor(vararg configs: Config<T>): this(configs.toList())

    override fun getFields() = configs.values

    override fun getField(name: String) = configs[name]

}