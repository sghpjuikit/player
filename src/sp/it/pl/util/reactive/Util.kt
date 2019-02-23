package sp.it.pl.util.reactive

import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings.size
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableSet
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.Window
import org.reactfx.EventStreams
import org.reactfx.Subscription
import sp.it.pl.util.async.runLater
import sp.it.pl.util.dev.Experimental
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.identityHashCode
import java.util.function.Consumer

private typealias DisposeOn = (Subscription) -> Unit

@Experimental
fun <T,O> ObservableValue<T>.map(mapper: (T) -> O) = object: ObservableValue<O> {
    private val listeners1 by lazy { HashSet<ChangeListener<in O>>(2) }
    private val listeners2 by lazy { HashSet<InvalidationListener>(2) }

    init {
        this@map attachChanges { ov, nv ->
            val omv = mapper(ov)
            val nmv = mapper(nv)
            listeners1.forEach { it.changed(this, omv, nmv) }
            listeners2.forEach { it.invalidated(this) }
        }
    }

    override fun addListener(listener: ChangeListener<in O>) { listeners1 += listener }
    override fun removeListener(listener: ChangeListener<in O>) { listeners1 -= listener }
    override fun addListener(listener: InvalidationListener) { listeners2 += listener }
    override fun removeListener(listener: InvalidationListener) { listeners2 += listener }
    override fun getValue() = mapper(this@map.value)
}

/** Convenience method for adding this subscription to a disposer. Equivalent to: this.apply(disposerRegister) */
infix fun Subscription.on(disposerRegister: DisposeOn) = apply(disposerRegister)

/** Equivalent to [Subscription.and]. */
operator fun Subscription.plus(subscription: Subscription) = and(subscription)!!

/** Equivalent to [Subscription.and]. */
operator fun Subscription.plus(subscription: () -> Unit) = and(subscription)!!

/** Equivalent to [Subscription.and]. */
operator fun Subscription.plus(subscription: Disposer) = and(subscription)!!

/** Sets a value consumer to be fired immediately and on every value change. */
infix fun <O> ObservableValue<O>.sync(u: (O) -> Unit) = maintain(Consumer { u(it) })

/** Sets the value of this observable to the specified property immediately and on every value change. */
infix fun <O> ObservableValue<O>.syncTo(w: WritableValue<in O>): Subscription {
    w.value = value
    return this attachTo w
}

/** Sets the value the specified observable to the this property immediately and on every value change. */
infix fun <O> WritableValue<O>.syncFrom(o: ObservableValue<out O>): Subscription = o syncTo this

/** Sets the value the specified observable to the this property immediately and on every value change. */
@Experimental
fun <O> WritableValue<O>.syncFrom(o: ObservableValue<out O>, disposer: Disposer) = syncFrom(o) on disposer

/** Sets the mapped value the specified observable to the this property immediately and on every value change. */
fun <O,R> WritableValue<O>.syncFrom(o: ObservableValue<out R>, mapper: (R) -> O): Subscription {
    value = mapper(o.value)
    val l = ChangeListener<R> { _, _, nv -> value = mapper(nv) }
    o.addListener(l)
    return Subscription { o.removeListener(l) }
}

/** Sets the mapped value the specified observable to the this property immediately and on every value change. */
@Experimental
fun <O,R> WritableValue<O>.syncFrom(o: ObservableValue<out R>, disposer: DisposeOn, mapper: (R) -> O) = syncFrom(o, mapper) on disposer

/** Sets a value consumer to be fired on every value change. */
infix fun <O> ObservableValue<O>.attach(u: (O) -> Unit): Subscription {
    val l = ChangeListener<O> { _, _, nv -> u(nv) }
    addListener(l)
    return Subscription { removeListener(l) }
}

/** Sets a value consumer to be fired on every value change. */
@Experimental
fun <O> ObservableValue<O>.attach(disposer: DisposeOn, u: (O) -> Unit) = attach(u) on disposer

/** Sets the value the specified observable to the this property on every value change. */
infix fun <O> ObservableValue<O>.attachTo(w: WritableValue<in O>): Subscription {
    val l = ChangeListener<O> { _, _, nv -> w.value = nv }
    this.addListener(l)
    return Subscription { this.removeListener(l) }
}
/** Sets the mapped value the specified observable to the this property on every value change. */
infix fun <O> WritableValue<O>.attachFrom(o: ObservableValue<out O>): Subscription = o attachTo this

/** Sets a value consumer to be fired on every value change of either observables. */
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

/** Sets a value consumer to be fired immediately and on every value change of either observables. */
fun <O1, O2> syncTo(o1: ObservableValue<O1>, o2: ObservableValue<O2>, u: (O1, O2) -> Unit): Subscription {
    u(o1.value, o2.value)
    return attachTo(o1, o2, u)
}

/** Sets a value consumer to be fired on every value change of either observables. */
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

/** Sets a value consumer to be fired immediately and on every value change of either observables. */
fun <O1, O2, O3> syncTo(o1: ObservableValue<O1>, o2: ObservableValue<O2>, o3: ObservableValue<O3>, u: (O1, O2, O3) -> Unit): Subscription {
    u(o1.value, o2.value, o3.value)
    return attachTo(o1, o2, o3, u)
}

