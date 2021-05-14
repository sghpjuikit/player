package sp.it.pl.layout.widget.controller.io

import sp.it.util.dev.failIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.type.VType
import sp.it.util.type.type
import java.util.HashMap
import java.util.UUID
import sp.it.util.collections.materialize
import sp.it.util.functional.asIs
import sp.it.util.type.isSupertypeOf

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

      inline fun <reified T> create(name: String, initialValue: T, noinline action: (T) -> Unit) = create(name, type(), initialValue, action)

      fun <T> create(name: String, type: VType<T>, initialValue: T, action: (T) -> Unit): Input<T> {
         failIf(mi[name]!=null) { "Input $name already exists" }

         val i = Input(name, type, initialValue, action)
         mi[name] = i
         IOLayer.allInputs += i
         return i
      }

      fun remove(i: Input<*>) {
         i.unbindAll()
         i.dispose()
         mi -= i.name
         IOLayer.allInputs -= i
      }

      fun removeAll() {
         mi.values.toList().materialize().forEach(::remove)
      }

      fun getInputRaw(name: String): Input<*>? = mi[name]

      @Suppress("UNCHECKED_CAST")
      fun <T> findInput(type: VType<T>, name: String): Input<T>? {
         val i = mi[name]
         if (i!=null && !i.type.isSupertypeOf(type)) return null
         return i as Input<T>?
      }

      inline fun <reified T> findInput(name: String): Input<T>? = findInput(type(), name)

      inline fun <reified T> getInput(name: String): Input<T> = findInput(name)!!

      operator fun contains(name: String) = mi.containsKey(name)

      operator fun contains(i: Input<*>) = mi.containsValue(i)
      fun getSize(): Int = mi.size

      fun getInputs(): Collection<Input<*>> = mi.values

   }

   inner class Outputs {

      inline fun <reified T> create(name: String, initialValue: T): Output<T> = create(name, type(), initialValue)

      fun <T> create(name: String, type: VType<T>, initialValue: T): Output<T> {
         failIf(mo[name]!=null) { "Output $name already exists" }

         val o = Output(id, name, type, initialValue)
         mo[name] = o
         return o
      }

      fun <T> findOutput(type: VType<T>, name: String): Output<T>? {
         val i = mo[name]
         if (i!=null && !type.isSupertypeOf(i.type)) return null
         return i.asIs()
      }

      inline fun <reified T> findOutput(name: String): Output<T>? = findOutput(type(), name)

      inline fun <reified T> getOutput(name: String): Output<T> = findOutput(name)!!

      operator fun contains(name: String) = mo.containsKey(name)

      operator fun contains(i: Output<*>) = mo.containsValue(i)

      fun getSize(): Int = mo.size

      fun getOutputs(): Collection<Output<*>> = mo.values

   }

   inner class InOutputs {

      inline fun <T, reified R> mapped(input: Input<T>, name: String, noinline mapper: (R) -> T) = mapped(input, name, type(), mapper)

      @Suppress("unchecked_cast")
      fun <T, R> mapped(input: Input<T>, name: String, type: VType<R>, mapper: (R) -> T): Input<R> {
         val io = InOutput(id, name, type, null as R)
         mio[name] = io
         mi[name] = io.i
         onDispose += input.bindMapped(io.o, mapper)
         IOLayer.addLinkForAll(input, io.i)
         onDispose += { IOLayer.remLinkForAll(io.i, input) }
         return io.i
      }

      inline fun <T, reified R> mapped(output: Output<T>, name: String, noinline mapper: (T) -> R) = mapped(output, name, type(), mapper)

      fun <T, R> mapped(output: Output<T>, name: String, type: VType<R>, mapper: (T) -> R): Output<R> {
         val io = InOutput(id, name, type, mapper(output.value))
         mio[name] = io
         mo[name] = io.o
         onDispose += io.i.bindMapped(output, mapper)
         IOLayer.addLinkForAll(io.o, output)
         onDispose += { IOLayer.remLinkForAll(output, io.o) }
         return io.o
      }

      operator fun contains(name: String) = mio.containsKey(name)

      operator fun contains(i: InOutput<*>) = mio.containsValue(i)

      fun getSize(): Int = mio.size

      fun getInOutputs(): Collection<InOutput<*>> = mio.values
   }

   companion object {

      private fun <T, R> Input<T>.bindMapped(output: Output<out R>, mapper: (R) -> T): Subscription {
         return output.attach { value = mapper(it) }
      }

   }
}