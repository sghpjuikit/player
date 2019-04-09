package sp.it.pl.gui.itemnode

import javafx.scene.Node
import sp.it.util.conf.Config
import sp.it.util.functional.invoke
import java.util.function.Consumer

/** Graphics with a value, usually an ui editor. */
abstract class ItemNode<out T> {

    /** @return current value */
    abstract fun getVal(): T?

    /** @return the root node */
    abstract fun getNode(): Node

    /** Focuses this node's content, usually a primary input field. */
    open fun focus() {}
}

/** Item node which directly holds the value. */
abstract class ValueNodeBase<T: Any?>(initialValue: T): ItemNode<T>() {
    @JvmField protected var value: T = initialValue

    override fun getVal() = value
}

/** Item node which directly holds the value and fires value change events. */
abstract class ValueNode<T: Any?>(initialValue: T): ValueNodeBase<T>(initialValue) {

    /** Value change handler invoked when value changes, consuming the new value. */
    @JvmField var onItemChange: Consumer<T> = Consumer {}

    /** Sets value & fires itemChange if available. Internal use only. */
    protected open fun changeValue(nv: T) {
        if (value===nv) return
        value = nv
        onItemChange(nv)
    }
}

/** Item node which holds the value in a [Config]. */
abstract class ConfigNode<T: Any?>(@JvmField val config: Config<T>): ItemNode<T>() {

    override fun getVal(): T = config.value

}