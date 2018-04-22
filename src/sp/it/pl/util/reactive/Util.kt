@file:JvmName("Util")

package sp.it.pl.util.reactive

import javafx.beans.binding.Bindings
import javafx.beans.binding.Bindings.size
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.event.Event
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.Screen
import javafx.stage.Window
import org.reactfx.EventStreams
import org.reactfx.Subscription
import sp.it.pl.util.dev.Experimental
import sp.it.pl.util.functional.invoke
import java.util.function.Consumer
import kotlin.reflect.KCallable

/** Sets a value consumer to be fired immediately and on every value change. */
infix fun <O> ObservableValue<O>.sync(u: (O) -> Unit) = maintain(Consumer { u(it) })

infix fun <O> ObservableValue<O>.syncTo(w: WritableValue<in O>): Subscription {
    w.value = value
    return this attachTo w
}

infix fun <O> WritableValue<O>.syncFrom(o: ObservableValue<out O>): Subscription = o syncTo this

/** Sets a value consumer to be fired on every value change. */
infix fun <O> ObservableValue<O>.attach(u: (O) -> Unit): Subscription {
    val l = ChangeListener<O> { _, _, nv -> u(nv) }
    addListener(l)
    return Subscription { removeListener(l) }
}

infix fun <O> ObservableValue<O>.attachTo(w: WritableValue<in O>): Subscription {
    val l = ChangeListener<O> { _, _, nv -> w.value = nv }
    this.addListener(l)
    return Subscription { this.removeListener(l) }
}

infix fun <O> WritableValue<O>.attachFrom(o: ObservableValue<out O>): Subscription = o syncTo this

fun <O1, O2> attachTo(o1: ObservableValue<O1>, o2: ObservableValue<O2>, u: (O1, O2) -> Unit): Subscription {
    val l1 = ChangeListener<O1> { _, _, nv -> u(nv, o2.value) }
    val l2 = ChangeListener<O2> { _, _, nv -> u(o1.value, nv) }
    o1.addListener(l1)
    o2.addListener(l2)
    return Subscription {
        o1.removeListener(l1)
        o2.removeListener(l2)
    }
}

fun <O1, O2> syncTo(o1: ObservableValue<O1>, o2: ObservableValue<O2>, u: (O1, O2) -> Unit): Subscription {
    u(o1.value, o2.value)
    return attachTo(o1, o2, u)
}

fun <O1, O2, O3> attachTo(o1: ObservableValue<O1>, o2: ObservableValue<O2>, o3: ObservableValue<O3>, u: (O1, O2, O3) -> Unit): Subscription {
    val l1 = ChangeListener<O1> { _, _, nv -> u(nv, o2.value, o3.value) }
    val l2 = ChangeListener<O2> { _, _, nv -> u(o1.value, nv, o3.value) }
    val l3 = ChangeListener<O3> { _, _, nv -> u(o1.value, o2.value, nv) }
    o1.addListener(l1)
    o2.addListener(l2)
    o3.addListener(l3)
    return Subscription {
        o1.removeListener(l1)
        o2.removeListener(l2)
        o3.removeListener(l3)
    }
}

fun <O1, O2, O3> syncTo(o1: ObservableValue<O1>, o2: ObservableValue<O2>, o3: ObservableValue<O3>, u: (O1, O2, O3) -> Unit): Subscription {
    u(o1.value, o2.value, o3.value)
    return attachTo(o1, o2, o3, u)
}

/** Sets a value change consumer to be fired on every value change. */
infix fun <O> ObservableValue<O>.changes(u: (O, O) -> Unit): Subscription {
    val l = ChangeListener<O> { _, ov, nv -> u(ov, nv) }
    addListener(l)
    return Subscription { removeListener(l) }
}

/** Sets a value consumer to be fired if the value is true immediately and on every value change. */
infix fun ObservableValue<Boolean>.syncTrue(u: (Boolean) -> Unit): Subscription = maintain(Consumer { if (it) u(it) })

/** Sets a value consumer to be fired if the value is false immediately and on every value change. */
infix fun ObservableValue<Boolean>.syncFalse(u: (Boolean) -> Unit): Subscription = maintain(Consumer { if (!it) u(it) })

/** Sets a size consumer to be fired immediately and on every list size change. */
infix fun <T> ObservableList<T>.syncSize(action: (Int) -> Unit): Subscription {
    var s = -1
    val l = ListChangeListener<T> { _ ->
        val os = s
        val ns = size
        if (os!=ns) action(ns)
        s = ns
    }
    l.onChanged(null)
    addListener(l)
    return Subscription { removeListener(l) }
}

/** Sets a size consumer to be fired on every list size change. */
infix fun <T> ObservableList<T>.attachSize(action: (Int) -> Unit): Subscription {
    var s = size
    val l = ListChangeListener<T> { _ ->
        val os = s
        val ns = size
        if (os!=ns) action(ns)
        s = ns
    }
    l.onChanged(null)
    addListener(l)
    return Subscription { removeListener(l) }
}

/** Binds the two properties bi-directionally. */
infix fun <O> Property<O>.syncBi(w: Property<O>): Subscription {
    bindBidirectional(w)
    return Subscription { unbindBidirectional(w) }
}

/** @returns observable size of this list */
fun <T> ObservableList<T>.sizes() = size(this)!!

/** @returns observable size of this set */
fun <T> ObservableSet<T>.sizes() = size(this)!!

@Experimental
fun <O, R> ObservableValue<O>.select(extractor: KCallable<ObservableValue<R>>): ObservableValue<R> {
    return Bindings.select(this, extractor.name.removeSuffix("Property"))
}

