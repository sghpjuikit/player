package sp.it.util.math

import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder
import sp.it.util.dev.Dependency
import sp.it.util.functional.Try
import sp.it.util.functional.runTry

/** Mathematical `Double -> Double` Function created from string expression. */
class StrExF(s: String): (Double) -> Double {
   private val expression: String
   private val e: Expression

   init {
      try {
         expression = s
         e = ExpressionBuilder(s).variables("x").build()
         val v = e.validate(false)
         if (!v.isValid)
            throw Exception(v.errors[0])
      } catch (e: Exception) {
         throw IllegalStateException(e)
      }
   }

   override fun invoke(queryParam: Double): Double = e.setVariable("x", queryParam).evaluate()

   @Dependency("fromString")
   override fun toString() = expression

   companion object {

      @Dependency("toString")
      fun fromString(s: String): Try<StrExF, Throwable> = runTry {
         StrExF(s)
      }
   }

}