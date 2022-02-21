package sp.it.util.reactive

import java.util.IdentityHashMap
import java.util.function.Consumer
import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.binding.Bindings.size
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.collections.ListChangeListener
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract
import sp.it.util.access.OrV
import sp.it.util.async.runLater
import sp.it.util.collections.ObservableListRO
import sp.it.util.dev.Experimental
import sp.it.util.dev.fail
import sp.it.util.functional.kt
import sp.it.util.functional.net
import sp.it.util.identityHashCode

private abstract class MappedObservableValue<O>: ObservableValue<O> {
   protected val listeners1 by lazy { HashSet<ChangeListener<in O>>(2) }
   protected val listeners2 by lazy { HashSet<InvalidationListener>(2) }

   override fun addListener(listener: ChangeListener<in O>) {
      listeners1 += listener
      updateListening()
   }

   override fun removeListener(listener: ChangeListener<in O>) {
      listeners1 -= listener
      updateListening()
   }

   override fun addListener(listener: InvalidationListener) {
      listeners2 += listener
      updateListening()
   }

   override fun removeListener(listener: InvalidationListener) {
      listeners2 -= listener
      updateListening()
   }

   protected abstract fun updateListening()

}

/**
 * Maps this [ObservableValue] into one that contains values mapped by [mapper].
 * When the returned observable is observed, the mapping is eager, so it happens on every change of this observable.
 * When the returned observable is not observed, the mapping is lazy, so it happens on every [getValue].
 *
 * * The returned observable holds forever reference to this observable.
 * * This observable holds reference to the returned observable if the returned observable is observed.
 *
 * Therefore, to garbage collect the observable:
 * * If this and returned observable share lifecycle, no action is necessary
 * * If this observable outlives the returned observable, remove all listeners from the returned observable at the end of its life
 * * If the returned observable outlives this observable, remove any reference to this observable at the end of its life
 */
infix fun <T, O> ObservableValue<T>.map(mapper: (T) -> O): ObservableValue<O> = object: MappedObservableValue<O>() {
   private var mv: O = computeValue()
   private val s = Subscribed {
      mv = mapper(this@map.value)
      this@map sync { nv ->
         val omv = mv
         val nmv = mapper(nv)
         if (omv!=nmv) {
            mv = nmv
            listeners1.forEach { it.changed(this, omv, nmv) }
            listeners2.forEach { it.invalidated(this) }
         }
      }
   }

   private fun computeValue(): O = mapper(this@map.value)

   override fun updateListening() = s.subscribe(listeners1.isNotEmpty() || listeners2.isNotEmpty())

   override fun getValue() = if (s.isSubscribed) mv else computeValue()
}

/**
 * Maps this [ObservableValue] into one that contains values mapped by [mapper].
 * When the returned observable is observed, the mapping is eager, so it happens on every change of this observable.
 * When the returned observable is not observed, the mapping is lazy, so it happens on every [getValue].
 *
 * * The returned observable holds forever reference to this observable.
 * * This observable holds reference to the returned observable if the returned observable is observed.
 *
 * Therefore, to garbage collect the observable:
 * * If this and returned observable share lifecycle, no action is necessary
 * * If this observable outlives the returned observable, remove all listeners from the returned observable at the end of its life
 * * If the returned observable outlives this observable, remove any reference to this observable at the end of its life
 */
@Suppress("SpellCheckingInspection", "UnnecessaryVariable")
infix fun <T, O> ObservableValue<T>.flatMap(mapper: (T) -> ObservableValue<O>): ObservableValue<O> = object: MappedObservableValue<O>() {
   private var mv: O = computeValue()
   private var so = Subscription()
   private val s = Subscribed {
      this@flatMap sync { nov ->
         val nmov = mapper(nov)

         so.unsubscribe()
         so = nmov sync { nv ->
            val omv = mv
            val nmv = nv
            if (omv!=nmv) {
               mv = nmv
               listeners1.forEach { it.changed(this, omv, nmv) }
               listeners2.forEach { it.invalidated(this) }
            }
         }
      }
   }

   private fun computeValue(): O = mapper(this@flatMap.value).value

   override fun updateListening() = s.subscribe(listeners1.isNotEmpty() || listeners2.isNotEmpty())

   override fun getValue() = if (s.isSubscribed) mv else computeValue()
}