@Experimental
fun <O, R> ObservableValue<O>.into(extractor: (O) -> ObservableValue<R>, action: (O, R) -> Unit): Subscription {
    var s = Subscription { }
    return this attach { v1 ->
        s.unsubscribe()
        if (v1!=null) s = extractor(v1) sync { v2 ->
            action(v1, v2)
        }
    }
}

// TODO: remove
fun <O, V> ObservableValue<O>.maintain(m: (O) -> V, u: Consumer<in V>): Subscription {
    u(m(this.value))
    return EventStreams.valuesOf(this).map(m).subscribe(u)
}

// TODO: remove
fun <O> ObservableValue<O>.maintain(u: Consumer<O>): Subscription {
    val l = ChangeListener<O> { _, _, nv -> u(nv) }
    u(this.value)
    this.addListener(l)
    return Subscription { this.removeListener(l) }
}

// TODO: remove
fun <O, V> ObservableValue<O>.maintain(m: (O) -> V, w: WritableValue<in V>): Subscription {
    w.value = m(this.value)
    val l = ChangeListener<O> { _, _, nv -> w.value = m(nv) }
    this.addListener(l)
    return Subscription { this.removeListener(l) }
}

// TODO: remove
infix fun <O> ObservableValue<O>.maintain(w: WritableValue<in O>): Subscription {
    w.value = value
    val l = ChangeListener<O> { _, _, nv -> w.value = nv }
    this.addListener(l)
    return Subscription { this.removeListener(l) }
}

/**
 * Same as [sync1If], but will not run immediately if the value already passes the condition - only reacts on value
 * changes.
 */
fun <T> ObservableValue<T>.attach1If(condition: (T) -> Boolean, action: (T) -> Unit): Subscription {
    val l = object: ChangeListener<T> {
        override fun changed(observable: ObservableValue<out T>, ov: T, nv: T) {
            if (condition(nv)) {
                action(nv)
                removeListener(this)
            }
        }
    }
    addListener(l)
    return Subscription { removeListener(l) }
}

/**
 * Runs action (consuming the property's value) as soon as the condition is met. Useful to execute initialization,
 * for example to wait for nonnull value.
 *
 * The action runs immediately if current value already meets the condition. Otherwise registers a one-time
 * listener, which will run the action when the value changes to such that the condition is met.
 *
 * It is guaranteed:
 *  *  action executes at most once
 *
 * It is not guaranteed:
 *  *  action will execute, because the value may never meet the condition or it was unsubscribed before it happened
 *
 * @param condition test the value must pass for the action to execute
 * @param action action receiving the value as argument and that runs exactly once when the condition is first met
 */
fun <T> ObservableValue<T>.sync1If(condition: (T) -> Boolean, action: (T) -> Unit): Subscription =
        if (condition(value)) {
            action(value)
            Subscription {}
        } else {
            attach1If(condition, action)
        }

/** [sync1If] testing the value is not null. */
fun <T> ObservableValue<T>.sync1IfNonNull(action: (T) -> Unit) = sync1If({ it!=null }, action)

// TODO: move out
fun sync1IfImageLoaded(image: Image, action: Runnable) = image.progressProperty().sync1If({ it.toDouble()==1.0 }) { action() }

// TODO: move out
fun doIfImageLoaded(imageView: ImageView, action: Consumer<Image>) = imageView.imageProperty().into(Image::progressProperty) { i, p -> if (p==1.0) action(i) }

// TODO: move out
/** Creates list change listener which calls an action for every added or removed item. */
fun onScreenChange(onChange: Consumer<in Screen>): Subscription {
    val s1 = Screen.getScreens().onItemAdded { onChange(it) }
    val s2 = Screen.getScreens().onItemRemoved { onChange(it) }
    return s1.and(s2)
}

/** Call specified handler every time an item this list changes */
fun <T> ObservableList<T>.onChange(changeHandler: () -> Unit): Subscription {
    val l = ListChangeListener<T> {
        while (it.next()) {
            changeHandler()
        }
    }
    addListener(l)
    return Subscription { removeListener(l) }
}

/** Call specified handler every time an item is added to this list passing it as argument */
fun <T> ObservableList<T>.onItemAdded(addedHandler: (T) -> Unit): Subscription {
    val l = ListChangeListener<T> {
        while (it.next()) {
            if (!it.wasPermutated() && !it.wasUpdated()) {
                if (it.wasAdded()) it.addedSubList.forEach(addedHandler)
            }
        }
    }
    addListener(l)
    return Subscription { removeListener(l) }
}

/** Call specified handler every time an item is removed from this list passing it as argument */
fun <T> ObservableList<T>.onItemRemoved(removedHandler: (T) -> Unit): Subscription {
    val l = ListChangeListener<T> {
        while (it.next()) {
            if (!it.wasPermutated() && !it.wasUpdated()) {
                if (it.wasRemoved()) it.removed.forEach(removedHandler)
            }
        }
    }
    addListener(l)
    return Subscription { removeListener(l) }
}

/** Equivalent to [Window.addEventHandler]. */
fun <T: Event> Window.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    addEventHandler(eventType, eventHandler)
    return Subscription { removeEventHandler(eventType, eventHandler) }
}

/** Equivalent to [Window.addEventFilter]. */
fun <T: Event> Window.onEventUp(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    addEventFilter(eventType, eventHandler)
    return Subscription { removeEventFilter(eventType, eventHandler) }
}

/** Equivalent to [Node.addEventHandler]. */
fun <T: Event> Node.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    addEventHandler(eventType, eventHandler)
    return Subscription { removeEventHandler(eventType, eventHandler) }
}

/** Equivalent to [Node.addEventFilter]. */
fun <T: Event> Node.onEventUp(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    addEventFilter(eventType, eventHandler)
    return Subscription { removeEventFilter(eventType, eventHandler) }
}