package sp.it.pl.layout.widget.controller.io

import sp.it.util.dev.failIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.type.VType
import sp.it.util.type.raw
import sp.it.util.type.type
import java.util.HashMap
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

class IO(private val id: UUID) {
   @JvmField val o = Outputs()
   @JvmField val i = Inputs()
   @JvmField val io = InOutputs()

   private val mi = HashMap<String, Input<*>>()
   private val mo = HashMap<String, Output<*>>()
   private val mio = HashMap<String, InOutput<*>>()
   private val onDispose = Disposer()

   fun dispose() {
      onDispose()
      i.getInputs().forEach { it.unbindAll() }
   }

   inner class Inputs {

      @Suppress("UNCHECKED_CAST")
      @JvmOverloads
      inline fun <reified T> create(name: String, initialValue: T? = null, noinline action: (T?) -> Unit) = create(name, type(), initialValue, action)

      @Suppress("UNCHECKED_CAST")
      @JvmOverloads
      fun <T> create(name: String, type: VType<T>, initialValue: T? = null, action: (T?) -> Unit): Input<T?> {
         failIf(mi[name]!=null) { "Input $name already exists" }

         val i: Input<T?> = Input(name, type, initialValue, action)
         mi[name] = i
         return i
      }

      fun getInputRaw(name: String): Input<*>? = mi[name]

      @Suppress("UNCHECKED_CAST")
      fun <T: Any> findInput(type: KClass<T>, name: String): Input<T?>? {
         val i = mi[name]
         if (i!=null && !type.isSubclassOf(i.type.raw)) throw ClassCastException()
         return i as Input<T?>?
      }

      fun <T: Any> getInput(type: KClass<T>, name: String): Input<T?> = findInput(type, name)!!

      inline fun <reified T: Any> findInput(name: String) = findInput(T::class, name)

      inline fun <reified T: Any> getInput(name: String) = findInput(T::class, name)!!

      operator fun contains(name: String) = mi.containsKey(name)

      operator fun contains(i: Input<*>) = mi.containsValue(i) // m.containsKey(i.name) <-- faster, but not correct

      fun getSize(): Int = mi.size

      fun getInputs(): Collection<Input<*>> = mi.values

   }

   inner class Outputs {

      inline fun <reified T> create(name: String, value: T?): Output<T?> = create(name, type(), value)

      @Suppress("UNCHECKED_CAST")
      fun <T> create(name: String, type: VType<T>, value: T?): Output<T?> {
         failIf(mo[name]!=null) { "Output $name already exists" }

         val o = Output<T?>(id, name, type)
         o.value = value
         mo[name] = o
         return o
      }

      @Suppress("UNCHECKED_CAST")
      fun <T: Any> findOutput(type: KClass<T>, name: String): Output<T?>? {
         val i = mo[name]
         if (i!=null && !type.isSuperclassOf(i.type.raw)) throw ClassCastException()
         return i as Output<T?>?
      }

      fun <T: Any> getOutput(type: KClass<T>, name: String): Output<T?> = findOutput(type, name)!!

      inline fun <reified T: Any> findOutput(name: String) = findOutput(T::class, name)

      inline fun <reified T: Any> getOutput(name: String) = findOutput(T::class, name)!!

      operator fun contains(name: String) = mo.containsKey(name)

      operator fun contains(i: Output<*>) = mo.containsValue(i) // m.containsKey(i.id.name) <--faster, but not correct

      fun getSize(): Int = mo.size

      fun getOutputs(): Collection<Output<*>> = mo.values

   }

   inner class InOutputs {

      inline fun <T: Any, reified R> mapped(output: Input<T?>, name: String, noinline mapper: (R) -> T?) = mapped(output, name, type(), mapper)

      fun <T: Any, R> mapped(input: Input<T?>, name: String, type: VType<R>, mapper: (R) -> T?): Input<R?> {
         val io = InOutput(id, name, type)
         mio[name] = io
         mi[name] = io.i
         onDispose += input.bindMapped(io.o, mapper)
         IOLayer.addLinkForAll(input, io.i)
         onDispose += { IOLayer.remLinkForAll(io.i, input) }
         return io.i
      }

      inline fun <T: Any, reified R: Any> mapped(output: Output<T?>, name: String, noinline mapper: (T) -> R?) = mapped(output, name, type<R>(), mapper)

      fun <T: Any, R> mapped(output: Output<T?>, name: String, type: VType<R>, mapper: (T) -> R?): Output<R?> {
         val io = InOutput(id, name, type)
         mio[name] = io
         mo[name] = io.o
         onDispose += io.i.bindMapped(output, mapper)
         IOLayer.addLinkForAll(io.o, output)
         onDispose += { IOLayer.remLinkForAll(output, io.o) }
         return io.o
      }

      operator fun contains(name: String) = mio.containsKey(name)

      operator fun contains(i: InOutput<*>) = mio.containsValue(i) // m.containsKey(i.id.name) <--faster, but not correct

      fun getSize(): Int = mio.size

      fun getInOutputs(): Collection<InOutput<*>> = mio.values
   }

   companion object {

      private fun <T, R> Input<T?>.bindMapped(output: Output<out R?>, mapper: (R) -> T?): Subscription {
         return output.sync { value = if (it==null) null else mapper(it) }
      }

   }
}