/**
 * Maps this and the specified [ObservableValue] into one that contains [Pair] of values from both.
 * When the returned observable is observed, the mapping is eager, so it happens on every change of this adn that observable.
 * When the returned observable is not observed, the mapping is lazy, so it happens on every [getValue].
 *
 * * The returned observable holds forever reference to this observable.
 * * This observable holds reference to the returned observable if the returned observable is observed.
 *
 * Therefore, to garbage collect the observable:
 * * If this and returned observable share lifecycle, no action is necessary
 * * If this observable outlives the returned observable, remove all listeners from the returned observable at the end of its life
 * * If the returned observable outlives this observable, remove any reference to this observable at the end of its life
 */
infix fun <T, O> ObservableValue<T>.zip(that: ObservableValue<O>): ObservableValue<Pair<T, O>> = object: MappedObservableValue<Pair<T, O>>() {
   private var mv = computeValue()
   private val s = Subscribed {
      mv = computeValue()
      val updater = {
         val omv = mv
         val nmv = computeValue()
         if (omv!=nmv) {
            mv = nmv
            listeners1.forEach { it.changed(this, omv, nmv) }
            listeners2.forEach { it.invalidated(this) }
         }
      }
      that attach { updater() }
      this@zip attach { updater() }
   }

   private fun computeValue(): Pair<T, O> = this@zip.value to that.value

   override fun updateListening() = s.subscribe(listeners1.isNotEmpty() || listeners2.isNotEmpty())

   override fun getValue() = if (s.isSubscribed) mv else computeValue()
}

/** Sets a block to be fired immediately and on every value change. */
infix fun <O> ObservableValue<O>.sync(block: (O) -> Unit): Subscription {
   block(value)
   return attach { block(value) }
}

/** Java convenience method equivalent to [sync]. */
fun <O> ObservableValue<O>.syncC(block: Consumer<in O>) = sync(block.kt)

/** Sets a disposable block to be fired on every value change. The block is disposed on the next change. */
infix fun <O> ObservableValue<O>.attachWhile(block: (O) -> Subscription): Subscription {
   val inner = Disposer()
   val outer = attach {
      inner()
      inner += block(it)
   }
   return outer + inner
}

/** Sets a disposable block to be fired on every non-null value change. The block is disposed on the next change (null or not). */
infix fun <O: Any> ObservableValue<O?>.attachNonNullWhile(block: (O) -> Subscription) = attachWhile { it ->
   it?.net(block).orEmpty()
}

/** Sets a disposable block to be fired immediately and on every value change. The block is disposed on the next change. */
infix fun <O> ObservableValue<O>.syncWhile(block: (O) -> Subscription): Subscription {
   val inner = Disposer()
   val outer = sync {
      inner()
      inner += block(it)
   }
   return outer + inner
}

/** Sets a disposable block to be fired immediately and on every non-null value change. The block is disposed on the next change (null or not). */
infix fun <O: Any> ObservableValue<O?>.syncNonNullWhile(block: (O) -> Subscription) = syncWhile { it ->
   it?.net(block).orEmpty()
}

/** Sets the value of this observable to the specified property immediately and on every value change. */
infix fun <O> ObservableValue<O>.syncTo(w: WritableValue<in O>) = sync { w.value = it }

/** Sets the value of the specified observable to this property immediately and on every value change. */
infix fun <O> WritableValue<O>.syncFrom(o: ObservableValue<out O>) = o syncTo this

/** Sets the value of the specified observable to this property immediately and on every value change. */
@Experimental("Questionable API")
fun <O> WritableValue<O>.syncFrom(o: ObservableValue<out O>, disposer: Disposer) = syncFrom(o) on disposer