/** Sets a value change consumer to be fired on every value change. */
infix fun <O> ObservableValue<O>.attachChanges(u: (O, O) -> Unit): Subscription {
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
    val l = ListChangeListener<T> {
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
    val l = ListChangeListener<T> {
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

/** Sets action to be invoked immediately and on every change of this observable or the extracted observable. */
fun <O, R> ObservableValue<O>.syncInto(extractor: (O) -> ObservableValue<R>, action: (R?) -> Unit): Subscription {
    val inner = Disposer()
    val outer = this sync {
        inner()
        if (it==null) action(null)
        else extractor(it) sync { action(it) } on inner
    }
    return outer+inner
}

/** Sets action to be resubscribed immediately and on every change of this observable or the extracted observable. */
fun <O, R> ObservableValue<O>.syncIntoWhile(extractor: (O) -> ObservableValue<R>, action: (R?) -> Subscription): Subscription {
    val superInner = Disposer()
    val inner = Disposer()
    val outer = this sync {
        inner()
        if (it==null) {
            superInner()
            superInner += action(null)
        } else {
            inner += extractor(it) sync {
                superInner()
                superInner += action(it)
            }
        }
    }
    return outer+inner+superInner
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

/** [sync1If], that does not run immediately (even if the value passes the condition). */
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
fun <T> ObservableValue<T>.sync1If(condition: (T) -> Boolean, action: (T) -> Unit): Subscription {
    return if (condition(value)) {
        action(value)
        Subscription {}
    } else {
        attach1If(condition, action)
    }
}

/** [attach1If] testing the value is not null. */
fun <T> ObservableValue<T>.attach1IfNonNull(action: (T) -> Unit) = sync1If({ it!=null }, action)

/** [sync1If] testing the value is not null. */
fun <T> ObservableValue<T>.sync1IfNonNull(action: (T) -> Unit) = sync1If({ it!=null }, action)


/**
 * Runs action once node is in scene graph and after proper layout, i.e., its scene being non null and after executing
 * a layout pass. The action will never run in current scene pulse as [runLater] will be invoked at least once.
 */
fun Node.sync1IfInScene(action: () -> Unit): Subscription {
    val disposer = Disposer()
    fun Node.onAddedToScene(action: () -> Unit): Subscription = sceneProperty().sync1IfNonNull {
        runLater {
            if (scene==null) {
                onAddedToScene(action) on disposer
            } else {
                action()
            }
        }
    }
    onAddedToScene(action) on disposer
    return Subscription { disposer() }
}

fun sync1IfImageLoaded(image: Image, action: Runnable) = image.progressProperty().sync1If({ it.toDouble()==1.0 }) { action() }

fun doIfImageLoaded(imageView: ImageView, action: Consumer<Image>) = imageView.imageProperty().syncInto(Image::progressProperty) { p -> if (p==1.0) action(imageView.image) }

/** Call specified handler every time an item in this list changes */
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

/** Call specified handler for every current and future item of this collection. */
fun <T> ObservableList<T>.onItemDo(block: (T) -> Unit): Subscription {
    forEach { block(it) }
    return onItemAdded { block(it) }
}

/**
 * Subscribe specified handler for every current and future item of this collection until it is removed or unsubscribed.
 * Collection must not contain duplicates (defined as [Any.identityHashCode]).
 */
fun <T> ObservableList<T>.onItemSync(subscriber: (T) -> Subscription): Subscription {
    fun T.id() = identityHashCode()
    val ds = HashMap<Int, Subscription>(size)
    val disposer = Disposer()
    forEach { ds[it.id()] = subscriber(it) }
    onItemRemoved { ds.remove(it.id())?.unsubscribe() } on disposer
    onItemAdded { if (ds.containsKey(it.id())) fail { "Duplicate=$it" } else ds[it.id()] = subscriber(it) } on disposer
    return Subscription {
        disposer()
        ds.forEach { it.value.unsubscribe() }
    }
}

/** Equivalent to [Window.addEventHandler]. */
fun <T: Event> Window.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    val handler = EventHandler<T> { eventHandler(it) }
    addEventHandler(eventType, handler)
    return Subscription { removeEventHandler(eventType, handler) }
}

/** Equivalent to [Window.addEventFilter]. */
fun <T: Event> Window.onEventUp(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    val handler = EventHandler<T> { eventHandler(it) }
    addEventFilter(eventType, handler)
    return Subscription { removeEventFilter(eventType, handler) }
}

/** Equivalent to [Node.addEventHandler]. */
fun <T: Event> Node.onEventDown(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    val handler = EventHandler<T> { eventHandler(it) }
    addEventHandler(eventType, handler)
    return Subscription { removeEventHandler(eventType, handler) }
}

/** Equivalent to [Node.addEventFilter]. */
fun <T: Event> Node.onEventUp(eventType: EventType<T>, eventHandler: (T) -> Unit): Subscription {
    val handler = EventHandler<T> { eventHandler(it) }
    addEventFilter(eventType, handler)
    return Subscription { removeEventFilter(eventType, handler) }
}