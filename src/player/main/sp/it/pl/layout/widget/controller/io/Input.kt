package sp.it.pl.layout.widget.controller.io

import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.main.APP
import sp.it.util.dev.Idempotent
import sp.it.util.dev.failIf
import sp.it.util.reactive.Subscription
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.jvmErasure
import sp.it.util.type.raw
import sp.it.util.type.typeResolved
import java.util.HashMap
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import sp.it.pl.layout.widget.controller.io.Output.Id
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.type.estimateRuntimeType

open class Input<T>: Put<T> {

   private val sources = HashMap<Output<out T>, Subscription>()

   constructor(name: String, type: VType<T>, initialValue: T, action: (T) -> Unit): super(type, name, initialValue) {
      attach(action)
   }

   /** @return true if this input can receive values from given output */
   open fun isAssignable(output: Output<*>): Boolean = isAssignable(output.type)

   /** @return true if this input can receive values of the specified type */
   open fun isAssignable(oType: VType<*>): Boolean = when {
      oType isSubtypeOf type -> true
      type.jvmErasure==List::class && oType.jvmErasure==List::class -> {
         oType.listType().isSubtypeOf(type.listType())
      }
      type.jvmErasure==List::class -> {
         isAssignable(oType.jvmErasure, type.listType().raw)
      }
      else -> false
   }

   private fun isAssignable(type1: KClass<*>, type2: KClass<*>) = type1.isSubclassOf(type2) || type2.isSubclassOf(type1)

   /** @return true if this input can receive the specified value */
   fun isAssignable(value: Any?): Boolean = when {
      value==null -> type.isNullable
      type.jvmErasure==List::class -> type.listType().isSubtypeOf(value.asIs<List<*>>().estimateRuntimeType().type)
      else -> type.jvmErasure.isInstance(value)
   }

   private fun monitor(output: Output<out T>): Subscription {
      failIf(!isAssignable(output)) { "Input<$type> can not bind to put<${output.type}>" }

      return output.sync { v ->
         when {
            output.type isSubtypeOf type -> value = v
            type.jvmErasure==List::class && output.type.jvmErasure==List::class -> {
               valueAny = v
            }
            type.jvmErasure==List::class -> {
               if (type.listType().raw.isInstance(v))
                  valueAny = v
            }
            else -> {}
         }
      }
   }

   @Suppress("UNCHECKED_CAST")
   var valueAny: Any?
      get() = value
      set(it) {
         value = when {
            it==null -> null as T
            type.jvmErasure==List::class -> when (it) {
               is List<*> -> it as T
               else -> listOf(it) as T
            }
            else -> it as T
         }
      }

   /** Sets value of this input to that of the specified output immediately and on every output value change. */
   @Idempotent
   fun bind(output: Output<out T>): Subscription {
      sources.computeIfAbsent(output) {
         monitor(it).also {
            IOLayer.addLinkForAll(this, output)
         }
      }
      return Subscription { unbind(output) }
   }

   @Suppress("UNCHECKED_CAST")
   @Idempotent
   fun bindAllIdentical() {
      val allWidgets = APP.widgetManager.widgets.findAll(OPEN).filter { it.isLoaded }.toList()
      val outputs = getSources().mapTo(HashSet()) { o -> o.id to allWidgets.find { o in it.controller!!.io.o.getOutputs() }?.factory }
      outputs.forEach { (id, factory) ->
         allWidgets.asSequence()
            .filter { it.factory==factory }
            .map { it.controller!!.io.o.getOutputs().find { it.id.name==id.name }!! }
            .forEach { (this as Input<Any?>).bind(it as Output<Any?>) }
      }
   }

   @Idempotent
   fun bind(generatorRef: GeneratingOutputRef<*>): Subscription {
      val o = null
         ?: sources.keys.find {  it.id == generatorRef.id  }
         ?: IOLayer.allInoutputs.find { it.o.id == generatorRef.id }?.o
         ?: generatorRef.asIs<GeneratingOutputRef<Any?>>().block(generatorRef.asIs()).o

      return bind(o.asIs<Output<T>>())
   }

   /** @return true iff any [Output] is bound to this input using [bind]. ] */
   fun isBound(): Boolean = sources.keys.isNotEmpty()

   /** @return true iff an [Output] with the specified id is bound to this input using [bind]. ] */
   fun isBound(id: Id): Boolean = sources.keys.any { it.id==id }

   /** @return true iff at least one [Output] with owner id other than specified is bound to this input using [bind]. ] */
   @JvmOverloads
   fun isBoundUnless(exceptOwner: UUID? = null): Boolean = sources.keys.any { it.id.ownerId!=exceptOwner }

   @Idempotent
   fun unbind(output: Output<*>) {
      sources.remove(output).ifNotNull {
         it.unsubscribe()
         IOLayer.remLinkForAll(this, output)
      }
   }

   fun unbindAll() {
      sources.forEach { (o, disposer) ->
         disposer.unsubscribe()
         IOLayer.remLinkForAll(this, o)
      }
      sources.clear()
   }

   fun getSources(): Set<Output<out T>> = sources.keys

   override fun toString() = "$name, $type"

   companion object {
      private fun VType<*>.listType() = type.argOf(List::class, 0).typeResolved
   }
}