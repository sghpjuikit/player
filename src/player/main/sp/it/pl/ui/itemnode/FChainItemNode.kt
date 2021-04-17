package sp.it.pl.ui.itemnode

import javafx.util.Callback
import sp.it.util.collections.list.PrefList
import sp.it.util.collections.setTo
import sp.it.util.dev.fail
import sp.it.util.functional.Functors.F1
import sp.it.util.functional.Functors.NullIn
import sp.it.util.functional.Functors.NullOut
import sp.it.util.functional.PF
import sp.it.util.functional.TypeAwareF
import sp.it.util.functional.Util.IDENTITY
import sp.it.util.functional.asIs
import sp.it.util.reactive.attach
import sp.it.util.reactive.onChange
import sp.it.util.type.VType
import sp.it.util.type.typeNothingNonNull
import java.util.function.BiPredicate
import java.util.stream.Stream
import kotlin.streams.asSequence
import sp.it.util.type.type

/**
 * Function editor with function chaining.
 * Provides ui for creating functions.
 * <p/>
 * This editor provides editable and scalable chain of stackable functions. The
 * chain:
 * <ul>
 * <li> Is always reducible to single function called reduction function. Applying
 * reduction function is equivalent to applying all functions in the chain in
 * their chain order.
 * <li> can be used as a reduction function {@link #getVal()} or a stream of
 * the standalone functions can be obtained {@link #getValues()}.
 * <li> begins with an input type. It determines the type of input of the first
 * function in the chain and input type of the reduction function. By default it
 * is Void.class. {@link #getTypeIn()}
 * <li> produces output of type determined by the last function in the chain
 * {@link #getTypeOut()}
 * </ul>
 * <p>
 * Supported are also:
 * <ul>
 * <li> null. Functions in the chain  are wrapped to function wrappers to
 * cases of producing or receiving null. The strategy can be customized
 * {@link #setNullHandling(sp.it.util.functional.Functors.NullIn, sp.it.util.functional.Functors.NullOut)}
 * <li> Void. Functions with void parameter can simulate supplier (no input) or
 * consumers (no output) functions. They can be anywhere within the chain.
 * </ul>
 */
class FChainItemNode: ChainValueNode<(Any?) -> Any?, FItemNode<Any?, Any?>, (Any?) -> Any?> {
   private val functorPool: (VType<*>) -> PrefList<PF<*, *>>
   private var handleNullIn = NullIn.NULL
   private var handleNullOut = NullOut.NULL

   @Suppress("UNUSED_ANONYMOUS_PARAMETER", "RemoveExplicitTypeArguments")
   constructor(functorPool: (VType<*>) -> PrefList<PF<*, *>>): super(throwingF()) {
      this.functorPool = functorPool
      chainedFactory = Callback { i ->
         FItemNode(typeOut, type<Any?>(), functorPool.asIs())
      }
      isHomogeneousRem = BiPredicate { i, f ->
         when {
            // Link is homogeneous if removing the function poses no problem.
            // For function f this is when previous function output type is same as next function input type

            // Identity function always produces homogeneous link.
            // Workaround for identity function erasing its own return type to Object.class
            // Identity function (x -> x) has same input type and output type, but this is
            // generalized to Object since it works for any object. We either handle this manually
            // here, or guarantee that the identity function input type will not be erased
            // (basically we need instance of identity function per each class)
            f===IDENTITY -> true
            f is TypeAwareF<*, *> && f.f===IDENTITY -> true
            else -> {
               // If two subsequent functions have same output type or same input type, one of them is safe
               // to remove (which one depends on whether we check inputs or outputs).
               //
               // The exceptional case is the first and the last link of the chain, which lack the previous/following function.
               // However, they can still be removed. Checking for output types will result in the first
               // link being an exceptional case. Below we check for inputs, which results in the last
               // link to be exceptional case. We do this, because the last link can always be removed
               // hence we do not have to handle the case.
               val next = getValueAt(i + 1)
               if (next is TypeAwareF<*, *> && f is TypeAwareF<*, *>) f.typeIn==next.typeIn else false
            }
         }
      }
      isHomogeneousAdd = BiPredicate { i, _ -> i==chain.size - 1 }
      isHomogeneousOn = BiPredicate { _, _ -> true }
      isHomogeneousEdit = BiPredicate { _, _ -> true }
      maxChainLength attach {
         val m: Int = it.toInt()
         if (m<chain.size) {
            chain setTo chain.subList(0, m)
            generateValue()
         }
         chain.forEach { it.updateIcons() }
      }
      maxChainLength.value = Integer.MAX_VALUE
      chain.onChange { chain.forEach { it.updateIcons() } }
      chain.onChange { chain.forEachIndexed { i, it -> it.chained.isEditableRawFunction.value = chain.lastIndex==i } }
      inconsistentState = false
      generateValue()
   }

   /**
    * Sets input type.
    * Input type determines:
    *  * the input type of this chain's reduction function
    *  * input type of the first transformation function in the chain
    *
    * This parameter is:
    *
    *  *  determined by object type this chain's reduction function will be applied to
    *  *  determines available functions in the dropdown box or the 1st chain element
    *
    * If the class changes, entire transformation chain is cleared.
    * Calling this method always results in an update event.
    *
    * Default non nullable [Nothing]
    */
   var typeIn: VType<*> = typeNothingNonNull()
      set(value) {
         if (value==field) {
            generateValue()
         } else {
            field = value
            chain.clear()
            addChained()
         }
      }

   val typeOut: VType<*>
      get() = chain.map { it.chained.typeOut }.fold(typeIn) { i, o -> o ?: i }

   /**
    * Sets null handling during chain function reduction.
    * Calling this method results in an update event.
    */
   fun setNullHandling(ni: NullIn, no: NullOut) {
      handleNullIn = ni
      handleNullOut = no
      generateValue()
   }

   override fun reduce(values: Stream<(Any?) -> Any?>): (Any?) -> Any? = values.asSequence()
      .map { it.asIs<F1<Any?, Any?>>() }   // helps type system, remove when F1 uses declaration variance
      .map { it.wrap(handleNullIn, handleNullOut) }
      .fold(F1 { it }) { a, b -> b.compose(a) }

   companion object {
      private fun throwingF() = F1<Any?, Any?> { fail { "Initial function value. Must not be invoked" } }
   }
}