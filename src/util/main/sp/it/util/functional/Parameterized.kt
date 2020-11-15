package sp.it.util.functional

import sp.it.util.conf.Constraint
import sp.it.util.functional.Util.IDENTITY
import sp.it.util.functional.Util.IS
import sp.it.util.functional.Util.IS0
import sp.it.util.functional.Util.ISNT
import sp.it.util.functional.Util.ISNT0
import sp.it.util.type.VType

private typealias Args = Array<Any?>
private typealias Params = List<Parameter<Any?>>

interface Parameterized<TYPE, PARAMETER> {
   val parameters: List<Parameter<PARAMETER>>
   fun realize(args: List<PARAMETER>): TYPE
}

class Parameter<P> {
   val type: VType<P>
   val defaultValue: P
   val name: String
   val description: String
   val constraints: Set<Constraint<P>>

   constructor(name: String?, description: String?, type: VType<P>, defaultValue: P, constraints: Set<Constraint<P>>) {
      this.name = name ?: "<value>"
      this.description = description ?: this.name
      this.type = type
      this.defaultValue = defaultValue
      this.constraints = constraints
   }

   constructor(type: VType<P>, defaultValue: P): this(null, null, type, defaultValue, setOf())
}

/* Parameterized function - variadic I -> O function factory with parameters */
abstract class PF<I, O>(val name: String, val `in`: VType<I>, val out: VType<O>, private val f: Any): Parameterized<(I) -> O, Any?> {
   abstract fun apply(i: I, args: Args): O
   override fun realize(args: List<Any?>): (I) -> O = when (f) {
      IDENTITY, IS0, ISNT0, IS, ISNT -> f.asIs()
      else -> TypeAwareF({ i -> apply(i, args.toTypedArray()) }, `in`, out)
   }
}

/** Parametric function, `I -> O` function defined as `(I, P1, P2, ..., Pn) -> O` variadic function with parameters. */
class PF0<I, O>(_name: String, i: VType<I>, o: VType<O>, val f: (I) -> O): PF<I, O>(_name, i, o, f) {
   override val parameters: Params = listOf()
   override fun apply(i: I, args: Args): O = f(i)
}

/** Unary parametric function. */
@Suppress("UNCHECKED_CAST")
class PF1<I, P1, O>(_name: String, i: VType<I>, o: VType<O>, val p1: Parameter<P1>, val f: (I, P1) -> O): PF<I, O>(_name, i, o, f) {
   override val parameters: Params = listOf(p1).asIs()
   override fun apply(i: I, args: Args): O = f(i, args[0] as P1)
}

/** Binary parametric function. */
@Suppress("UNCHECKED_CAST")
class PF2<I, P1, P2, O>(_name: String, i: VType<I>, o: VType<O>, val p1: Parameter<P1>, val p2: Parameter<P2>, val f: (I, P1, P2) -> O): PF<I, O>(_name, i, o, f) {
   override val parameters: Params = listOf(p1, p2).asIs()
   override fun apply(i: I, args: Args): O = f(i, args[0] as P1, args[1] as P2)
}

/** Tertiary  parametric function. */
@Suppress("UNCHECKED_CAST")
class PF3<I, P1, P2, P3, O>(_name: String, i: VType<I>, o: VType<O>, val p1: Parameter<P1>, val p2: Parameter<P2>, val p3: Parameter<P3>, val f: (I, P1, P2, P3) -> O): PF<I, O>(_name, i, o, f) {
   override val parameters: Params = listOf(p1, p2, p3).asIs()
   override fun apply(i: I, args: Args): O = f(i, args[0] as P1, args[1] as P2, args[2] as P3)
}

/** N-ary parametric function. */
class PFN<I, O>(_name: String, i: VType<I>, o: VType<O>, val ps: Array<Parameter<Any>>, val f: (I, Args?) -> O): PF<I, O>(_name, i, o, f) {
   override val parameters: Params = ps.toList().asIs()
   override fun apply(i: I, args: Args): O = f(i, args)
}

/** Function with runtime [typeIn]/[typeOut] type information. */
class TypeAwareF<I, O>(val f: (I) -> O, val typeIn: VType<I>, val typeOut: VType<O>): (I) -> O {
   override fun invoke(i: I) = f(i)
}