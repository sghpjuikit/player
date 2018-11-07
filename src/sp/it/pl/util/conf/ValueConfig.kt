package sp.it.pl.util.conf

import sp.it.pl.util.conf.Config.ConfigBase

private typealias In<T> = (T) -> Unit

/** Simple [sp.it.pl.util.conf.Config] wrapping a value. */
class ValueConfig<V>: ConfigBase<V> {

    private var value: V? = null
    private var applier: In<V>? = null

    constructor(type: Class<V>, name: String, gui_name: String, value: V, category: String, info: String, editable: EditMode, applier: In<V>): super(type, name, gui_name, value, category, info, editable) {
        this.value = value
        this.applier = applier
    }

    constructor(type: Class<V>, name: String, value: V): super(type, name, name, value, "", "", EditMode.USER) {
        this.value = value
    }

    constructor(type: Class<V>, name: String, value: V, applier: In<V>): super(type, name, name, value, "", "", EditMode.USER) {
        this.value = value
        this.applier = applier
    }

    constructor(type: Class<V>, name: String, value: V, info: String): super(type, name, name, value, "", info, EditMode.USER) {
        this.value = value
    }

    constructor(type: Class<V>, name: String, value: V, info: String, applier: In<V>): super(type, name, name, value, "", info, EditMode.USER) {
        this.value = value
        this.applier = applier
    }

    override fun getValue(): V? {
        return value
    }

    override fun setValue(v: V) {
        value = v
    }

    override fun applyValue(v: V) {
        applier?.invoke(v)
    }

}