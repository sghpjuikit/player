@file:JvmName("Util")
@file:Suppress("unused")

package util.reactive

import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.Event
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.stage.Screen
import org.reactfx.EventStreams
import org.reactfx.Subscription
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Predicate

/** Sets a value consumer to be fired immediately and on every value change. */
infix fun <O> ObservableValue<O>.sync(u: (O) -> Unit) = maintain(Consumer { u(it) })

/** Sets a value consumer to be fired on every value change. */
infix fun <O> ObservableValue<O>.attach(u: (O) -> Unit): Subscription {
    val l = ChangeListener<O> { _, _, nv -> u(nv) }
    addListener(l)
    return Subscription { removeListener(l) }
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

fun <O, V> ObservableValue<O>.maintain(m: (O) -> V, u: Consumer<in V>): Subscription {
    u.accept(m(this.value))
    return EventStreams.valuesOf(this).map(m).subscribe(u)
}

fun <O> ObservableValue<O>.maintain(u: Consumer<O>): Subscription {
    val l = ChangeListener<O> { _, _, nv -> u.accept(nv) }
    u.accept(this.value)
    this.addListener(l)
    return Subscription { this.removeListener(l) }
}

fun <O, V> ObservableValue<O>.maintain(m: (O) -> V, w: WritableValue<in V>): Subscription {
    w.value = m(this.value)
    val l = ChangeListener<O> { _, _, nv -> w.value = m(nv) }
    this.addListener(l)
    return Subscription { this.removeListener(l) }
}

fun <O1, O2> maintain(o1: ObservableValue<O1>, o2: ObservableValue<O2>, u: BiConsumer<in O1, in O2>): Subscription {
    val l1 = ChangeListener<O1> { _, _, nv -> u.accept(nv, o2.value) }
    val l2 = ChangeListener<O2> { _, _, nv -> u.accept(o1.value, nv) }
    u.accept(o1.value, o2.value)
    o1.addListener(l1)
    o2.addListener(l2)
    return Subscription {
        o1.removeListener(l1)
        o2.removeListener(l2)
    }
}

infix fun <O> ObservableValue<O>.maintain(w: WritableValue<in O>): Subscription {
    w.value = this.value
    val l = ChangeListener<O> { _, _, nv -> w.value = nv }
    this.addListener(l)
    return Subscription { this.removeListener(l) }
}

fun <T> sizeOf(list: ObservableList<T>, action: Consumer<in Int>): Subscription {
    val l = ListChangeListener<T> { _ -> action.accept(list.size) }
    l.onChanged(null)
    list.addListener(l)
    return Subscription { list.removeListener(l) }
}

/**
 * Prints the value to console immediately and then on every change.
 */
fun <T> ObservableValue<T>.printOnChange(): Subscription = maintain(Consumer { println(it) })

/**
 * [.doOnceIf]
 * testing the value for nullity.<br></br>
 * The action executes when value is not null.
 */
fun <T> doOnceIfNonNull(property: ObservableValue<T>, action: Consumer<T>): Subscription {
    return doOnceIf(property, Predicate { Objects.nonNull(it) }, action)
}

/**
 * [.doOnceIf]
 * testing the image for loading being complete.<br></br>
 * The action executes when image finishes loading. Note that image may be constructed in a way that makes it
 * loaded at once, in which case the action runs immediately - in this method.

 * @throws java.lang.RuntimeException if any param null
 */
fun <T> doOnceIfImageLoaded(image: Image, action: Runnable): Subscription {
    return doOnceIf(image.progressProperty(), Predicate { progress -> progress.toDouble()==1.0 }, Consumer { _ -> action.run() })
}

/**
 * Runs action (consuming the property's value) as soon as the condition is met. Useful to execute initialization,
 * for example to wait for nonnull value.
 *
 *
 * The action runs immediately if current value already meets the condition. Otherwise registers a one-time
 * listener, which will run the action when the value changes to such that the condition is met.
 *
 *
 * It is guaranteed:
 *
 *  *  action executes at most once
 *
 * It is not guaranteed:
 *
 *  *  action will execute - it maye never execute either because the value never met the condition or the
 * listener was unregistered manually using the returned subscription.
 *

 * @param property observable value to consume
 * *
 * @param condition test the value must pass for the action to execute
 * *
 * @param action action receiving the value and that runs exactly once when the condition is first met
 */
fun <T> doOnceIf(property: ObservableValue<T>, condition: Predicate<in T>, action: Consumer<T>): Subscription {
    if (condition.test(property.value)) {
        action.accept(property.value)
        return Subscription {}
    } else {
        val l = singletonListener(property, condition, action)
        property.addListener(l)
        return Subscription { property.removeListener(l) }
    }
}

fun <T> singletonListener(property: ObservableValue<T>, condition: Predicate<in T>, action: Consumer<T>): ChangeListener<T> {
    return object: ChangeListener<T> {
        override fun changed(observable: ObservableValue<out T>, ov: T, nv: T) {
            if (condition.test(nv)) {
                action.accept(nv)
                property.removeListener(this)
            }
        }
    }
}

fun <T> installSingletonListener(property: ObservableValue<T>, condition: Predicate<in T>, action: Consumer<T>): Subscription {
    val l = singletonListener(property, condition, action)
    property.addListener(l)
    return Subscription { property.removeListener(l) }
}

/** Creates list change listener which calls an action for every added or removed item.  */
fun onScreenChange(onChange: Consumer<in Screen>): Subscription {
    val l = listChangeListener(ListChangeListener<Screen> { change -> if (change.wasAdded()) onChange.accept(change.addedSubList[0]) })
    Screen.getScreens().addListener(l)
    return Subscription { Screen.getScreens().removeListener(l) }
}

fun <T> listChangeListener(onAdded: ListChangeListener<T>, onRemoved: ListChangeListener<T>): ListChangeListener<T> {
    return ListChangeListener { change ->
        while (change.next()) {
            if (!change.wasPermutated() && !change.wasUpdated()) {
                if (change.wasAdded()) onAdded.onChanged(change)
                if (change.wasAdded()) onRemoved.onChanged(change)
            }
        }
    }
}

/**
 * Creates list change listener which calls the provided listeners on every change.
 *
 * This is a convenience method taking care of the while(change.next()) code pattern explained in
 * [javafx.collections.ListChangeListener.Change].
 */
fun <T> listChangeListener(onChange: ListChangeListener<T>): ListChangeListener<T> {
    return ListChangeListener { change ->
        while (change.next()) {
            onChange.onChanged(change)
        }
    }
}

/** Creates list change listener which calls an action for every added or removed item. */
@JvmOverloads
fun <T> listChangeHandlerEach(addedHandler: Consumer<T>, removedHandler: Consumer<T> = Consumer {}): ListChangeListener<T> {
    return ListChangeListener { change ->
        while (change.next()) {
            if (!change.wasPermutated() && !change.wasUpdated()) {
                if (change.wasAdded()) change.addedSubList.forEach(addedHandler)
                if (change.wasRemoved()) change.removed.forEach(removedHandler)
            }
        }
    }
}

/** Creates list change listener which calls an action added or removed item list. */
@JvmOverloads
fun <T> listChangeHandler(addedHandler: Consumer<List<T>>, removedHandler: Consumer<List<T>> = Consumer {}): ListChangeListener<T> {
    return ListChangeListener { change ->
        while (change.next()) {
            if (!change.wasPermutated() && !change.wasUpdated()) {
                if (change.wasAdded()) addedHandler.accept(change.addedSubList)
                if (change.wasRemoved()) removedHandler.accept(change.removed)
            }
        }
    }
}


fun <T: Event> Node.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    addEventHandler(eventType, eventHandler);
    return Subscription { removeEventHandler(eventType, eventHandler) }
}

fun <T: Event> Node.onEventUp(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    addEventFilter(eventType, eventHandler);
    return Subscription { removeEventFilter(eventType, eventHandler) }
}