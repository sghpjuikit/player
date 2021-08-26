package sp.it.pl.layout.controller.io

/** Common supertype for inputs/outputs of a component. */
interface XPut<T>

/** Common ways to compare values by identity. */
object EqualizeBy {
   /** Comparison identical to `value1==value2`. */
   val EQ = { a: Any?, b: Any? -> a==b }
   /** Comparison identical to `value1===value2`. */
   val REF = { a: Any?, b: Any? -> a===b }
   /** Comparison that always considers values different. */
   val NONE = { _: Any?, _: Any? -> false }
}