/** Sets a block to be fired on every value change. */
infix fun <O> ObservableValue<O>.attach(block: (O) -> Unit): Subscription {
   val l = ChangeListener<O> { _, _, nv -> block(nv) }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Sets a block to be fired on every value change. */
@Experimental("Questionable API")
fun <O> ObservableValue<O>.attach(disposer: Unsubscriber, block: (O) -> Unit) = attach(block) on disposer

/** Sets a block to be fired on every value change. */
infix fun <O> ObservableValue<O>.attachChanges(block: (O, O) -> Unit): Subscription {
   val l = ChangeListener<O> { _, ov, nv -> block(ov, nv) }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Sets the value of the specified observable to this property on every value change. */
infix fun <O> ObservableValue<O>.attachTo(w: WritableValue<in O>) = attach { w.value = it }

/** Sets the value of this specified observable to this property on every value change. */
infix fun <O> WritableValue<O>.attachFrom(o: ObservableValue<out O>): Subscription = o attachTo this

/** Sets a block to be fired if the value is true on every value change. */
infix fun ObservableValue<Boolean>.attachTrue(block: (Boolean) -> Unit) = attach { if (it) block(true) }

/** Sets a block to be fired if the value is false on every value change. */
infix fun ObservableValue<Boolean>.attachFalse(block: (Boolean) -> Unit) = attach { if (!it) block(false) }

/** Sets a block to be fired if the value is true immediately and on every value change. */
infix fun ObservableValue<Boolean>.syncTrue(block: (Boolean) -> Unit) = sync { if (it) block(true) }

/** Sets a block to be fired if the value is false immediately and on every value change. */
infix fun ObservableValue<Boolean>.syncFalse(block: (Boolean) -> Unit) = sync { if (!it) block(false) }

/** Sets a block to be fired immediately and on every list size change. */
infix fun <T> ObservableList<T>.syncSize(block: (Int) -> Unit): Subscription {
   var s = -1
   val l = ListChangeListener<T> {
      val os = s
      val ns = size
      if (os!=ns) block(ns)
      s = ns
   }
   l.onChanged(null)
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Sets a block to be fired on every list size change. */
infix fun <T> ObservableList<T>.attachSize(block: (Int) -> Unit): Subscription {
   var s = size
   val l = ListChangeListener<T> {
      val os = s
      val ns = size
      if (os!=ns) block(ns)
      s = ns
   }
   l.onChanged(null)
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Binds the two properties bi-directionally using this value as the initial value. */
infix fun <O> Property<O>.syncBiTo(w: Property<O>): Subscription {
   w.value = value
   bindBidirectional(w)
   return Subscription { unbindBidirectional(w) }
}

/** Binds the two properties bi-directionally using the specified value as the initial value. */
infix fun <O> Property<O>.syncBiFrom(w: Property<O>): Subscription {
   value = w.value
   bindBidirectional(w)
   return Subscription { unbindBidirectional(w) }
}

/** Binds the two properties bi-directionally using the specified value as the initial value. The specified value may be set to [OrV.State.OVERRIDE] if this changes */
infix fun <T> Property<T>.syncBiFromWithOverride(or: OrV<T>): Subscription {
   return Subscription(
      this syncFrom or,
      this sync {
         val wasSame = it==or.value
         if (!wasSame) or.value = it
         if (!or.override.value && !wasSame) or.override.value = true
      }
   )
}

/** @returns observable size of this list */
fun <T> ObservableList<T>.sizes() = size(this)!!

/** @returns observable size of this set */
fun <T> ObservableSet<T>.sizes() = size(this)!!

/** Sets block to be invoked immediately and on every change of the extracted observable of the value of this observable. */
fun <O, R> ObservableValue<O>.syncInto(extractor: (O) -> ObservableValue<R>, block: (R?) -> Unit): Subscription {
   val inner = Disposer()
   val outer = this sync {
      inner()
      if (it==null) block(null)
      else extractor(it) sync { block(it) } on inner
   }
   return outer + inner
}

/** Sets block to be resubscribed immediately and on every change of the extracted observable of the value of this observable until value changes. */
fun <O, R> ObservableValue<O>.syncIntoWhile(extractor: (O) -> ObservableValue<R>, block: (R?) -> Subscription): Subscription {
   val superInner = Disposer()
   val inner = Disposer()
   val outer = this sync {
      inner()
      if (it==null) {
         superInner()
         superInner += block(null)
      } else {
         inner += extractor(it) sync {
            superInner()
            superInner += block(it)
         }
      }
   }
   return outer + inner + superInner
}

/** Sets block to be resubscribed immediately and on every non-null change of the extracted observable of the value until value changes. */
fun <O: Any?, R: Any> ObservableValue<O>.syncNonNullIntoWhile(extractor: (O) -> ObservableValue<R?>, block: (R) -> Subscription) = syncIntoWhile(extractor) { it?.net(block).orEmpty() }

/** [sync1If], that does not run immediately (even if the value passes the condition). */
@OptIn(ExperimentalContracts::class)
inline fun <T> ObservableValue<T>.attach1If(crossinline condition: (T) -> Boolean, crossinline block: (T) -> Unit): Subscription {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   val l = object: ChangeListener<T> {
      override fun changed(observable: ObservableValue<out T>, ov: T, nv: T) {
         if (condition(nv)) {
            block(nv)
            removeListener(this)
         }
      }
   }
   addListener(l)
   return Subscription { removeListener(l) }
}

/**
 * Runs block (consuming the property's value) as soon as the condition is met. Useful to execute initialization,
 * for example to wait for nonnull value.
 *
 * The block runs immediately if current value already meets the condition. Otherwise, registers a one-time
 * listener, which will run the block when the value changes to such that the condition is met.
 *
 * It is guaranteed:
 *  *  block executes at most once
 *
 * It is not guaranteed:
 *  *  block will execute, because the value may never meet the condition, or it was unsubscribed before it happened
 *
 * @param condition test the value must pass for the action to execute
 * @param block action receiving the value as argument and that runs exactly once when the condition is first met
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> ObservableValue<T>.sync1If(crossinline condition: (T) -> Boolean, crossinline block: (T) -> Unit): Subscription {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   return if (condition(value)) {
      block(value)
      Subscription {}
   } else {
      attach1If(condition, block)
   }
}

/** [attach1If] testing the value is not null. */
@OptIn(ExperimentalContracts::class)
inline fun <T: Any> ObservableValue<T?>.attach1IfNonNull(crossinline block: (T) -> Unit): Subscription {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   return sync1If({ it!=null }, { block(it!!) })
}

/** [sync1If] testing the value is not null. */
@OptIn(ExperimentalContracts::class)
fun <T: Any> ObservableValue<T?>.sync1IfNonNull(block: (T) -> Unit): Subscription {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   return sync1If({ it!=null }, { block(it!!) })
}

/**
 * Runs block once node is in scene graph and after proper layout, i.e., its scene being non-null and after executing
 * a layout pass. The block will never run in current scene pulse as [runLater] will be invoked at least once.
 */
@OptIn(ExperimentalContracts::class)
fun Node.sync1IfInScene(block: () -> Unit): Subscription {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   var disposed = false
   val disposer = Disposer().apply {  }
   fun Node.onAddedToScene(action: () -> Unit): Subscription = sceneProperty().sync1IfNonNull {
      runLater {
         if (!disposed) {
            if (scene==null) onAddedToScene(action) on disposer
            else action()
         }
      }
   }
   onAddedToScene(block) on disposer
   return Subscription {
      disposed = true
      disposer()
   }
}

@OptIn(ExperimentalContracts::class)
inline fun Image.sync1IfImageLoaded(crossinline block: () -> Unit): Subscription {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   return progressProperty().sync1If({ it.toDouble()==1.0 }) { block() }
}

@OptIn(ExperimentalContracts::class)
inline fun ImageView.doIfImageLoaded(crossinline block: (Image?) -> Unit): Subscription {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   return imageProperty().syncInto(Image::progressProperty) { if (it==1.0) block(image) }
}

/**
 * Call specified block on every invalidation, using [Observable.addListener].
 *
 * Change vs. Invalidation:
 *
 * The number of events is the same as when using concrete listeners, such as [ChangeListener], [ListChangeListener] or
 * [SetChangeListener].
 *
 * Event avoidance:
 * * [WritableValue.setValue] with the same (as per equals) value as already set will not fire event.
 * * [ObservableList.addAll], [ObservableList.clear], [ObservableList.setAll] produce only one event.
 * * [ObservableSet], unlike list, fires events for each item so [ObservableSet.clear] and the like fire as many events
 * as there is items added/removed.
 */
fun Observable.onChange(block: () -> Unit): Subscription {
   val l = InvalidationListener { block() }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** [Observable.onChange] that also invokes the block immediately. */
fun Observable.onChangeAndNow(block: () -> Unit): Subscription {
   block()
   return onChange(block)
}

/** Sets a disposable block to be on every change. The block is disposed on the next change. */
infix fun Observable.attachWhile(block: () -> Subscription): Subscription {
   val inner = Disposer()
   val outer = onChange {
      inner()
      inner += block()
   }
   return outer + inner
}

/** Sets a disposable block to be on every change to `true`. The block is disposed on the next change. */
infix fun ObservableValue<Boolean?>.attachWhileTrue(block: () -> Subscription): Subscription = attachWhile { if (value==true) block() else Subscription() }

/** Sets a disposable block to be on every change to `false`. The block is disposed on the next change. */
infix fun ObservableValue<Boolean?>.attachWhileFalse(block: () -> Subscription): Subscription = attachWhile { if (value==false) block() else Subscription() }

/** Sets a disposable block to be on every change to `null`. The block is disposed on the next change. */
infix fun <T> ObservableValue<T?>.attachWhileNull(block: () -> Subscription): Subscription = attachWhile { if (value==null) block() else Subscription() }

/** Sets a disposable block to be fired immediately and on every change. The block is disposed on the next change. */
infix fun Observable.syncWhile(block: () -> Subscription): Subscription {
   val inner = Disposer()
   val outer = onChangeAndNow {
      inner()
      inner += block()
   }
   return outer + inner
}

/** Sets a disposable block to be fired immediately if `true` and on every change to `true`. The block is disposed on the next change. */
infix fun ObservableValue<Boolean?>.syncWhileTrue(block: () -> Subscription): Subscription = syncWhile { if (value==true) block() else Subscription() }

/** Sets a disposable block to be fired immediately if `false` and on every change to `false`. The block is disposed on the next change. */
infix fun ObservableValue<Boolean?>.syncWhileFalse(block: () -> Subscription): Subscription = syncWhile { if (value==false) block() else Subscription() }

/** Sets a disposable block to be fired immediately if `null` and on every change to `null`. The block is disposed on the next change. */
infix fun <T> ObservableValue<T?>.syncWhileNull(block: () -> Subscription): Subscription = syncWhile { if (value==null) block() else Subscription() }

/** Call specified block every time an item is added to this list passing it as argument */
fun <T> ObservableList<T>.onItemAdded(block: (T) -> Unit): Subscription = onItemsAdded { it.forEach(block) }

/** Call specified block every time an item is added to this list passing it as argument */
fun <T> ObservableListRO<T>.onItemAdded(block: (T) -> Unit): Subscription = onItemsAdded { it.forEach(block) }

/** Call specified block every time 1-N items is added to this list passing the added items as argument */
fun <T> ObservableList<T>.onItemsAdded(block: (List<T>) -> Unit): Subscription = onItemsAdded(this::addListener, this::removeListener, block)

/** Call specified block every time 1-N items is added to this list passing the added items as argument */
fun <T> ObservableListRO<T>.onItemsAdded(block: (List<T>) -> Unit): Subscription = onItemsAdded(this::addListener, this::removeListener, block)

private fun <T> onItemsAdded(addListener: (ListChangeListener<T>) -> Unit, removeListener: (ListChangeListener<T>) -> Unit, block: (List<T>) -> Unit): Subscription {
   val l = ListChangeListener<T> {
      while (it.next()) {
         if (!it.wasPermutated() && !it.wasUpdated() && it.wasAdded())
            block(it.addedSubList)
      }
   }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Call specified block every time an item is added to this set passing it as argument */
fun <T> ObservableSet<T>.onItemAdded(block: (T) -> Unit): Subscription {
   val l = SetChangeListener<T> {
      if (it.wasAdded()) block(it.elementAdded)
   }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Call specified block every time 1-N items is removed from this list passing the removed items as argument */
fun <K, V> ObservableMap<K, V>.onItemAdded(block: (K, V) -> Unit): Subscription {
   val l = MapChangeListener<K, V> {
      if (it.wasAdded()) block(it.key, it.valueAdded)
   }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Call specified block every time an item is removed from this list passing it as argument */
fun <T> ObservableList<T>.onItemRemoved(block: (T) -> Unit): Subscription = onItemsRemoved { it.forEach(block) }

/** Call specified block every time an item is removed from this list passing it as argument */
fun <T> ObservableListRO<T>.onItemRemoved(block: (T) -> Unit): Subscription = onItemsRemoved { it.forEach(block) }

/** Call specified block every time 1-N items is removed from this list passing the removed items as argument */
fun <T> ObservableList<T>.onItemsRemoved(block: (List<T>) -> Unit): Subscription = onItemsRemoved(this::addListener, this::removeListener, block)

/** Call specified block every time 1-N items is removed from this list passing the removed items as argument */
fun <T> ObservableListRO<T>.onItemsRemoved(block: (List<T>) -> Unit): Subscription = onItemsRemoved(this::addListener, this::removeListener, block)

private fun <T> onItemsRemoved(addListener: (ListChangeListener<T>) -> Unit, removeListener: (ListChangeListener<T>) -> Unit, block: (List<T>) -> Unit): Subscription {
   val l = ListChangeListener<T> {
      while (it.next()) {
         if (!it.wasPermutated() && !it.wasUpdated() && it.wasRemoved())
            block(it.removed)
      }
   }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Call specified block every time an item is removed from this set passing it as argument */
fun <K, V> ObservableMap<K, V>.onItemRemoved(block: (K, V) -> Unit): Subscription {
   val l = MapChangeListener<K, V> {
      if (it.wasRemoved()) block(it.key, it.valueRemoved)
   }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Call specified block every time an item is removed from this set passing it as argument */
fun <T> ObservableSet<T>.onItemRemoved(block: (T) -> Unit): Subscription {
   val l = SetChangeListener<T> {
      if (it.wasRemoved()) block(it.elementRemoved)
   }
   addListener(l)
   return Subscription { removeListener(l) }
}

/** Call specified block for every current and future item of this list. */
fun <T> ObservableList<T>.onItemSync(block: (T) -> Unit): Subscription {
   forEach(block)
   return onItemAdded(block)
}

/** Call specified block for every current and future item of this list. */
fun <T> ObservableListRO<T>.onItemSync(block: (T) -> Unit): Subscription {
   forEach(block)
   return onItemAdded(block)
}

/** Call specified block for every current and future item of this set. */
fun <T> ObservableSet<T>.onItemSync(block: (T) -> Unit): Subscription {
   forEach(block)
   return onItemAdded(block)
}

/** Call specified block for every current and future item of this set. */
fun <K, V> ObservableMap<K, V>.onItemSync(block: (K, V) -> Unit): Subscription {
   forEach(block)
   return onItemAdded(block)
}

/**
 * Subscribe specified disposable block for every current and future item of this collection until it is removed or unsubscribed.
 * Collection must not contain duplicates (as per [Any.identityHashCode]).
 */
fun <T> ObservableList<T>.onItemSyncWhile(subscriber: (T) -> Subscription): Subscription {
   val ds = IdentityHashMap<T, Subscription>(size)
   val disposer = Disposer()
   forEach { ds[it] = subscriber(it) }
   onItemRemoved { ds.remove(it)?.unsubscribe() } on disposer
   onItemAdded { if (ds.containsKey(it)) fail { "Duplicate=$it" } else ds[it] = subscriber(it) } on disposer
   return Subscription {
      disposer()
      ds.forEach { it.value.unsubscribe() }
      ds.clear()
   }
}

/**
 * Subscribe specified disposable block for every current and future item of this collection until it is removed or unsubscribed.
 * Collection must not contain duplicates (as per [Any.identityHashCode]).
 */
fun <T> ObservableSet<T>.onItemSyncWhile(subscriber: (T) -> Subscription): Subscription {
   val ds = IdentityHashMap<T, Subscription>(size)
   val disposer = Disposer()
   forEach { ds[it] = subscriber(it) }
   onItemRemoved { ds.remove(it)?.unsubscribe() } on disposer
   onItemAdded { if (ds.containsKey(it)) fail { "Duplicate=$it" } else ds[it] = subscriber(it) } on disposer
   return Subscription {
      disposer()
      ds.forEach { it.value.unsubscribe() }
      ds.clear()
   }
}